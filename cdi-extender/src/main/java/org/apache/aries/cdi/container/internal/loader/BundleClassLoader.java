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

package org.apache.aries.cdi.container.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.webbeans.config.WebBeansContext;
import org.osgi.framework.Bundle;

public class BundleClassLoader extends ClassLoader {

	public BundleClassLoader(Bundle[] bundles) {
		if (bundles.length == 0) {
			throw new IllegalArgumentException(
				"At least one bundle is required");
		}

		_bundles = bundles;
	}

	@Override
	public URL findResource(String name) {
		for (Bundle bundle : _bundles) {
			URL url = bundle.getResource(name);

			if (url != null) {
				return url;
			}
		}

		return null;
	}

	@Override
	public Enumeration<URL> findResources(String name) {
		for (Bundle bundle : _bundles) {
			try {
				Enumeration<URL> enumeration = bundle.getResources(name);

				if ((enumeration != null) && enumeration.hasMoreElements()) {
					return enumeration;
				}
			}
			catch (IOException ioe) {
			}
		}

		if (name != null && name.startsWith("META-INF/openwebbeans/")) { // fallback
			try {
				return WebBeansContext.class.getClassLoader().getResources(name);
			}
			catch (IOException e) {
			}
		}
		return Collections.emptyEnumeration();
	}

	public Bundle[] getBundles() {
		return _bundles;
	}

	@Override
	public URL getResource(String name) {
		return findResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) {
		return findResources(name);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Object classLoadingLock = getClassLoadingLock(name);

		synchronized (classLoadingLock) {
			for (Bundle bundle : _bundles) {
				try {
					return bundle.loadClass(name);
				}
				catch (ClassNotFoundException cnfe) {
					continue;
				}
			}
			if (name != null) {
				if (name.startsWith("org.apache.webbeans.") || name.startsWith("sun.misc.")) {
					return WebBeansContext.class.getClassLoader().loadClass(name);
				}
			}

			throw new ClassNotFoundException(name);
		}
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
		throws ClassNotFoundException {

		Object classLoadingLock = getClassLoadingLock(name);

		synchronized (classLoadingLock) {
			Class<?> clazz = _cache.get(name);

			if (clazz == null) {
				clazz = findClass(name);

				if (resolve) {
					resolveClass(clazz);
				}

				_cache.put(name, clazz);
			}

			return clazz;
		}
	}

	private final Bundle[] _bundles;
	private final ConcurrentMap<String, Class<?>> _cache = new ConcurrentHashMap<>();

}