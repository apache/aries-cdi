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

@Capability(
	attribute = "objectClass:List<String>=javax.enterprise.inject.spi.Extension",
	namespace = SERVICE_NAMESPACE
)
@Capability(
	namespace = CDI_EXTENSION_PROPERTY,
	name = EXTENSION_NAME,
	uses= {
		javax.annotation.Priority.class,
		javax.enterprise.event.Observes.class,
		Extension.class
	},
	version = EXTENSION_VERSION,
	attribute = {
		"aries.cdi.extension.bean.classes:List<String>='org.apache.geronimo.config.cdi.ConfigInjectionProducer'"
	}
)
@RequireCDIExtender
package org.apache.aries.cdi.extension.mp.config;

import static org.apache.aries.cdi.extension.mp.config.MPConfigExtension.EXTENSION_NAME;
import static org.apache.aries.cdi.extension.mp.config.MPConfigExtension.EXTENSION_VERSION;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import javax.enterprise.inject.spi.Extension;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.cdi.annotations.RequireCDIExtender;
