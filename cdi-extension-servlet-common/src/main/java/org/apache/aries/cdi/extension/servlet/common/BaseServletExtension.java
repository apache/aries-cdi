/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.cdi.extension.servlet.common;

import static java.util.Optional.ofNullable;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extension.spi.adapt.ProcessPotentialService;
import org.apache.aries.cdi.extension.spi.adapt.FiltersOn;
import org.apache.aries.cdi.extension.spi.adapt.RegisterExtension;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterDispatcher;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterPattern;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterServlet;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardListener;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletMultipart;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletPattern;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.framework.ServiceRegistration;

public class BaseServletExtension implements Extension {
	protected Configuration configuration;
	protected volatile ServiceRegistration<?> _listenerRegistration;
	protected final AtomicBoolean destroyed = new AtomicBoolean(false);

	void register(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager manager) {
		manager.fireEvent(new RegisterExtension(this));
	}

	void setConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void webFilter(@Observes @FiltersOn(annotations = WebFilter.class) ProcessPotentialService pat,
				   BeanManager beanManager) {
		beanManager.fireEvent(MergeServiceTypes.forEvent(pat).withTypes(Filter.class).build());
		final AnnotatedTypeConfigurator<?> configurator = pat.configureAnnotatedType();
		final AnnotatedType<?> annotatedType = pat.getAnnotatedType();

		WebFilter webFilter = annotatedType.getAnnotation(WebFilter.class);

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String) configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
					select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webFilter.description().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webFilter.description()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterName.class) && !webFilter.filterName().isEmpty()) {
			configurator.add(HttpWhiteboardFilterName.Literal.of(webFilter.filterName()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterServlet.class) && webFilter.servletNames().length > 0) {
			configurator.add(HttpWhiteboardFilterServlet.Literal.of(webFilter.servletNames()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterPattern.class)) {
			if (webFilter.value().length > 0) {
				configurator.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.value()));
			} else if (webFilter.urlPatterns().length > 0) {
				configurator.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.urlPatterns()));
			}
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterDispatcher.class) && webFilter.dispatcherTypes().length > 0) {
			configurator.add(HttpWhiteboardFilterDispatcher.Literal.of(webFilter.dispatcherTypes()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterAsyncSupported.class)) {
			configurator.add(HttpWhiteboardFilterAsyncSupported.Literal.of(webFilter.asyncSupported()));
		}
	}

	void webListener(@Observes @FiltersOn(annotations = WebListener.class) ProcessPotentialService pat,
						 BeanManager beanManager) {
		final AnnotatedType<?> annotatedType = pat.getAnnotatedType();
		final Class<?> javaClass = annotatedType.getJavaClass();
		final Class<?>[] serviceTypes = Stream.of(
				ServletContextListener.class,
				ServletContextAttributeListener.class,
				ServletRequestListener.class,
				ServletRequestAttributeListener.class,
				HttpSessionListener.class,
				HttpSessionAttributeListener.class,
				HttpSessionIdListener.class)
				.filter(c -> c.isAssignableFrom(javaClass))
				.toArray(Class[]::new);

		beanManager.fireEvent(MergeServiceTypes.forEvent(pat).withTypes(serviceTypes).build());

		AnnotatedTypeConfigurator<?> configurator = pat.configureAnnotatedType();

		WebListener webListener = annotatedType.getAnnotation(WebListener.class);

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String) configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
					select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardListener.class)) {
			configurator.add(HttpWhiteboardListener.Literal.INSTANCE);
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webListener.value().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webListener.value()));
		}
	}

	void webServlet(@Observes @FiltersOn(annotations = WebServlet.class) ProcessPotentialService pat,
						BeanManager beanManager) {
		beanManager.fireEvent(MergeServiceTypes.forEvent(pat).withTypes(Servlet.class).build());

		final AnnotatedTypeConfigurator<?> configurator = pat.configureAnnotatedType();
		final AnnotatedType<?> annotatedType = pat.getAnnotatedType();
		WebServlet webServlet = annotatedType.getAnnotation(WebServlet.class);

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String) configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
					select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletName.class) && !webServlet.name().isEmpty()) {
			configurator.add(HttpWhiteboardServletName.Literal.of(webServlet.name()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletPattern.class)) {
			if (webServlet.value().length > 0) {
				configurator.add(HttpWhiteboardServletPattern.Literal.of(webServlet.value()));
			} else if (webServlet.urlPatterns().length > 0) {
				configurator.add(HttpWhiteboardServletPattern.Literal.of(webServlet.urlPatterns()));
			}
		}

		if (!annotatedType.isAnnotationPresent(ServiceRanking.class)) {
			configurator.add(ServiceRanking.Literal.of(webServlet.loadOnStartup()));
		}

		// TODO Howto: INIT PARAMS ???

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletAsyncSupported.class)) {
			configurator.add(HttpWhiteboardServletAsyncSupported.Literal.of(webServlet.asyncSupported()));
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webServlet.description().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webServlet.description()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletMultipart.class)) {
			MultipartConfig multipartConfig = annotatedType.getAnnotation(MultipartConfig.class);

			if (multipartConfig != null) {
				configurator.add(HttpWhiteboardServletMultipart.Literal.of(true, multipartConfig.fileSizeThreshold(), multipartConfig.location(), multipartConfig.maxFileSize(), multipartConfig.maxRequestSize()));
			}
		}

		// TODO HowTo: ServletSecurity ???
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		if (_listenerRegistration != null && !destroyed.get()) {
			try {
				_listenerRegistration.unregister();
			} catch (IllegalStateException ise) {
				// the service was already unregistered.
			}
		}
	}
}
