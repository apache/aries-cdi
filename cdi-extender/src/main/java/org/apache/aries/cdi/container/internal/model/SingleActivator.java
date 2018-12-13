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

package org.apache.aries.cdi.container.internal.model;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.container.internal.bean.ReferenceBean;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.container.Op;
import org.apache.aries.cdi.container.internal.container.Op.Mode;
import org.apache.aries.cdi.container.internal.util.Maps;
import org.apache.aries.cdi.container.internal.util.SRs;
import org.apache.aries.cdi.container.internal.util.Syncro;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.log.Logger;

public class SingleActivator extends InstanceActivator {

	public static class Builder extends InstanceActivator.Builder<Builder> {

		public Builder(ContainerState containerState) {
			super(containerState, null);
		}

		@Override
		public InstanceActivator build() {
			return _cache.computeIfAbsent(_instance, i -> new SingleActivator(this));
		}

		private final Map<ExtendedComponentInstanceDTO, SingleActivator> _cache = new ConcurrentHashMap<>();

	}

	private SingleActivator(Builder builder) {
		super(builder);
		_log = containerState.containerLogs().getLogger(getClass());
	}

	@Override
	public boolean close() {
		try (Syncro synchro = _lock.open()) {
			if (serviceRegistration != null) {
				serviceRegistration.unregister();
				serviceRegistration = null;
			}

			_instance.activations.removeIf(
				a -> {
					ExtendedActivationDTO extended = (ExtendedActivationDTO)a;
					Objects.requireNonNull(extended);
					Objects.requireNonNull(extended.onClose);
					extended.onClose.accept(extended);
					return true;
				}
			);

			_instance.active = false;

			return true;
		}
	}

	@Override
	public Op closeOp() {
		return Op.of(Mode.CLOSE, Op.Type.SINGLE_ACTIVATOR, _instance.ident());
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean open() {
		try (Syncro synchro = _lock.open()) {
			if (containerState.bundleContext() == null) {
				// this bundle was already removed
				return false;
			}

			if (!_instance.referencesResolved() || _instance.active) {
				return false;
			}

			final BeanManager beanManager = containerState.beanManager();

			if (beanManager == null) {
				return false;
			}

			ExtendedActivationTemplateDTO activationTemplate =
				(ExtendedActivationTemplateDTO)_instance.template.activations.get(0);

			_instance.template.references.stream().map(ExtendedReferenceTemplateDTO.class::cast).forEach(
				t -> {
					_instance.references.stream().filter(
						r -> r.template == t
					).findFirst().map(
						ExtendedReferenceDTO.class::cast
					).ifPresent(
						r -> {
							ReferenceBean bean = t.bean;
							bean.setBeanManager(beanManager);
							bean.setReferenceDTO(r);
						}
					);
				}
			);

			ExtendedComponentTemplateDTO extended = (ExtendedComponentTemplateDTO)_instance.template;

			Set<Bean<?>> beans = beanManager.getBeans(
				extended.bean.getBeanClass(), extended.bean.getQualifiers().toArray(new Annotation[0]));
			Bean<? extends Object> bean = beanManager.resolve(beans);

			if (activationTemplate.serviceClasses.isEmpty() /* immediate */) {
				activate(bean, activationTemplate, beanManager);

				_log.debug(l -> l.debug("CCR `immediate component` {} activated on {}", _instance.ident(), bundle()));
			}
			else if (activationTemplate.scope == ServiceScope.SINGLETON) {
				Entry<ExtendedActivationDTO, Object> entry = activate(
					bean, activationTemplate, beanManager);
				serviceRegistration = containerState.bundleContext().registerService(
					activationTemplate.serviceClasses.toArray(new String[0]),
					entry.getValue(),
					Maps.dict(_instance.properties));
				entry.getKey().service = SRs.from(serviceRegistration.getReference());

				_log.debug(l -> l.debug("CCR `singleton scope service` {} activated on {}", _instance.ident(), bundle()));
			}
			else if (activationTemplate.scope == ServiceScope.BUNDLE) {
				serviceRegistration = containerState.bundleContext().registerService(
					activationTemplate.serviceClasses.toArray(new String[0]),
					new ServiceFactory() {

						@Override
						public Object getService(Bundle bundle, ServiceRegistration registration) {
							Entry<ExtendedActivationDTO, Object> entry = activate(
								bean, activationTemplate, beanManager);
							entry.getKey().service = SRs.from(registration.getReference());
							_locals.put(entry.getValue(), entry.getKey());
							return entry.getValue();
						}

						@Override
						public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
							ExtendedActivationDTO activationDTO = _locals.remove(object);

							if (activationDTO != null) {
								activationDTO.onClose.accept(activationDTO);
							}
						}

						final Map<Object, ExtendedActivationDTO> _locals = new ConcurrentHashMap<>();

					},
					Maps.dict(_instance.properties)
				);

				_log.debug(l -> l.debug("CCR `bundle scope service` {} activated on {}", _instance.ident(), bundle()));
			}
			else if (activationTemplate.scope == ServiceScope.PROTOTYPE) {
				serviceRegistration = containerState.bundleContext().registerService(
					activationTemplate.serviceClasses.toArray(new String[0]),
					new PrototypeServiceFactory() {

						@Override
						public Object getService(Bundle bundle, ServiceRegistration registration) {
							Entry<ExtendedActivationDTO, Object> entry = activate(
								bean, activationTemplate, beanManager);
							entry.getKey().service = SRs.from(registration.getReference());
							_locals.put(entry.getValue(), entry.getKey());
							return entry.getValue();
						}

						@Override
						public void ungetService(Bundle bundle, ServiceRegistration registration, Object object) {
							ExtendedActivationDTO activationDTO = _locals.remove(object);

							if (activationDTO != null) {
								activationDTO.onClose.accept(activationDTO);
							}
						}

						final Map<Object, ExtendedActivationDTO> _locals = new ConcurrentHashMap<>();

					},
					Maps.dict(_instance.properties)
				);

				_log.debug(l -> l.debug("CCR `prototype scope service` {} activated on {}", _instance.ident(), bundle()));
			}

			_instance.active = true;

			return true;
		}
	}

	@Override
	public Op openOp() {
		return Op.of(Mode.OPEN, Op.Type.SINGLE_ACTIVATOR, _instance.ident());
	}

	@Override
	public String toString() {
		return Arrays.asList(getClass().getSimpleName(), _instance.ident()).toString();
	}

	private final Syncro _lock = new Syncro(true);
	private final Logger _log;
	private volatile ServiceRegistration<?> serviceRegistration;

}
