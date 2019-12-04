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

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static javax.interceptor.Interceptor.Priority.LIBRARY_BEFORE;
import static org.apache.aries.cdi.extension.mp.metrics.StubExtension.EXTENSION_NAME;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_RESOURCE;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.cdi.spi.configuration.Configuration;
import org.apache.geronimo.microprofile.metrics.cdi.CountedInterceptor;
import org.apache.geronimo.microprofile.metrics.cdi.MeteredInterceptor;
import org.apache.geronimo.microprofile.metrics.cdi.MetricsExtension;
import org.apache.geronimo.microprofile.metrics.cdi.TimedInterceptor;
import org.apache.geronimo.microprofile.metrics.jaxrs.CdiMetricsEndpoints;
import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(
	attribute = {
		CDI_EXTENSION_PROPERTY + "=" + EXTENSION_NAME,
		"service.scope=prototype",
		"service.vendor=Apache Software Foundation",
		"version:Version=1.1.1"
	},
	effective = "active",
	uses = Extension.class,
	value = Extension.class
)
public class StubExtension extends MetricsExtension {

	public final static String EXTENSION_NAME = "eclipse.microprofile.metrics";

	private volatile Configuration configuration;

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void addExtensionBeans(
		@Priority(LIBRARY_BEFORE + 800)
		@Observes final AfterBeanDiscovery abd, final BeanManager bm) {

		abd.addBean().beanClass(CountedInterceptor.class).createWith(c -> new CountedInterceptor());
		abd.addBean().beanClass(MeteredInterceptor.class).createWith(c -> new MeteredInterceptor());
		abd.addBean().beanClass(TimedInterceptor.class).createWith(c -> new TimedInterceptor());
		abd.addBean().beanClass(CdiMetricsEndpoints.class).createWith(c -> new CdiMetricsEndpoints());
	}

	void registerMetricsEndpoint(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		beanManager.getEvent().fireAsync(new Ready());
	}

	void registerMetricsEndpoint0(
		@ObservesAsync Ready ready, BeanManager beanManager, BundleContext bundleContext) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP Metrics JSON Provider");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, configuration.get(JAX_RS_APPLICATION_SELECT));
		properties.put(JAX_RS_EXTENSION, Boolean.TRUE.toString());
		properties.put(JAX_RS_MEDIA_TYPE, MediaType.APPLICATION_JSON);
		properties.put(JAX_RS_NAME, "metrics.json.provider");
		properties.put(SERVICE_RANKING, Integer.MAX_VALUE - 100);

		JsonbJaxrsProvider<?> johnzonProvider = new JsonbJaxrsProvider<>();

		_jsonProviderRegistration = bundleContext.registerService(
			new String[] {MessageBodyReader.class.getName(), MessageBodyWriter.class.getName()}, johnzonProvider, properties);

		properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP Metrics Portable Extension Endpoint");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, configuration.get(JAX_RS_APPLICATION_SELECT));
		properties.put(JAX_RS_RESOURCE, Boolean.TRUE.toString());
		properties.put(JAX_RS_EXTENSION_SELECT, "(osgi.jaxrs.name=metrics.json.provider)");
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
				// the service was already unregistered.
			}
		}
		if (_jsonProviderRegistration != null) {
			try {
				_jsonProviderRegistration.unregister();
			}
			catch (IllegalStateException ise) {
				// the service was already unregistered.
			}
		}
	}

	public static class Ready {}

	private volatile ServiceRegistration<?> _jsonProviderRegistration;
	private volatile ServiceRegistration<?> _endpointRegistration;

}
