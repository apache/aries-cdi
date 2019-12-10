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

package org.apache.aries.cdi.extension.servlet.weld;

import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER;

import java.util.Dictionary;
import java.util.Hashtable;
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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extension.servlet.common.WebFilterProcessor;
import org.apache.aries.cdi.extension.servlet.common.WebListenerProcessor;
import org.apache.aries.cdi.extension.servlet.common.WebServletProcessor;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class WeldServletExtension implements Extension {

	private final BundleContext bundleContext;
	volatile Configuration configuration;

	public WeldServletExtension(Bundle bundle) {
		this.bundleContext = bundle.getBundleContext();
	}

	void setConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	<X> void webFilter(@Observes @WithAnnotations(WebFilter.class) ProcessAnnotatedType<X> pat) {
		new WebFilterProcessor().process(configuration, pat);
	}

	<X> void webListener(@Observes @WithAnnotations(WebListener.class) ProcessAnnotatedType<X> pat) {
		new WebListenerProcessor().process(configuration, pat);
	}

	<X> void webServlet(@Observes @WithAnnotations(WebServlet.class) ProcessAnnotatedType<X> pat) {
		new WebServletProcessor().process(configuration, pat);
	}

	void afterDeploymentValidation(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		beanManager.getEvent().fireAsync(new Ready());
	}

	void ready(
		@ObservesAsync Ready ready, BeanManager beanManager) {

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
