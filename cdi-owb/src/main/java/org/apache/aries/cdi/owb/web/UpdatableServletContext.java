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
package org.apache.aries.cdi.owb.web;

import static java.util.Optional.ofNullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.osgi.framework.BundleContext;

@SuppressWarnings("serial")
public class UpdatableServletContext extends ServletContextEvent {
	private final WebBeansContext bootstrap;
	private final BundleContext bundleContext;
	private final ServletContext context;
	private ServletContext delegate;

	public UpdatableServletContext(WebBeansContext bootstrap, BundleContext bundleContext) {
		super(new MockServletContext());
		this.bootstrap = bootstrap;
		this.bundleContext = bundleContext;

		// ensure we can switch the impl and keep ServletContextBean working with an updated context
		this.context = ServletContext.class.cast(Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
				new Class<?>[]{ServletContext.class},
				(proxy, method, args) -> {
					try {
						return method.invoke(ofNullable(delegate).orElseGet(UpdatableServletContext.super::getServletContext), args);
					}
					catch (final InvocationTargetException ite) {
						throw ite.getTargetException();
					}
				}));
	}

	public void setDelegate(final ServletContext delegate) {
		this.delegate = delegate;
		this.delegate.setAttribute(BundleContext.class.getName(), bundleContext);
		this.delegate.setAttribute(WebBeansContext.class.getName(), bootstrap);
	}

	public ServletContext getOriginal() {
		return super.getServletContext();
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}
}
