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

package org.apache.aries.cdi.extension.mp.jwt;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

public class JwtAuthFilter implements ContainerRequestFilter, Filter {

	private final Filter delegate;
	private final ClassLoader loader;

	public JwtAuthFilter(Filter delegate, ClassLoader loader) {
		this.delegate = delegate;
		this.loader = loader;
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(loader);
			delegate.init(arg0);
		}
		finally {
			currentThread.setContextClassLoader(current);
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
			throws IOException, ServletException {

		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(loader);
			delegate.doFilter(arg0, arg1, arg2);
		}
		finally {
			currentThread.setContextClassLoader(current);
		}
	}

	@Override
	public void destroy() {
		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();

		try {
			currentThread.setContextClassLoader(loader);
			delegate.destroy();
		}
		finally {
			currentThread.setContextClassLoader(current);
		}
	}

}
