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

package org.apache.aries.cdi.container.test.beans;

import java.util.Collection;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.Configuration;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.service.cdi.annotations.ReferencePolicy;
import org.osgi.service.cdi.annotations.ReferencePolicyOption;
import org.osgi.service.cdi.annotations.ReferenceScope;

public class BarAnnotated {

	@Inject
	@Reference(cardinality = ReferenceCardinality.OPTIONAL)
	Foo foo;

	@Inject
	@Reference(name = "foos")
	Instance<Foo> instanceFoos;

	@Inject
	@Reference(policy = ReferencePolicy.DYNAMIC)
	Collection<Foo> collectionFoos;

	@Inject
	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	Collection<Map.Entry<Map<String, Object>, Foo>> tupleFoos;

	@Inject
	@Reference(scope = ReferenceScope.PROTOTYPE)
	Collection<ServiceReference<Foo>> serviceReferencesFoos;

	@Inject
	@Reference(service = Foo.class)
	Collection<Map<String, Object>> propertiesFoos;

	@Inject
	@Configuration("foo.config")
	Config config;

}
