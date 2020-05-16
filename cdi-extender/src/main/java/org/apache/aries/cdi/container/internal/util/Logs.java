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

package org.apache.aries.cdi.container.internal.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

public class Logs {

	public static class Builder {

		public Builder(BundleContext bundleContext) {
			_bundleContext = bundleContext;
		}

		public Logs build() {
			return new Logs(_bundleContext);
		}

		private final BundleContext _bundleContext;

	}

	private Logs(BundleContext bundleContext) {
		LoggerFactory loggerFactory = null;

		if ((bundleContext != null) && Perms.hasLoggerFactoryServicePermission(bundleContext)) {
			ServiceTracker<LoggerFactory, LoggerFactory> tracker = new ServiceTracker<>(bundleContext, LoggerFactory.class, null);

			tracker.open();

			loggerFactory = tracker.getService();
		}

		_loggerFactory = loggerFactory;
	}

	public Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	public Logger getLogger(String name) {
		if (_loggerFactory != null) {
			return _loggerFactory.getLogger(name);
		}

		return getLoggerProxy(new SysoutLogger(name));
	}

	public static Logger getLoggerProxy(InvocationHandler invocationHandler) {
		return (Logger)Proxy.newProxyInstance(
			Logs.class.getClassLoader(), new Class<?>[] {Logger.class}, invocationHandler);
	}

	public LoggerFactory getLoggerFactory() {
		return _loggerFactory;
	}

	private final LoggerFactory _loggerFactory;

	public static class SysoutLogger implements InvocationHandler {

		static final Map<Method, Method> methodMap = new ConcurrentHashMap<>();

		static {
			Method[] handlerMethods = SysoutLogger.class.getDeclaredMethods();

			for (Method handlerMethod : handlerMethods) {
				try {
					Method method = Logger.class.getMethod(handlerMethod.getName(), handlerMethod.getParameterTypes());
					methodMap.put(method, handlerMethod);
				} catch (NoSuchMethodException | SecurityException e) {
					// nothing to do
				}
			}
		}

		private final String name;

		public SysoutLogger(String name) {
			this.name = name;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Method handlerMethod = methodMap.get(method);

			if (handlerMethod != null) {
				try {
					if (handlerMethod != null) {
						return handlerMethod.invoke(this, args);
					}
				} catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
			return method.invoke(this, args);
		}

		public void audit(String message) {
		}

		public void audit(String format, Object arg) {
		}

		public void audit(String format, Object arg1, Object arg2) {
		}

		public void audit(String format, Object... arguments) {
		}

		public void debug(String message) {
		}

		public void debug(String format, Object arg) {
		}

		public void debug(String format, Object arg1, Object arg2) {
		}

		public void debug(String format, Object... arguments) {
		}

		public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
		}

		public void error(String message) {
		}

		public void error(String format, Object arg) {
		}

		public void error(String format, Object arg1, Object arg2) {
		}

		public void error(String format, Object... arguments) {
		}

		public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
		}

		public String getName() {
			return name;
		}

		public void info(String message) {
		}

		public void info(String format, Object arg) {
		}

		public void info(String format, Object arg1, Object arg2) {
		}

		public void info(String format, Object... arguments) {
		}

		public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
		}

		public boolean isDebugEnabled() {
			return false;
		}

		public boolean isErrorEnabled() {
			return false;
		}

		public boolean isInfoEnabled() {
			return false;
		}

		public boolean isTraceEnabled() {
			return false;
		}

		public boolean isWarnEnabled() {
			return false;
		}

		public void trace(String message) {
		}

		public void trace(String format, Object arg) {
		}

		public void trace(String format, Object arg1, Object arg2) {
		}

		public void trace(String format, Object... arguments) {
		}

		public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
		}

		public void warn(String message) {
		}

		public void warn(String format, Object arg) {
		}

		public void warn(String format, Object arg1, Object arg2) {
		}

		public void warn(String format, Object... arguments) {
		}

		public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
		}

	}

}