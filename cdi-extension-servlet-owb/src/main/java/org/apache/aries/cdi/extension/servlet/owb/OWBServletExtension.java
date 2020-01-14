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

package org.apache.aries.cdi.extension.servlet.owb;

import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static javax.interceptor.Interceptor.Priority.LIBRARY_AFTER;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extension.servlet.common.BaseServletExtension;
import org.apache.aries.cdi.owb.spi.StartObjectSupplier;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

@SuppressWarnings("serial")
public class OWBServletExtension extends BaseServletExtension implements StartObjectSupplier {

	private final BundleContext bundleContext;
	private final ServletContext proxyContext;
	private final ServletContextEvent startEvent;
	private volatile ServletContext delegateContext;

	protected OWBServletExtension() { // proxy
		bundleContext = null;
		proxyContext = null;
		startEvent = null;
	}

	public OWBServletExtension(Bundle bundle) {
		this.startEvent = new ServletContextEvent(new MockServletContext()) {
			@Override
			public ServletContext getServletContext() {
				return proxyContext;
			}
		};

		this.bundleContext = bundle.getBundleContext();
		this.delegateContext = new ForwardingContext();

		// ensure we can switch the impl and keep ServletContextBean working with an updated context
		this.proxyContext = ServletContext.class.cast(Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
				new Class<?>[]{ServletContext.class},
				(proxy, method, args) -> {
					try {
						final ServletContext ctx = ofNullable(delegateContext).orElseGet(startEvent::getServletContext);
						return method.invoke(ctx, args);
					}
					catch (final InvocationTargetException ite) {
						throw ite.getTargetException();
					}
				}));
	}

	public void setDelegate(final ServletContext delegateContext) {
		final ServletContext oldCtx = this.delegateContext;
		this.delegateContext = delegateContext;
		list(oldCtx.getAttributeNames()).forEach(attr -> this.delegateContext.setAttribute(attr, oldCtx.getAttribute(attr)));
	}

	void afterDeploymentValidation(
		@Observes @Priority(LIBRARY_AFTER + 800)
		AfterDeploymentValidation adv, BeanManager beanManager) {

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_DESCRIPTION, "Aries CDI - HTTP Portable Extension for OpenWebBeans");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(HTTP_WHITEBOARD_CONTEXT_SELECT, configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT));
		properties.put(HTTP_WHITEBOARD_LISTENER, Boolean.TRUE.toString());
		properties.put(SERVICE_RANKING, Integer.MAX_VALUE - 100);

		_listenerRegistration = bundleContext.registerService(
			LISTENER_CLASSES, new CdiListener(WebBeansContext.currentInstance()), properties);
	}

	private static final String[] LISTENER_CLASSES = new String[]{
		ServletContextListener.class.getName(),
		ServletRequestListener.class.getName(),
		HttpSessionListener.class.getName()
	};

	@Override
	public Object getStartObject() {
		return startEvent;
	}

	private class CdiListener extends org.apache.webbeans.servlet.WebBeansConfigurationListener {
		private final WebBeansContext webBeansContext;

		private CdiListener(final WebBeansContext webBeansContext) {
			this.webBeansContext = webBeansContext;
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			ServletContext realSC = event.getServletContext();

			// update the sce to have the real one in CDI
			setDelegate(realSC);

			// propagate attributes from the temporary sc
			list(startEvent.getServletContext().getAttributeNames()).forEach(
				attr -> realSC.setAttribute(attr, startEvent.getServletContext().getAttribute(attr)));

			realSC.setAttribute(BundleContext.class.getName(), bundleContext);
			realSC.setAttribute(WebBeansContext.class.getName(), webBeansContext);

			// already started in the activator so let's skip it, just ensure it is skipped if re-called
			event.getServletContext().setAttribute(getClass().getName(), true);
			if (lifeCycle == null) {
				lifeCycle = webBeansContext.getService(ContainerLifecycle.class);
			}
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			try {
				super.contextDestroyed(sce);
			}
			finally {
				destroyed.set(true);
			}
		}
	}

	private static class ForwardingContext extends MockServletContext {
		@Override
		public String getVirtualServerName() {
			return "http";
		}
	}
}

