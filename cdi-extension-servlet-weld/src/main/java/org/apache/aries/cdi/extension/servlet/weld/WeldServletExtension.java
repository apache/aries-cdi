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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extension.servlet.common.BaseServletExtension;
import org.jboss.weld.module.web.servlet.WeldInitialListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class WeldServletExtension extends BaseServletExtension {

	private final BundleContext bundleContext;

	protected WeldServletExtension() { // proxy
		bundleContext = null;
	}

	public WeldServletExtension(Bundle bundle) {
		this.bundleContext = bundle.getBundleContext();
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

	private static final String[] LISTENER_CLASSES = new String[] {
		ServletContextListener.class.getName(),
		ServletRequestListener.class.getName(),
		HttpSessionListener.class.getName()
	};

	public static class Ready {}

	private class ListenerWrapper<T extends HttpSessionListener & ServletContextListener & ServletRequestListener>
		implements HttpSessionListener, ServletContextListener, ServletRequestListener {

		private final T delegate;
		private final CountDownLatch latch = new CountDownLatch(1);

		public ListenerWrapper(T delegate) {
			this.delegate = delegate;
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			try {
				latch.await(20, TimeUnit.SECONDS);
				delegate.contextDestroyed(sce);
			} catch (InterruptedException e) {
			}
			finally {
				destroyed.set(true);
			}
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			delegate.contextInitialized(sce);
			latch.countDown();
		}

		@Override
		public void requestDestroyed(ServletRequestEvent sre) {
			try {
				latch.await(20, TimeUnit.SECONDS);
				delegate.requestDestroyed(sre);
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void requestInitialized(ServletRequestEvent sre) {
			try {
				latch.await(20, TimeUnit.SECONDS);
				delegate.requestInitialized(sre);
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void sessionCreated(HttpSessionEvent se) {
			try {
				latch.await(20, TimeUnit.SECONDS);
				delegate.sessionCreated(se);
			} catch (InterruptedException e) {
			}
		}

		@Override
		public void sessionDestroyed(HttpSessionEvent se) {
			try {
				latch.await(20, TimeUnit.SECONDS);
				delegate.sessionDestroyed(se);
			} catch (InterruptedException e) {
			}
		}

	}

}
