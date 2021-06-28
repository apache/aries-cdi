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
package org.apache.aries.cdi.owb.core;

import java.lang.reflect.Modifier;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.proxy.Unsafe;
import org.apache.webbeans.spi.DefiningClassService;

public class OSGiDefiningClassService implements DefiningClassService {
	private final ClassLoaders classloaders;

	public OSGiDefiningClassService(final WebBeansContext context) {
		this.classloaders = context.getService(ClassLoaders.class);
	}

	@Override
	public ClassLoader getProxyClassLoader(final Class<?> aClass) {
		return classloaders.loader;
	}

	/**
	 * We prefer to register the proxy in the dedicated cdi classloader but due to classloader rules it is not always possible.
	 * In such cases we try to use unsafe.
	 *
	 * @param name proxy name.
	 * @param bytes proxy bytecode.
	 * @param proxied proxied class.
	 * @param <T> proxied type.
	 * @return the proxy class instance.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<T> defineAndLoad(final String name, final byte[] bytes, final Class<T> proxied) {
		if (requiresUnsafe(proxied)) { // todo: today we don't really support that
			final ClassLoader classLoader = proxied.getClassLoader();
			if (classLoader != classloaders.bundleLoader) {
				// todo: log a warning?
			}
			return UnsafeFacade.INSTANCE.defineAndLoadClass(classLoader, name, bytes, proxied);
		}
		return (Class<T>) classloaders.loader.getOrRegister(name, bytes, proxied.getPackage(), proxied.getProtectionDomain());
	}

	private boolean requiresUnsafe(final Class<?> aClass) {
		return !Modifier.isPublic(aClass.getModifiers());
	}

	public static class ClassLoaders {
		private final ClassLoader bundleLoader;
		private final SpiLoader loader;

		public ClassLoaders(final ClassLoader bundleLoader, final SpiLoader loader) {
			this.bundleLoader = bundleLoader;
			this.loader = loader;
		}
	}

	// lazy init unsafe, not needed for a lot of apps and avoids warnings on java > 8
	private static class UnsafeFacade {
		private static final Unsafe INSTANCE = new Unsafe();
	}
}
