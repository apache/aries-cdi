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

package org.apache.aries.cdi.extension.mp.metrics;

import static java.lang.String.format;
import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.aries.cdi.extension.mp.metrics.MPMetricsExtension.EXTENSION_NAME;
import static org.apache.aries.cdi.extension.mp.metrics.MPMetricsExtension.EXTENSION_VERSION;
import static org.osgi.framework.Constants.SCOPE_PROTOTYPE;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_SCOPE;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_RESOURCE;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.cdi.spi.configuration.Configuration;
import org.apache.geronimo.microprofile.metrics.cdi.MetricsExtension;
import org.apache.geronimo.microprofile.metrics.jaxrs.CdiMetricsEndpoints;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(
	attribute = {
		CDI_EXTENSION_PROPERTY + '=' + EXTENSION_NAME,
		SERVICE_SCOPE + '=' + SCOPE_PROTOTYPE,
		SERVICE_VENDOR + "=Apache Software Foundation",
		"version:Version=" + EXTENSION_VERSION
	},
	uses = Extension.class,
	value = Extension.class
)
public class MPMetricsExtension extends MetricsExtension {

	public final static String EXTENSION_NAME = "eclipse.microprofile.metrics";
	public final static String EXTENSION_VERSION = "1.1.1";

	private volatile BundleContext bundleContext;
	private volatile Configuration configuration;

	void getBundleContext(@Observes BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void registerMetricsEndpoint(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		if (bundleContext == null || configuration == null) {
			return;
		}

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP Metrics Portable Extension Endpoint");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, configuration.get(JAX_RS_APPLICATION_SELECT));
		properties.put(JAX_RS_RESOURCE, Boolean.TRUE.toString());
		properties.put(JAX_RS_EXTENSION_SELECT, new String[] {
			format("(&(objectClass=%s)(%s=%s))", MessageBodyReader.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON),
			format("(&(objectClass=%s)(%s=%s))", MessageBodyWriter.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON)
		});
		properties.put(SERVICE_RANKING, Integer.MAX_VALUE - 100);

		AnnotatedType<CdiMetricsEndpoints> annotatedType = beanManager.createAnnotatedType(CdiMetricsEndpoints.class);
		InjectionTargetFactory<CdiMetricsEndpoints> injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
		Bean<CdiMetricsEndpoints> bean = beanManager.createBean(beanManager.createBeanAttributes(annotatedType), CdiMetricsEndpoints.class, injectionTargetFactory);

		CdiMetricsEndpoints cdiMetricsEndpoints = bean.create(beanManager.createCreationalContext(bean));

		_endpointRegistration = bundleContext.registerService(
			CdiMetricsEndpoints.class, cdiMetricsEndpoints, properties);
	}

	void unregisterMetricsEndpoint(@Observes BeforeShutdown bs) {
		if (_endpointRegistration != null) {
			try {
				_endpointRegistration.unregister();
			}
			catch (IllegalStateException ise) {
				//
			}
		}
	}

	private volatile ServiceRegistration<?> _endpointRegistration;

}
