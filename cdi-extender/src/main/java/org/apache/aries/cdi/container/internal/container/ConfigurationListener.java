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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.container.Op.Type;
import org.apache.aries.cdi.container.internal.model.Component;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentInstanceDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationDTO;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.Predicates;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.apache.aries.cdi.container.internal.util.Throw;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.log.Logger;
import org.osgi.util.promise.Promise;

public class ConfigurationListener extends Phase implements org.osgi.service.cm.ConfigurationListener {

	public static class Builder {

		public Builder(ContainerState containerState) {
			_containerState = containerState;
		}

		public Builder component(Component component) {
			_component = component;
			return this;
		}

		public ConfigurationListener build() {
			Objects.requireNonNull(_component);
			return new ConfigurationListener(_containerState, _component);
		}

		private Component _component;
		private final ContainerState _containerState;

	}

	protected ConfigurationListener(
		ContainerState containerState,
		Component component) {

		super(containerState, component);
		_component = component;
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		try (Syncro open = syncro.open()) {
			if (_listenerService != null) {
				_listenerService.unregister();
				_listenerService = null;
			}

			return next.map(
				next -> {
					submit(next.closeOp(), next::close).onFailure(
						f -> {
							_log.error(l -> l.error("CCR Failure in configuration listener close on {}", next, f));

							error(f);
						}
					);

					return true;
				}
			).orElse(true);
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Type.CONFIGURATION_LISTENER, _component.template().name);
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		next.map(next -> (Component)next).ifPresent(
			next -> next.configurationTemplates().stream().filter(
				t -> Predicates.isMatchingConfiguration(event).test(t)
			).findFirst().ifPresent(
				t -> {
					String eventString = Arrays.asList(event.getPid(), event.getFactoryPid(), type(event)).toString();

					Promise<Boolean> result = containerState.submit(
						Op.of(Mode.OPEN, Type.CONFIGURATION_EVENT, eventString),
						() -> {
							_log.debug(l -> l.debug("CCR Event {} matched {} because of {}", eventString, _component.template().name, _component.template().configurations));
							processEvent(next, t, event);
							return true;
						}
					);

					try {
						result.getValue();
					}
					catch (Exception e) {
						Throw.exception(e);
					}
				}
			)
		);
	}

	@Override
	public boolean open() {
		try (Syncro open = syncro.open()) {
			if (containerState.bundleContext() == null) {
				// this bundle was already removed
				return false;
			}

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("name", toString());
			properties.put(Constants.SERVICE_DESCRIPTION, "Aries CDI - Configuration Listener for " + containerState.bundle());
			properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

			_listenerService = containerState.bundleContext().registerService(
				org.osgi.service.cm.ConfigurationListener.class, this, properties);

			return next.map(next -> (Component)next).map(
				component -> {
					submit(component.openOp(), component::open).then(
						s -> {
							component.configurationTemplates().stream().filter(
								ct -> Objects.nonNull(ct.pid)
							).forEach(
								template -> {
									if (template.maximumCardinality == MaximumCardinality.ONE) {
										containerState.findConfig(template.pid).ifPresent(
											c -> processEvent(
												component,
												template,
												new ConfigurationEvent(
													containerState.caTracker().getServiceReference(),
													ConfigurationEvent.CM_UPDATED,
													null,
													c.getPid()))
										);
									}
									else {
										containerState.findConfigs(template.pid, true).ifPresent(
											arr -> Arrays.stream(arr).forEach(
												c -> processEvent(
													component,
													template,
													new ConfigurationEvent(
														containerState.caTracker().getServiceReference(),
														ConfigurationEvent.CM_UPDATED,
														c.getFactoryPid(),
														c.getPid()))
											)
										);
									}
								}
							);

							return s;
						},
						f -> {
							_log.error(l -> l.error("CCR Failure during configuration start on {}", next, f.getFailure()));

							error(f.getFailure());
						}
					);

					return true;
				}
			).orElse(true);
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Type.CONFIGURATION_LISTENER, _component.template().name);
	}

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), _component).toString();
	}

	private void processEvent(Component component, ConfigurationTemplateDTO t, ConfigurationEvent event) {
		boolean required = t.policy == ConfigurationPolicy.REQUIRED;
		boolean single = t.maximumCardinality == MaximumCardinality.ONE;

		switch (event.getType()) {
			case ConfigurationEvent.CM_DELETED:
				component.instances().stream().map(
					ExtendedComponentInstanceDTO.class::cast
				).filter(
					instance -> (!single && event.getPid().equals(instance.pid)) || single
				).forEach(
					instance -> {
						submit(instance.closeOp(), instance::close).then(
							s -> {
								if (!required) {
									instance.configurations.removeIf(
										c -> c.template == t
									);

									submit(instance.openOp(), instance::open);
								}
								else {
									component.instances().remove(instance);
								}

								return s;
							}
						);
					}
				);
				return;
			case ConfigurationEvent.CM_LOCATION_CHANGED:
				break;
			case ConfigurationEvent.CM_UPDATED:
				if (!single &&
					!component.instances().stream().map(
						ExtendedComponentInstanceDTO.class::cast
					).filter(
						instance -> event.getPid().equals(instance.pid)
					).findFirst().isPresent()) {

					ExtendedComponentInstanceDTO instance = new ExtendedComponentInstanceDTO(
						containerState, _component.activatorBuilder());
					instance.activations = new CopyOnWriteArrayList<>();
					instance.configurations = new CopyOnWriteArrayList<>();
					instance.pid = event.getPid();
					instance.references = new CopyOnWriteArrayList<>();
					instance.template = component.template();

					component.instances().add(instance);
				}

				containerState.findConfig(event.getPid()).ifPresent(
					configuration -> {
						ExtendedConfigurationDTO configurationDTO = new ExtendedConfigurationDTO();

						configurationDTO.configuration = configuration;
						configurationDTO.pid = configuration.getPid();
						configurationDTO.properties = Maps.of(configuration.getProcessedProperties(event.getReference()));
						configurationDTO.template = t;

						component.instances().stream().map(
							ExtendedComponentInstanceDTO.class::cast
						).filter(
							instance -> (!single && event.getPid().equals(instance.pid)) || single
						).forEach(
							instance -> {
								submit(instance.closeOp(), instance::close).then(
									s -> {
										instance.configurations.removeIf(c -> c.template == t);
										instance.configurations.add(configurationDTO);

										submit(instance.openOp(), instance::open);

										return s;
									}
								);
							}
						);
					}
				);
				break;
		}
	}

	private String type(ConfigurationEvent event) {
		if (event.getType() == ConfigurationEvent.CM_DELETED)
			return "DELETED";
		if (event.getType() == ConfigurationEvent.CM_LOCATION_CHANGED)
			return "LOCATION_CHANGED";
		if (event.getType() == ConfigurationEvent.CM_UPDATED)
			return "UPDATED";
		throw new IllegalArgumentException("CM Event type " + event.getType());
	}

	private volatile ServiceRegistration<org.osgi.service.cm.ConfigurationListener> _listenerService;

	private final Component _component;
	private final Logger _log;

}