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

@Requirement(
	effective = "active",
	filter = "(objectClass=org.osgi.service.cm.ConfigurationAdmin)",
	namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@RequireCDIExtension("aries.cdi.http")
@RequireCDIExtension("aries.cdi.jaxrs")
@RequireCDIExtension("aries.cdi.jndi")
package org.apache.aries.cdi.test.cases.base;

import org.apache.aries.cdi.extra.RequireCDIExtension;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.namespace.service.ServiceNamespace;
