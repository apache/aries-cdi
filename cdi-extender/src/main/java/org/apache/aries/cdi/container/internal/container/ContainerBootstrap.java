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

import static aQute.bnd.exceptions.FunctionWithException.asFunction;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.loader.BundleClassLoader;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.spi.ContainerListener;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.apache.aries.cdi.spi.CDIContainerInitializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.service.cdi.runtime.dto.ExtensionDTO;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerBootstrap extends Phase {

	public ContainerBootstrap(
			ContainerState containerState,
			ServiceTracker<CDIContainerInitializer, ServiceObjects<CDIContainerInitializer>> containerTracker,
			ConfigurationListener.Builder configurationBuilder,
			SingleComponent.Builder singleBuilder,
			FactoryComponent.Builder factoryBuilder,
			ServiceTracker<ContainerListener, ContainerListener> listeners) {

		super(containerState, null);

		_configurationBuilder = configurationBuilder;
		_containerTracker = containerTracker;
		_listeners = listeners;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		try (Syncro syncro = _lock.open()) {
			if (_containerInstance != null) {
				_log.debug(l -> l.debug("CCR container shutdown for {}", bundle()));
				try {
					_containerInstance.close();
					withListeners(ContainerListener::onStopSuccess);
				} catch (final RuntimeException re) {
					withListeners(listener -> listener.onStopError(re));
					throw re;
				} finally {
					_containerInstance = null;
					try {
						getServiceObjects().ungetService(_initializer);
						_initializer = null;
					}
					catch (Throwable t) {
						_log.trace(l -> l.trace("CCR Failure in returning initializer instance on {}", bundle(), t));
					}
				}
			}

			return true;
		}
		catch (Throwable t) {
			_log.error(l -> l.error("CCR Failure in container bootstrap shutdown on {}", bundle(), t));

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

			if (_containerInstance != null) {
				return true;
			}

			if (containerState.containerDTO().components.isEmpty()) {
				return false;
			}

			_log.debug(log -> log.debug("CCR container startup for {}", bundle()));

			try {
				// always use a new class loader
				BundleClassLoader loader = new BundleClassLoader(containerState.bundle(), containerState.extenderBundle());

				_initializer = getServiceObjects().getService();

				processExtensions(loader, _initializer);

				containerState.containerComponentTemplateDTO().properties.forEach(_initializer::addProperty);

				_containerInstance = _initializer
						.addBeanClasses(containerState.beansModel().getOSGiBeans().stream().map(OSGiBean::getBeanClass).toArray(Class<?>[]::new))
						.addBeanXmls(containerState.beansModel().getBeansXml().toArray(new URL[0]))
						.setBundleContext(bundle().getBundleContext())
						.setClassLoader(loader)
						.initialize();

				withListeners(ContainerListener::onStartSuccess);
			} catch (final RuntimeException re) {
				withListeners(listener -> listener.onStartError(re));
				throw re;
			}

			return true;
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}

	protected void processExtensions(BundleClassLoader loader, CDIContainerInitializer initializer) {
		AtomicInteger counter = new AtomicInteger();

		// Add the internal extensions
		initializer.addExtension(
			new ConfigurationExtension(containerState),
			Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
					Constants.SERVICE_DESCRIPTION, "Aries CDI ConfigurationExtension"));
		initializer.addExtension(
			new BundleContextExtension(containerState.bundleContext()),
			Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
					Constants.SERVICE_DESCRIPTION, "Aries CDI BundleContextExtension"));
		initializer.addExtension(
			new RuntimeExtension(containerState, _configurationBuilder, _singleBuilder, _factoryBuilder),
			Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
					Constants.SERVICE_DESCRIPTION, "Aries CDI RuntimeExtension"));
		initializer.addExtension(
			new LoggerExtension(containerState),
			Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
					Constants.SERVICE_DESCRIPTION, "Aries CDI LoggerExtension"));
		initializer.addExtension(
			new ServiceAdapterExtension(),
			Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
					Constants.SERVICE_DESCRIPTION, "Aries CDI ServiceAdapterExtension"));

		// Add extensions found from the bundle's class loader, such as those in the Bundle-ClassPath
		final Collection<Class<?>> serviceExtensions = new HashSet<>();

		// Add external extensions
		for (ExtensionDTO extensionDTO : containerState.containerDTO().extensions) {
			ExtendedExtensionDTO extendedExtensionDTO = (ExtendedExtensionDTO)extensionDTO;

			Dictionary<String,Object> properties = extendedExtensionDTO.extension.getServiceReference().getProperties();

			final Extension service = extendedExtensionDTO.extension.getService();
			initializer.addExtension(service, Maps.of(properties));
			serviceExtensions.add(service.getClass());

			Bundle extensionBundle = extendedExtensionDTO.extension.getServiceReference().getBundle();

			getClassesFromExtensionCapability(properties, extensionBundle, initializer);

			if (!loader.getBundles().contains(extensionBundle)) {
				loader.getBundles().add(extensionBundle);
			}
		}

		ServiceLoader.load(Extension.class, containerState.classLoader()).forEach(extension -> {
			// let ServiceLoader.load have duplicatesd fail (cdi spec)
			// we just want to solve conflicts between ServiceLoader and OSGi extensions
			if (!serviceExtensions.contains(extension.getClass())) {
				initializer.addExtension(
						extension,
						Maps.of(Constants.SERVICE_ID, counter.decrementAndGet(),
								Constants.SERVICE_DESCRIPTION, "ClassLoader Extension from " + containerState.bundle()));
			}
		});
	}

	private void getClassesFromExtensionCapability(Dictionary<String,Object> properties, Bundle extensionBundle, CDIContainerInitializer initializer) {
		List<BundleCapability> capabilities = extensionBundle.adapt(BundleWiring.class).getCapabilities(CDI_EXTENSION_PROPERTY);

		if (capabilities.isEmpty()) {
			return;
		}

		Map<String, Object> attributes = capabilities.stream().map(Capability::getAttributes).filter(
			map -> map.get(CDI_EXTENSION_PROPERTY).equals(properties.get(CDI_EXTENSION_PROPERTY))
		).findFirst().orElseGet(Collections::emptyMap);

		ofNullable(
			attributes.get("aries.cdi.extension.bean.classes")
		).map(List.class::cast).map(List<String>::stream).orElseGet(Stream::empty).map(
			asFunction(extensionBundle::loadClass)
		).forEach(
			initializer::addBeanClasses
		);
	}

	private void withListeners(final Consumer<ContainerListener> action) {
		final ServiceReference<ContainerListener>[] refs = _listeners.getServiceReferences();
		if (refs != null && refs.length > 0) {
			final BundleContext bundleContext = bundle().getBundleContext();
			Stream.of(refs).forEach(ref -> {
				final ContainerListener service = bundleContext.getService(ref);
				if (service != null) {
					try {
						action.accept(service);
					} finally {
						bundleContext.ungetService(ref);
					}
				}
			});
		}
	}

	private ServiceObjects<CDIContainerInitializer> getServiceObjects() {
		if (_serviceObjects == null) {
			try (Syncro syncro = _lock.open()) {
				if (_serviceObjects == null) {
					int shortWaitLoop = 100;
					while (_containerTracker.isEmpty() && (shortWaitLoop > 0)) {
						shortWaitLoop--;
					}

					_serviceObjects = requireNonNull(
						_containerTracker.getService(),
						"A prototype scope org.apache.aries.cdi.spi.CDIContainerInitializer service must be available.");
				}
			}
		}

		return _serviceObjects;
	}

	private volatile AutoCloseable _containerInstance;
	private final ServiceTracker<CDIContainerInitializer, ServiceObjects<CDIContainerInitializer>> _containerTracker;
	private final ConfigurationListener.Builder _configurationBuilder;
	private final FactoryComponent.Builder _factoryBuilder;
	private CDIContainerInitializer _initializer;
	private volatile ServiceObjects<CDIContainerInitializer> _serviceObjects;
	private final SingleComponent.Builder _singleBuilder;
	private final Syncro _lock = new Syncro(true);
	private final Logger _log;
	private final ServiceTracker<ContainerListener, ContainerListener> _listeners;

}
