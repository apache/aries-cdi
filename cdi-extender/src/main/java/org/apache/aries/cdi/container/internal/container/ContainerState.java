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

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.aries.cdi.container.internal.util.Filters.asFilter;
import static org.apache.aries.cdi.container.internal.util.Throw.asString;
import static org.apache.aries.cdi.container.internal.util.Throw.exception;
import static org.osgi.framework.Constants.SERVICE_BUNDLEID;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_FILTER_DIRECTIVE;
import static org.osgi.service.cdi.CDIConstants.CDI_CAPABILITY_NAME;
import static org.osgi.service.cdi.CDIConstants.CDI_CONTAINER_ID;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.cdi.ComponentType.CONTAINER;
import static org.osgi.service.cdi.ConfigurationPolicy.OPTIONAL;
import static org.osgi.service.cdi.MaximumCardinality.ONE;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.ChangeCount;
import org.apache.aries.cdi.container.internal.loader.BundleClassLoader;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.BeansModelBuilder;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedExtensionTemplateDTO;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerState {

	public ContainerState(
		Bundle bundle,
		Bundle extenderBundle,
		ChangeCount ccrChangeCount,
		PromiseFactory promiseFactory,
		ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker,
		Logs ccrLogs) {

		_bundle = bundle;
		_extenderBundle = extenderBundle;
		_ccrLogs = ccrLogs;
		_bundleContext = bundle.getBundleContext();
		_log = _ccrLogs.getLogger(getClass());
		_containerLogs = new Logs.Builder(_bundleContext).build();

		_changeCount = new ChangeCount();
		_changeCount.addObserver(ccrChangeCount);

		_promiseFactory = promiseFactory;
		_caTracker = caTracker;

		BundleWiring bundleWiring = _bundle.adapt(BundleWiring.class);

		Map<String, Object> cdiAttributes = emptyMap();

		for (BundleWire wire : bundleWiring.getRequiredWires(EXTENDER_NAMESPACE)) {
			BundleCapability capability = wire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String extender = (String)attributes.get(EXTENDER_NAMESPACE);

			if (extender.equals(CDI_CAPABILITY_NAME)) {
				BundleRequirement requirement = wire.getRequirement();
				cdiAttributes = requirement.getAttributes();
				break;
			}
		}

		_cdiAttributes = unmodifiableMap(cdiAttributes);

		Set<String> extensionRequirements = new LinkedHashSet<>();

		collectExtensionRequirements(bundleWiring, extensionRequirements);

		_containerDTO = new ContainerDTO();
		_containerDTO.bundle = _bundle.adapt(BundleDTO.class);
		_containerDTO.changeCount = _changeCount.get();
		_containerDTO.components = new CopyOnWriteArrayList<>();
		_containerDTO.errors = new CopyOnWriteArrayList<>();
		_containerDTO.extensions = new CopyOnWriteArrayList<>();
		_containerDTO.template = new ContainerTemplateDTO();
		_containerDTO.template.components = new CopyOnWriteArrayList<>();
		_containerDTO.template.extensions = new CopyOnWriteArrayList<>();
		_containerDTO.template.id = ofNullable(
			(String)_cdiAttributes.get(CDI_CONTAINER_ID)
		).orElse(
			_bundle.getSymbolicName()
		);

		extensionRequirements.forEach(
			extensionFilter -> {
				ExtendedExtensionTemplateDTO extensionTemplateDTO = new ExtendedExtensionTemplateDTO();

				try {
					extensionTemplateDTO.filter = asFilter(extensionFilter);
					extensionTemplateDTO.serviceFilter = extensionFilter;

					_containerDTO.template.extensions.add(extensionTemplateDTO);
				}
				catch (Exception e) {
					_containerDTO.errors.add(asString(e));
				}
			}
		);

		_containerComponentTemplateDTO = new ComponentTemplateDTO();
		_containerComponentTemplateDTO.activations = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.beans = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.configurations = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.name = _containerDTO.template.id;
		_containerComponentTemplateDTO.properties = emptyMap();
		_containerComponentTemplateDTO.references = new CopyOnWriteArrayList<>();
		_containerComponentTemplateDTO.type = CONTAINER;

		ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();
		configurationTemplate.maximumCardinality = ONE;
		configurationTemplate.pid = ofNullable(
			(String)_cdiAttributes.get(CDI_CONTAINER_ID)
		).map(
			s -> s.replaceAll("-", ".")
		).orElse(
			"osgi.cdi." + _bundle.getSymbolicName().replaceAll("-", ".")
		);
		configurationTemplate.policy = OPTIONAL;

		_containerComponentTemplateDTO.configurations.add(configurationTemplate);

		_containerDTO.template.components.add(_containerComponentTemplateDTO);

		_aggregateClassLoader = new BundleClassLoader(_bundle, _extenderBundle);

		_beansModel = new BeansModelBuilder(this, _aggregateClassLoader, bundleWiring, _cdiAttributes).build();

		try {
			new Discovery(this).discover();
		}
		catch (Exception e) {
			_log.error(l -> l.error("CCR Discovery resulted in errors on {}", bundle, e));

			_containerDTO.errors.add(asString(e));
		}

		_beanManagerDeferred = _promiseFactory.deferred();
	}

	public <T, R> Promise<R> addCallback(CheckedCallback<T, R> checkedCallback) {
		Deferred<R> deferred = _promiseFactory.deferred();
		_callbacks.put(checkedCallback, deferred);
		return deferred.getPromise();
	}

	public BeanManager beanManager() {
		try {
			return _beanManagerDeferred.getPromise().timeout(5000).getValue();
		} catch (InvocationTargetException | InterruptedException e) {
			return exception(e);
		}
	}

	public void beanManager(BeanManager beanManager) {
		if (_beanManagerDeferred.getPromise().isDone()) {
			_beanManagerDeferred = _promiseFactory.deferred();
		}
		_beanManagerDeferred.resolve(beanManager);
	}

	public BeansModel beansModel() {
		return _beansModel;
	}

	public Bundle bundle() {
		return _bundle;
	}

	public BundleContext bundleContext() {
		return _bundleContext;
	}

	public ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> caTracker() {
		return _caTracker;
	}

	public Logs ccrLogs() {
		return _ccrLogs;
	}

	public Map<String, Object> cdiAttributes() {
		return _cdiAttributes;
	}

	public BundleClassLoader classLoader() {
		return _aggregateClassLoader;
	}

	public void closing() {
		try {
			_closing.set(_promiseFactory.submit(() -> Boolean.TRUE).getValue());
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public ComponentContext componentContext() {
		return _componentContext;
	}

	public ComponentTemplateDTO containerComponentTemplateDTO() {
		return _containerComponentTemplateDTO;
	}

	public ContainerDTO containerDTO() {
		_containerDTO.changeCount = _changeCount.get();
		return _containerDTO;
	}

	public Logs containerLogs() {
		return _containerLogs;
	}

	public void error(Throwable t) {
		containerDTO().errors.add(Throw.asString(t));
	}

	public Bundle extenderBundle() {
		return _extenderBundle;
	}

	public Optional<Configuration> findConfig(String pid) {
		return findConfigs(pid, false).map(arr -> arr[0]);
	}

	public Optional<Configuration[]> findConfigs(String pid, boolean factory) {
		try {
			String query = "(service.pid=".concat(pid).concat(")");

			if (factory) {
				query = "(service.factoryPid=".concat(pid).concat(")");
			}

			ConfigurationAdmin cm = _caTracker.getService();

			if (cm == null) {
				_log.error(l -> l.error("CCR unexpected error fetching configuration admin for {}", pid));

				return empty();
			}

			return ofNullable(cm.listConfigurations(query));
		}
		catch (Exception e) {
			_log.warn(l -> l.warn("CCR unexpected error fetching configuration for {}", pid, e));

			return empty();
		}
	}

	public String id() {
		return _containerDTO.template.id;
	}

	public void incrementChangeCount() {
		_changeCount.incrementAndGet();
	}

	public PromiseFactory promiseFactory() {
		return _promiseFactory;
	}

	@SuppressWarnings("unchecked")
	public <T, R> Promise<T> submit(Op op, Callable<T> task) {
		try {
			switch (op.mode) {
				case CLOSE: {
					// always perform close synchronously
					_log.debug(l -> l.debug("CCR submit {}", op));
					return _promiseFactory.resolved(task.call());
				}
				case OPEN:
					// when closing don't do perform any opens
					// also, don't log it since it's just going to be noise
					if (_closing.get()) {
						return _promiseFactory.resolved((T)new Object());
					}
			}
		}
		catch (Exception e) {
			return _promiseFactory.failed(e);
		}

		_log.debug(l -> l.debug("CCR submit {}", op));

		Promise<T> promise = _promiseFactory.submit(task);

		for (Entry<CheckedCallback<?, ?>, Deferred<?>> entry : _callbacks.entrySet()) {
			CheckedCallback<T, R> cc = (CheckedCallback<T, R>)entry.getKey();
			if (cc.test(op)) {
				((Deferred<R>)entry.getValue()).resolveWith(promise.then(cc, cc)).then(
					s -> {
						_callbacks.remove(cc);
						return s;
					},
					f -> _callbacks.remove(cc)
				);
			}
		}

		return promise;
	}

	@Override
	public String toString() {
		return _bundle.toString();
	}

	private void collectExtensionRequirements(BundleWiring bundleWiring, Set<String> extensionRequirements) {
		for (BundleWire wire : bundleWiring.getRequiredWires(CDI_EXTENSION_PROPERTY)) {
			String filter = wire.getRequirement().getDirectives().get(
				REQUIREMENT_FILTER_DIRECTIVE);
			Bundle extensionProvider = wire.getProvider().getBundle();

			StringBuilder sb = new StringBuilder();

			sb.append("(&");
			sb.append(filter);
			sb.append("(");
			sb.append(SERVICE_BUNDLEID);
			sb.append("=");
			sb.append(extensionProvider.getBundleId());
			sb.append("))");

			if (extensionRequirements.add(sb.toString())) {
				collectExtensionRequirements(
					extensionProvider.adapt(BundleWiring.class), extensionRequirements);
			}
		}
	}

	private final BundleClassLoader _aggregateClassLoader;
	private volatile Deferred<BeanManager> _beanManagerDeferred;
	private final BeansModel _beansModel;
	private final Bundle _bundle;
	private final BundleContext _bundleContext;
	private final Map<CheckedCallback<?, ?>, Deferred<?>> _callbacks = new ConcurrentHashMap<>();
	private final ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> _caTracker;
	private final Logger _log;
	private final Logs _ccrLogs;
	private final Map<String, Object> _cdiAttributes;
	private final ChangeCount _changeCount;
	private final AtomicBoolean _closing = new AtomicBoolean(false);
	private final ComponentContext _componentContext = new ComponentContext();
	private final ContainerDTO _containerDTO;
	private final Logs _containerLogs;
	private final ComponentTemplateDTO _containerComponentTemplateDTO;
	private final Bundle _extenderBundle;
	private final PromiseFactory _promiseFactory;

}
