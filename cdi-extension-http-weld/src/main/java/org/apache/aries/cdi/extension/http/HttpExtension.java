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

package org.apache.aries.cdi.extension.http;

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

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
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.annotations.Service;

public class HttpExtension implements Extension {

	private volatile Configuration configuration;

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	<X> void processWebFilter(
		@Observes @WithAnnotations(WebFilter.class) ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebFilter webFilter = annotatedType.getAnnotation(WebFilter.class);

		final Set<Annotation> annotationsToAdd = new HashSet<>();

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotationsToAdd.add(Service.Literal.of(new Class[] {Filter.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotationsToAdd.add(HttpWhiteboardContextSelect.Literal.of((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)));
		}

		if (!webFilter.description().isEmpty()) {
			annotationsToAdd.add(ServiceDescription.Literal.of(webFilter.description()));
		}

		if (!webFilter.filterName().isEmpty()) {
			annotationsToAdd.add(HttpWhiteboardFilterName.Literal.of(webFilter.filterName()));
		}

		if (webFilter.servletNames().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterServlet.Literal.of(webFilter.servletNames()));
		}

		if (webFilter.value().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.value()));
		}
		else if (webFilter.urlPatterns().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.urlPatterns()));
		}

		if (webFilter.dispatcherTypes().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterDispatcher.Literal.of(webFilter.dispatcherTypes()));
		}

		annotationsToAdd.add(HttpWhiteboardFilterAsyncSupported.Literal.of(webFilter.asyncSupported()));

		if (!annotationsToAdd.isEmpty()) {
			annotationsToAdd.forEach(pat.configureAnnotatedType()::add);
		}
	}

	<X> void processWebListener(
		@Observes @WithAnnotations(WebListener.class) ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebListener webListener = annotatedType.getAnnotation(WebListener.class);

		final Set<Annotation> annotationsToAdd = new HashSet<>();

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			List<Class<?>> listenerTypes = new ArrayList<>();

			Class<X> javaClass = annotatedType.getJavaClass();

			if (javax.servlet.ServletContextListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletContextListener.class);
			}
			if (javax.servlet.ServletContextAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletContextAttributeListener.class);
			}
			if (javax.servlet.ServletRequestListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletRequestListener.class);
			}
			if (javax.servlet.ServletRequestAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.ServletRequestAttributeListener.class);
			}
			if (javax.servlet.http.HttpSessionListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionListener.class);
			}
			if (javax.servlet.http.HttpSessionAttributeListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionAttributeListener.class);
			}
			if (javax.servlet.http.HttpSessionIdListener.class.isAssignableFrom(javaClass)) {
				listenerTypes.add(javax.servlet.http.HttpSessionIdListener.class);
			}

			annotationsToAdd.add(Service.Literal.of(listenerTypes.toArray(new Class<?>[0])));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotationsToAdd.add(HttpWhiteboardContextSelect.Literal.of((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)));
		}

		annotationsToAdd.add(HttpWhiteboardListener.Literal.INSTANCE);

		if (!webListener.value().isEmpty()) {
			annotationsToAdd.add(ServiceDescription.Literal.of(webListener.value()));
		}

		if (!annotationsToAdd.isEmpty()) {
			annotationsToAdd.forEach(pat.configureAnnotatedType()::add);
		}
	}

	<X> void processWebServlet(
		@Observes @WithAnnotations(WebServlet.class) ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebServlet webServlet = annotatedType.getAnnotation(WebServlet.class);

		final Set<Annotation> annotationsToAdd = new HashSet<>();

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotationsToAdd.add(Service.Literal.of(new Class[] {Servlet.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotationsToAdd.add(HttpWhiteboardContextSelect.Literal.of((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)));
		}

		if (!webServlet.name().isEmpty()) {
			annotationsToAdd.add(HttpWhiteboardServletName.Literal.of(webServlet.name()));
		}

		if (webServlet.value().length > 0) {
			annotationsToAdd.add(HttpWhiteboardServletPattern.Literal.of(webServlet.value()));
		}
		else if (webServlet.urlPatterns().length > 0) {
			annotationsToAdd.add(HttpWhiteboardServletPattern.Literal.of(webServlet.urlPatterns()));
		}

		annotationsToAdd.add(ServiceRanking.Literal.of(webServlet.loadOnStartup()));

		// TODO Howto: INIT PARAMS ???

		annotationsToAdd.add(HttpWhiteboardServletAsyncSupported.Literal.of(webServlet.asyncSupported()));

		if (!webServlet.description().isEmpty()) {
			annotationsToAdd.add(ServiceDescription.Literal.of(webServlet.description()));
		}

		MultipartConfig multipartConfig = annotatedType.getAnnotation(MultipartConfig.class);

		if (multipartConfig != null) {
			annotationsToAdd.add(HttpWhiteboardServletMultipart.Literal.of(true, multipartConfig.fileSizeThreshold(), multipartConfig.location(), multipartConfig.maxFileSize(), multipartConfig.maxRequestSize()));
		}

		// TODO HowTo: ServletSecurity ???

		if (!annotationsToAdd.isEmpty()) {
			annotationsToAdd.forEach(pat.configureAnnotatedType()::add);
		}
	}

	void afterDeploymentValidation(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		beanManager.getEvent().fireAsync(new Ready());
	}

	void ready(
		@ObservesAsync Ready ready, BeanManager beanManager, BundleContext bundleContext) {

		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - HTTP Portable Extension for Weld");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT, configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT));
		properties.put(HTTP_WHITEBOARD_LISTENER, Boolean.TRUE.toString());
		properties.put(SERVICE_RANKING, Integer.MAX_VALUE - 100);

		AnnotatedType<WeldInitialListener> annotatedType = beanManager.createAnnotatedType(WeldInitialListener.class);
		InjectionTargetFactory<WeldInitialListener> injectionTargetFactory = beanManager.getInjectionTargetFactory(annotatedType);
		Bean<WeldInitialListener> bean = beanManager.createBean(beanManager.createBeanAttributes(annotatedType), WeldInitialListener.class, injectionTargetFactory);

		WeldInitialListener initialListener = bean.create(beanManager.createCreationalContext(bean));

		_listenerRegistration = bundleContext.registerService(
			LISTENER_CLASSES, new ListenerWrapper<>(initialListener), properties);
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		if (_listenerRegistration != null && !destroyed.get()) {
			try {
				_listenerRegistration.unregister();
			}
			catch (IllegalStateException ise) {
				// the service was already unregistered.
			}
		}
	}

	private static final String[] LISTENER_CLASSES = new String[] {
			ServletContextListener.class.getName(),
			ServletRequestListener.class.getName(),
			HttpSessionListener.class.getName()
	};

	private volatile ServiceRegistration<?> _listenerRegistration;
	private final AtomicBoolean destroyed = new AtomicBoolean(false);

	public static class Ready {}

	private class ListenerWrapper<T extends HttpSessionListener & ServletContextListener & ServletRequestListener>
		implements HttpSessionListener, ServletContextListener, ServletRequestListener {

		private final T delegate;

		public ListenerWrapper(T delegate) {
			this.delegate = delegate;
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			try {
				delegate.contextDestroyed(sce);
			}
			finally {
				destroyed.set(true);
			}
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			delegate.contextInitialized(sce);
		}

		@Override
		public void requestDestroyed(ServletRequestEvent sre) {
			delegate.requestDestroyed(sre);
		}

		@Override
		public void requestInitialized(ServletRequestEvent sre) {
			delegate.requestInitialized(sre);
		}

		@Override
		public void sessionCreated(HttpSessionEvent se) {
			delegate.sessionCreated(se);
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent se) {
			delegate.sessionDestroyed(se);
		}

	}

}
