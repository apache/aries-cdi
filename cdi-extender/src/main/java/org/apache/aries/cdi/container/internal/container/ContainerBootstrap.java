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

package org.apache.aries.cdi.container.internal.container;

import static java.util.Collections.list;
import static java.util.stream.Collectors.toSet;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.inject.spi.Extension;
import javax.servlet.ServletContext;

import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.servlet.UpdatableServletContext;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.portable.events.ExtensionLoader;
import org.apache.webbeans.spi.ApplicationBoundaryService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.DefiningClassService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.web.context.WebContextsService;
import org.apache.webbeans.web.lifecycle.WebContainerLifecycle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.Logger;

public class ContainerBootstrap extends Phase {

	public ContainerBootstrap(
		ContainerState containerState,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		super(containerState, null);

		_configurationBuilder = configurationBuilder;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_log = containerState.containerLogs().getLogger(getClass());
		_startObject = new UpdatableServletContext();
	}

	@Override
	public boolean close() {
		try (Syncro syncro = _lock.open()) {
			if (_bootstrap != null) {
				_log.debug(l -> l.debug("CCR container bootstrap shutdown on {}", _bootstrap));
				Thread currentThread = Thread.currentThread();
				ClassLoader current = currentThread.getContextClassLoader();
				try {
					currentThread.setContextClassLoader(containerState.classLoader());
					_bootstrap.getService(ContainerLifecycle.class).stopApplication(_startObject);
				}
				finally {
					currentThread.setContextClassLoader(current);
				}
				_bootstrap = null;
			}

			return true;
		}
		catch (Throwable t) {
			_log.error(l -> l.error("CCR Failure in container bootstrap shutdown on {}", _bootstrap, t));

			return false;
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}

	@Override
	public boolean open() {
		try (Syncro syncro = _lock.open()) {
			if (containerState.bundleContext() == null) {
				// this bundle was already removed
				return false;
			}

			if (_bootstrap != null) {
				return true;
			}

			if (containerState.containerDTO().components.isEmpty()) {
				return false;
			}

			Thread currentThread = Thread.currentThread();
			ClassLoader current = currentThread.getContextClassLoader();
			try {
				currentThread.setContextClassLoader(containerState.classLoader());

				List<Extension> extensions = getExtensions();

				// Add external extensions
				containerState.containerDTO().extensions.stream().map(
					ExtendedExtensionDTO.class::cast
				).map(
					e -> e.extension.getService()
				).forEach(extensions::add);

				final Properties properties = getConfiguration();
				final Map<Class<?>, Object> services = getServices(current);
				_bootstrap = new WebBeansContext(services, properties) {
					private final ExtensionLoader overridenExtensionLoader = new ExtensionLoader(this) {
						@Override
						public void loadExtensionServices() {
							extensions.forEach(this::addExtension);
						}
					};

					@Override
					public ExtensionLoader getExtensionLoader() {
						return overridenExtensionLoader;
					}
				};

				final DefaultSingletonService singletonService = getSingletonService();
				singletonService.register(currentThread.getContextClassLoader(), _bootstrap);

				final ContainerLifecycle lifecycle = _bootstrap.getService(ContainerLifecycle.class);
				final ServletContext servletContext = _startObject.getServletContext();
				servletContext.setAttribute(BundleContext.class.getName(), bundle().getBundleContext());
				servletContext.setAttribute(WebBeansContext.class.getName(), _bootstrap);
				// todo: use actual SE
				lifecycle.startApplication(_startObject);
			}
			finally {
				currentThread.setContextClassLoader(current);
			}

			return true;
		}
	}

	private DefaultSingletonService getSingletonService() {
		return DefaultSingletonService.class.cast(WebBeansFinder.getSingletonService());
	}

	protected Properties getConfiguration() {
		final Properties properties = new Properties();
		// OSGi
		properties.setProperty(DefiningClassService.class.getName(), OSGiDefiningClassService.class.getName());
		// Web mode - minimal set, see META-INF/openwebbeans/openwebbeans.properties in openwebbeans-web for details
		// todo: enable to not use web?
		properties.setProperty(ContainerLifecycle.class.getName(), WebContainerLifecycle.class.getName());
		properties.setProperty(ContextsService.class.getName(), WebContextsService.class.getName());
		// user/bundle overrides
		final Dictionary<String, String> headers = bundle().getHeaders();
		if (headers != null) {
			list(headers.elements()).stream()
					.filter(it -> it.startsWith("org.apache.openwebbeans."))
					.forEach(key -> properties.setProperty(key, headers.get(key)));
		}
		return properties;
	}

	protected Map<Class<?>, Object> getServices(final ClassLoader bundleLoader) {
		final Map<Class<?>, Object> services = new HashMap<>();
		services.put(OSGiDefiningClassService.ClassLoaders.class, new OSGiDefiningClassService.ClassLoaders(
				bundleLoader, containerState.classLoader()));
		services.put(ApplicationBoundaryService.class, new OsgiApplicationBoundaryService(bundleLoader, containerState.classLoader()));
		services.put(ScannerService.class, new CdiScannerService(
				containerState.beansModel().getBeanClassNames().stream()
						.map(containerState.beansModel()::getOSGiBean)
						.map(OSGiBean::getBeanClass)
						.collect(toSet()),
				containerState.beansModel().getBeansXml()));

		// just to be able to get it contextually if needed
		services.put(BundleContext.class, bundle().getBundleContext());
		services.put(UpdatableServletContext.class, _startObject);
		return services;
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}

	protected List<Extension> getExtensions() {
		List<Extension> extensions = new CopyOnWriteArrayList<>();

		// Add the internal extensions
		extensions.add(new BundleContextExtension(containerState.bundleContext()));
		extensions.add(new RuntimeExtension(containerState, _configurationBuilder, _singleBuilder, _factoryBuilder));
		extensions.add(new LoggerExtension(containerState));

		// Add extensions found from the bundle's class loader, such as those in the Bundle-ClassPath
		ServiceLoader.load(Extension.class, containerState.classLoader()).forEach(extensions::add);

		return extensions;
	}

	private volatile WebBeansContext _bootstrap;
	private final ConfigurationListener.Builder _configurationBuilder;
	private final FactoryComponent.Builder _factoryBuilder;
	private final SingleComponent.Builder _singleBuilder;
	private final Syncro _lock = new Syncro(true);
	private final Logger _log;
	private final UpdatableServletContext _startObject;
}