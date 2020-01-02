/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.owb.core;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.owb.spi.StartObjectSupplier;
import org.apache.aries.cdi.spi.CDIContainerInitializer;
import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.spi.ApplicationBoundaryService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.spi.ScannerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class OWBCDIContainerInitializer extends CDIContainerInitializer {

	public OWBCDIContainerInitializer(BundleContext bundleContext) {
		owbBundleContext = bundleContext;
	}

	@Override
	public CDIContainerInitializer addBeanClasses(Class<?>... classes) {
		beanClasses.addAll(Arrays.asList(classes));
		return this;
	}

	@Override
	public CDIContainerInitializer addBeanXmls(URL... beanXmls) {
		beanDescriptorURLs.addAll(Arrays.asList(beanXmls));
		return this;
	}

	@Override
	public CDIContainerInitializer addExtension(Extension extension, Map<String, Object> properties) {
		this.extensions.put(extension, properties);
		return this;
	}

	@Override
	public CDIContainerInitializer addProperty(String key, Object value) {
		properties.putIfAbsent(key, value);
		return this;
	}

	@Override
	public CDIContainerInitializer setBundleContext(BundleContext bundleContext) {
		clientBundleContext = bundleContext;
		return this;
	}

	@Override
	public CDIContainerInitializer setClassLoader(SpiLoader spiLoader) {
		this.spiLoader = spiLoader;
		return this;
	}

	@Override
	public SeContainer initialize() {
		requireNonNull(spiLoader).handleResources(
			s -> (s != null) && s.startsWith("META-INF/openwebbeans/"),
			this::getResources
		).findClass(
			s -> (s != null) && (s.startsWith("org.apache.webbeans.") || s.startsWith("sun.misc.")),
			this::loadClass);

		spiLoader.getBundles().add(owbBundleContext.getBundle());

		BundleWiring bundleWiring = owbBundleContext.getBundle().adapt(BundleWiring.class);
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.apache.webbeans.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!spiLoader.getBundles().contains(wireBundle)) {
				spiLoader.getBundles().add(wireBundle);
			}
		}

		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(spiLoader);
			startObject = requireNonNull(clientBundleContext);

			final Map<Class<?>, Object> services = new HashMap<>();
			properties.setProperty(
				DefiningClassService.class.getName(),
				OSGiDefiningClassService.class.getName());

			services.put(
				OSGiDefiningClassService.ClassLoaders.class,
				new OSGiDefiningClassService.ClassLoaders(current, spiLoader));
			services.put(
				ApplicationBoundaryService.class,
				new OsgiApplicationBoundaryService(current, spiLoader));
			services.put(
				ScannerService.class,
				new CdiScannerService(beanClasses, beanDescriptorURLs));
			services.put(BundleContext.class, clientBundleContext);

			// If we find the OpenWebBeans "aries.cdi.http" Extension enable web mode.
			// This Extension will have properties:
			//    osgi.cdi.extension = aries.cdi.http
			//    aries.cdi.http.provider = OpenWebBeans
			extensions.entrySet().stream()
					.filter(it -> StartObjectSupplier.class.isInstance(it.getKey()))
					.max(comparing(it -> StartObjectSupplier.class.cast(it.getKey()).ordinal()))
					.ifPresent(entry -> {
						// The service properties of the extension should list any properties needed
						// to configure OWB for web support.
						properties.putAll(entry.getValue());

						// Extract the start instance to ensure it works with the configured services (properties)
						startObject = StartObjectSupplier.class.cast(entry.getKey()).getStartObject();
					});

			bootstrap = new WebBeansContext(services, properties) {
				private final ExtensionLoader overridenExtensionLoader = new ExtensionLoader(this) {
					@Override
					public void loadExtensionServices() {
						extensions.forEach((k, v) -> addExtension(k));
					}
				};

				@Override
				public ExtensionLoader getExtensionLoader() {
					return overridenExtensionLoader;
				}
			};

			final DefaultSingletonService singletonService = getSingletonService();
			singletonService.register(spiLoader, bootstrap);
			final ContainerLifecycle lifecycle = bootstrap.getService(ContainerLifecycle.class);
			lifecycle.startApplication(startObject);

			return new OWBSeContainer();
		}
		finally {
			currentThread.setContextClassLoader(current);
		}
	}

	protected Enumeration<URL> getResources(String name) {
		try {
			return WebBeansContext.class.getClassLoader().getResources(name);
		} catch (IOException e) {
			throwsUnchecked(e);
			return null; // unreachable
		}
	}

	protected Class<?> loadClass(String name) {
		try {
			return WebBeansContext.class.getClassLoader().loadClass(name);
		} catch (ClassNotFoundException e) {
			throwsUnchecked(e);
			return null; // unreachable
		}
	}

	protected DefaultSingletonService getSingletonService() {
		return DefaultSingletonService.class.cast(WebBeansFinder.getSingletonService());
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwsUnchecked(Throwable throwable) throws E {
		throw (E) throwable;
	}

	private volatile WebBeansContext bootstrap;
	private final List<URL> beanDescriptorURLs = new ArrayList<>();
	private volatile BundleContext clientBundleContext;
	private volatile SpiLoader spiLoader;
	private final Set<Class<?>> beanClasses = new HashSet<>();
	private final Map<Extension, Map<String, Object>> extensions = new IdentityHashMap<>();
	private final BundleContext owbBundleContext;
	private final Properties properties = new Properties();
	private Object startObject;

	private class OWBSeContainer implements SeContainer {

		private volatile boolean running = true;

		@Override
		public void close() {
			running = false;
			Thread currentThread = Thread.currentThread();
			ClassLoader current = currentThread.getContextClassLoader();
			try {
				currentThread.setContextClassLoader(requireNonNull(spiLoader));
				bootstrap.getService(ContainerLifecycle.class).stopApplication(startObject);
			}
			finally {
				currentThread.setContextClassLoader(current);
			}
		}

		@Override
		public Instance<Object> select(Annotation... qualifiers) {
			return bootstrap.getBeanManagerImpl().createInstance().select(qualifiers);
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			return bootstrap.getBeanManagerImpl().createInstance().select(subtype, qualifiers);
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			return bootstrap.getBeanManagerImpl().createInstance().select(subtype, qualifiers);
		}

		@Override
		public boolean isUnsatisfied() {
			return bootstrap.getBeanManagerImpl().createInstance().isUnsatisfied();
		}

		@Override
		public boolean isAmbiguous() {
			return bootstrap.getBeanManagerImpl().createInstance().isAmbiguous();
		}

		@Override
		public void destroy(Object instance) {
			bootstrap.getBeanManagerImpl().createInstance().destroy(instance);
		}

		@Override
		public Iterator<Object> iterator() {
			return bootstrap.getBeanManagerImpl().createInstance().iterator();
		}

		@Override
		public boolean isRunning() {
			return running;
		}

		@Override
		public BeanManager getBeanManager() {
			return bootstrap.getBeanManagerImpl();
		}

		@Override
		public Object get() {
			return bootstrap.getBeanManagerImpl().createInstance().get();
		}

	}

}
