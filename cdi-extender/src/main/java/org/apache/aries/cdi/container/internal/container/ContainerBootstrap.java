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

import static org.apache.aries.cdi.spi.Keys.BEANS_XML_PROPERTY;
import static org.apache.aries.cdi.spi.Keys.BUNDLECONTEXT_PROPERTY;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.loader.BundleClassLoader;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionDTO;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.log.Logger;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerBootstrap extends Phase {

	public ContainerBootstrap(
		ContainerState containerState,
		ServiceTracker<SeContainerInitializer, ServiceObjects<SeContainerInitializer>> containerTracker,
		ConfigurationListener.Builder configurationBuilder,
		SingleComponent.Builder singleBuilder,
		FactoryComponent.Builder factoryBuilder) {

		super(containerState, null);

		_configurationBuilder = configurationBuilder;
		_containerTracker = containerTracker;
		_singleBuilder = singleBuilder;
		_factoryBuilder = factoryBuilder;
		_log = containerState.containerLogs().getLogger(getClass());

		_serviceObjects = _containerTracker.getService();
		_seContainerInitializerInstance = _serviceObjects.getService();
	}

	@Override
	public boolean close() {
		try (Syncro syncro = _lock.open()) {
			if (_seContainer != null) {
				_log.debug(l -> l.debug("CCR container shutdown for {}", bundle()));
				_seContainer.close();
				try {
					_serviceObjects.ungetService(_seContainerInitializerInstance);
				}
				catch (Throwable t) {
					_log.trace(l -> l.trace("CCR Failure in returning initializer instance on {}", bundle(), t));
				}
				_seContainer = null;
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

			if (_seContainer != null) {
				return true;
			}

			if (containerState.containerDTO().components.isEmpty()) {
				return false;
			}

			_log.debug(log -> log.debug("CCR container startup for {}", bundle()));

			_seContainer = _seContainerInitializerInstance
				// always use a new class loader
				.setClassLoader(new BundleClassLoader(containerState.bundle(), containerState.extenderBundle()))
				.addBeanClasses(containerState.beansModel().getOSGiBeans().stream().map(OSGiBean::getBeanClass).toArray(Class<?>[]::new))
				.setProperties(containerState.containerComponentTemplateDTO().properties)
				.addProperty(BEANS_XML_PROPERTY, containerState.beansModel().getBeansXml())
				.addProperty(BUNDLECONTEXT_PROPERTY, bundle().getBundleContext())
				.addExtensions(getExtensions().toArray(new Extension[0]))
				.initialize();

			return true;
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONTAINER_BOOTSTRAP, containerState.id());
	}

	protected List<Extension> getExtensions() {
		List<Extension> extensions = new ArrayList<>();

		// Add the internal extensions
		extensions.add(new BundleContextExtension(containerState.bundleContext()));
		extensions.add(new RuntimeExtension(containerState, _configurationBuilder, _singleBuilder, _factoryBuilder));
		extensions.add(new LoggerExtension(containerState));

		// Add extensions found from the bundle's class loader, such as those in the Bundle-ClassPath
		ServiceLoader.load(Extension.class, containerState.classLoader()).forEach(extensions::add);

		// Add external extensions
		containerState.containerDTO().extensions.stream().map(
			ExtendedExtensionDTO.class::cast
		).map(
			e -> e.extension.getService()
		).forEach(extensions::add);

		return extensions;
	}

	private volatile SeContainer _seContainer;
	private final ServiceTracker<SeContainerInitializer, ServiceObjects<SeContainerInitializer>> _containerTracker;
	private final ConfigurationListener.Builder _configurationBuilder;
	private final FactoryComponent.Builder _factoryBuilder;
	private final SeContainerInitializer _seContainerInitializerInstance;
	private final ServiceObjects<SeContainerInitializer> _serviceObjects;
	private final SingleComponent.Builder _singleBuilder;
	private final Syncro _lock = new Syncro(true);
	private final Logger _log;

}