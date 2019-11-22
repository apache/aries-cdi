package org.apache.aries.cdi.owb;

import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static org.apache.aries.cdi.spi.Keys.BEANS_XML_PROPERTY;
import static org.apache.aries.cdi.spi.Keys.BUNDLECONTEXT_PROPERTY;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.spi.ApplicationBoundaryService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.spi.ScannerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class OWBSeContainerInitializer extends SeContainerInitializer {

	public OWBSeContainerInitializer(BundleContext bundleContext) {
		owbBundleContext = bundleContext;
	}

	@Override
	public SeContainerInitializer addBeanClasses(Class<?>... classes) {
		beanClasses.addAll(Arrays.asList(classes));
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(Class<?>... packageClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Class<?>... packageClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(Package... packages) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Package... packages) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addExtensions(Extension... extensions) {
		this.extensions.addAll(Arrays.asList(extensions));
		return this;
	}

	@Override
	public SeContainerInitializer addExtensions(Class<? extends Extension>... extensions) {
		this.extensionClasses.addAll(Arrays.asList(extensions));
		return this;
	}

	@Override
	public SeContainerInitializer enableInterceptors(Class<?>... interceptorClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer enableDecorators(Class<?>... decoratorClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer selectAlternatives(Class<?>... alternativeClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer selectAlternativeStereotypes(
		Class<? extends Annotation>... alternativeStereotypeClasses) {

		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addProperty(String key, Object value) {
		properties.putIfAbsent(key, value);
		return this;
	}

	@Override
	public SeContainerInitializer setProperties(Map<String, Object> properties) {
		properties.putAll(properties);
		return this;
	}

	@Override
	public SeContainerInitializer disableDiscovery() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer setClassLoader(ClassLoader classLoader) {
		this.classLoader = (SpiLoader)classLoader;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SeContainer initialize() {
		requireNonNull(classLoader).handleResources(
			s -> (s != null) && s.startsWith("META-INF/openwebbeans/"),
			this::getResources
		).findClass(
			s -> (s != null) && (s.startsWith("org.apache.webbeans.") || s.startsWith("sun.misc.")),
			this::loadClass);

		classLoader.getBundles().add(owbBundleContext.getBundle());

		BundleWiring bundleWiring = owbBundleContext.getBundle().adapt(BundleWiring.class);
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.apache.webbeans.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!classLoader.getBundles().contains(wireBundle)) {
				classLoader.getBundles().add(wireBundle);
			}
		}

		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(classLoader);
			clientBundleContext = requireNonNull(BundleContext.class.cast(properties.get(BUNDLECONTEXT_PROPERTY)));
			startObject = clientBundleContext;

			final Map<Class<?>, Object> services = new HashMap<>();
			properties.setProperty(
				DefiningClassService.class.getName(),
				OSGiDefiningClassService.class.getName());

			services.put(
				OSGiDefiningClassService.ClassLoaders.class,
				new OSGiDefiningClassService.ClassLoaders(current, classLoader));
			services.put(
				ApplicationBoundaryService.class,
				new OsgiApplicationBoundaryService(current, classLoader));
			services.put(
				ScannerService.class,
				new CdiScannerService(beanClasses, Collection.class.cast(properties.get(BEANS_XML_PROPERTY))));
			services.put(BundleContext.class, clientBundleContext);

			if (Activator.webEnabled) {
				// Web mode - minimal set, see META-INF/openwebbeans/openwebbeans.properties in openwebbeans-web for details
				// todo: enable to not use web?
				properties.setProperty(
					ContainerLifecycle.class.getName(),
					org.apache.webbeans.web.lifecycle.WebContainerLifecycle.class.getName());
				properties.setProperty(
					ContextsService.class.getName(),
					org.apache.webbeans.web.context.WebContextsService.class.getName());
				properties.setProperty(
					ConversationService.class.getName(),
					org.apache.webbeans.web.context.WebConversationService.class.getName());

				startObject = new org.apache.aries.cdi.owb.web.UpdatableServletContext(bootstrap, clientBundleContext);
				services.put(org.apache.aries.cdi.owb.web.UpdatableServletContext.class, startObject);
			}

			Optional.ofNullable(clientBundleContext.getBundle().getHeaders()).ifPresent(
				headers -> list(headers.elements()).stream()
					.filter(it -> it.startsWith("org.apache.openwebbeans."))
					.forEach(key -> properties.setProperty(key, headers.get(key))
				)
			);

			bootstrap = new WebBeansContext(services, properties) {
				private final ExtensionLoader overridenExtensionLoader = new ExtensionLoader(this) {
					@Override
					public void loadExtensionServices() {
						extensions.removeIf(ext -> {addExtension(ext); return true;});
					}
				};

				@Override
				public ExtensionLoader getExtensionLoader() {
					return overridenExtensionLoader;
				}
			};

			final DefaultSingletonService singletonService = getSingletonService();
			singletonService.register(classLoader, bootstrap);
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
	private volatile BundleContext clientBundleContext;
	private volatile SpiLoader classLoader;
	private final Set<Class<?>> beanClasses = new HashSet<>();
	private final List<Extension> extensions = new ArrayList<>();
	private final List<Class<? extends Extension>> extensionClasses = new ArrayList<>();
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
				currentThread.setContextClassLoader(requireNonNull(classLoader));
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
