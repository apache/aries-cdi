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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.osgi.framework.Bundle;

public class BundleClassLoader extends SpiLoader {

	public BundleClassLoader(List<Bundle> bundles) {
		if (bundles.isEmpty()) {
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
			if ((bundle.getState() & Bundle.UNINSTALLED) == Bundle.UNINSTALLED) {
				continue;
			}
			try {
				Enumeration<URL> enumeration = bundle.getResources(name);

				if ((enumeration != null) && enumeration.hasMoreElements()) {
					return enumeration;
				}
			}
			catch (IOException ioe) {
			}

			if (resourcePredicate != null && resourcePredicate.test(name)) {
				return resourceFunction.apply(name);
			}
		}

		return Collections.emptyEnumeration();
	}

	@Override
	public List<Bundle> getBundles() {
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
	public BundleClassLoader handleResources(
		Predicate<String> predicate, Function<String, Enumeration<URL>> function) {

		resourcePredicate = requireNonNull(predicate);
		resourceFunction = requireNonNull(function);

		return this;
	}

	@Override
	public BundleClassLoader findClass(
		Predicate<String> predicate, Function<String, Class<?>> function) {

		classPredicate = requireNonNull(predicate);
		classFunction = requireNonNull(function);

		return this;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Object classLoadingLock = getClassLoadingLock(name);

		synchronized (classLoadingLock) {
			for (Bundle bundle : _bundles) {
				if ((bundle.getState() & Bundle.UNINSTALLED) == Bundle.UNINSTALLED) {
					continue;
				}
				try {
					return bundle.loadClass(name);
				}
				catch (ClassNotFoundException cnfe) {
					continue;
				}
			}

			if (classPredicate != null && classPredicate.test(name)) {
				return classFunction.apply(name);
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

	@Override
	public Class<?> getOrRegister(final String proxyClassName, final byte[] proxyBytes,
								final Package pck, final ProtectionDomain protectionDomain) {
		final String key = proxyClassName.replace('/', '.');
		Class<?> existing = _cache.get(key);
		if (existing == null) {
			Object classLoadingLock = getClassLoadingLock(key);
			synchronized (classLoadingLock) {
				existing = _cache.get(key);
				if (existing == null) {
					definePackageFor(pck, protectionDomain);
					existing = super.defineClass(proxyClassName, proxyBytes, 0, proxyBytes.length);
					resolveClass(existing);
					_cache.put(key, existing);
				}
			}
		}
		return existing;
	}

	private void definePackageFor(final Package model, final ProtectionDomain protectionDomain) {
		if (model == null) {
			return;
		}
		if (getPackage(model.getName()) == null) {
			if (model.isSealed() && protectionDomain != null &&
					protectionDomain.getCodeSource() != null &&
					protectionDomain.getCodeSource().getLocation() != null) {
				definePackage(
						model.getName(),
						model.getSpecificationTitle(),
						model.getSpecificationVersion(),
						model.getSpecificationVendor(),
						model.getImplementationTitle(),
						model.getImplementationVersion(),
						model.getImplementationVendor(),
						protectionDomain.getCodeSource().getLocation());
			} else {
				definePackage(
						model.getName(),
						model.getSpecificationTitle(),
						model.getSpecificationVersion(),
						model.getSpecificationVendor(),
						model.getImplementationTitle(),
						model.getImplementationVersion(),
						model.getImplementationVendor(),
						null);
			}
		}
	}

	private final List<Bundle> _bundles;
	private final ConcurrentMap<String, Class<?>> _cache = new ConcurrentHashMap<>();
	private volatile Predicate<String> classPredicate;
	private volatile Function<String, Class<?>> classFunction;
	private volatile Function<String, Enumeration<URL>> resourceFunction;
	private volatile Predicate<String> resourcePredicate;

}