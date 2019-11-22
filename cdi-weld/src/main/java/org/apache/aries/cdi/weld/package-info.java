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

@org.osgi.annotation.bundle.Capability(
	attribute = {
		"objectClass:List<String>=javax.enterprise.inject.se.SeContainerInitializer",
		"aries.cdi.spi=Weld"
	},
	namespace = org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE
)
@org.osgi.service.cdi.annotations.RequireCDIImplementation
package org.apache.aries.cdi.weld;