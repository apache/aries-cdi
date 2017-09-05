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

package org.apache.aries.cdi.test.beans;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.FieldInjectedReference;
import org.apache.aries.cdi.test.interfaces.PrototypeScoped;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Component;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceScope;
import org.osgi.service.cdi.annotations.ServiceScope;

@Component(
	service = {FieldInjectedPrototypeScopedImpl.class, FieldInjectedReference.class},
	serviceScope = ServiceScope.SINGLETON
)
public class FieldInjectedPrototypeScopedImpl implements FieldInjectedReference<PrototypeScoped> {

	@Inject
	@Reference(target = "(key=value)")
	private ServiceReference<PrototypeScoped> genericReference;

	@Inject
	@Reference(service = PrototypeScoped.class, target = "(key=value)")
	private Map<String, Object> properties;

	@Inject
	@Reference(
		scope = ReferenceScope.PROTOTYPE,
		service = PrototypeScoped.class,
		target = "(key=value)"
	)
	@SuppressWarnings("rawtypes")
	private ServiceReference rawReference;

	@Inject
	@Reference(target = "(key=value)")
	private PrototypeScoped service;

	@Override
	public ServiceReference<PrototypeScoped> getGenericReference() {
		return genericReference;
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public ServiceReference getRawReference() {
		return rawReference;
	}

	@Override
	public PrototypeScoped getService() {
		return service;
	}

	@PostConstruct
	private void postConstructed() {
		System.out.println("PostConstructed " + this);
	}

	@PreDestroy
	private void preDestroyed() {
		System.out.println("PreDestroyed " + this);
	}

}