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
		javax.enterprise.inject.spi.Extension.class
	},
	version = EXTENSION_VERSION,
	attribute = {
		"aries.cdi.extension.bean.classes:List<String>='"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jwt.ContextualJsonWebToken,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jwt.DateValidator,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.config.GeronimoJwtAuthConfig,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.GeronimoJwtAuthExceptionMapper,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.servlet.GeronimoJwtAuthFilter,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.GroupMapper,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.JAXRSRequestForwarder,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jwt.JwtParser,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.servlet.JwtRequest,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jwt.KidMapper,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.ResponseBuilder,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.RolesAllowedFeature,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.jwt.SignatureValidator,"
			+ "org.apache.geronimo.microprofile.impl.jwtauth.servlet.TokenAccessor'"
	}
)
@JSONRequired
@RequireCDIExtension("aries.cdi.http")
@RequireCDIExtension("aries.cdi.jaxrs")
@RequireCDIExtender
@RequireJaxrsWhiteboard
package org.apache.aries.cdi.extension.mp.jwt;

import static org.apache.aries.cdi.extension.mp.jwt.MPJwtAuthExtension.EXTENSION_NAME;
import static org.apache.aries.cdi.extension.mp.jwt.MPJwtAuthExtension.EXTENSION_VERSION;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import org.apache.aries.cdi.extra.RequireCDIExtension;
import org.apache.aries.cdi.extra.propertytypes.JSONRequired;
import org.osgi.annotation.bundle.Capability;
import org.osgi.service.cdi.annotations.RequireCDIExtender;
import org.osgi.service.jaxrs.whiteboard.annotations.RequireJaxrsWhiteboard;
