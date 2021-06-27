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

package org.apache.aries.cdi.weld;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class BundleResourcesLoader implements ProxyServices, ResourceLoader {

	private static java.lang.reflect.Method defineClass1, defineClass2;
	private static final AtomicBoolean classLoaderMethodsMadeAccessible = new AtomicBoolean(false);

	public static void makeClassLoaderMethodsAccessible() {
		// the AtomicBoolean make sure this gets invoked only once as WeldStartup is triggered per deployment
		if (classLoaderMethodsMadeAccessible.compareAndSet(false, true)) {
			try {
				AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws Exception {
						Class<?> cl = Class.forName("java.lang.ClassLoader");
						final String name = "defineClass";

						defineClass1 = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class);
						defineClass2 = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
						defineClass1.setAccessible(true);
						defineClass2.setAccessible(true);
						return null;
					}
				});
			} catch (PrivilegedActionException pae) {
				throw new RuntimeException("cannot initialize ClassPool", pae.getException());
			}
		}
	}

	static {
		makeClassLoaderMethodsAccessible();
	}

	BundleResourcesLoader(SpiLoader loader, Bundle spiImplBundle) {
		BundleWiring spiImplWiring = spiImplBundle.adapt(BundleWiring.class);

		List<Bundle> bundles = new ArrayList<>();

		bundles.add(spiImplBundle);

		List<BundleWire> requiredWires = spiImplWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);

		for (BundleWire bundleWire : requiredWires) {
			BundleCapability capability = bundleWire.getCapability();
			Map<String, Object> attributes = capability.getAttributes();
			String packageName = (String)attributes.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!packageName.startsWith("org.jboss.weld.")) {
				continue;
			}

			Bundle wireBundle = bundleWire.getProvider().getBundle();
			if (!bundles.contains(wireBundle)) {
				bundles.add(wireBundle);
			}
		}

		loader.getBundles().addAll(bundles);

		_classLoader = loader;
	}


	@Override
	public void cleanup() {
	}

	@Override
	public Class<?> classForName(String className) {
		try {
			return _classLoader.loadClass(className);
		}
		catch (ClassNotFoundException e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
		catch (LinkageError e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
		catch (TypeNotPresentException e) {
			throw new ResourceLoadingException(ERROR_LOADING_CLASS + className, e);
		}
	}

	@Override
	public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len) throws ClassFormatError {
		return defineClass(originalClass, className, classBytes, off, len, null);
	}

	@Override
	public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len, ProtectionDomain protectionDomain) throws ClassFormatError {
		try {
			java.lang.reflect.Method method;
			Object[] args;
			if (protectionDomain == null) {
				method = defineClass1;
				args = new Object[]{className, classBytes, 0, len};
			} else {
				method = defineClass2;
				args = new Object[]{className, classBytes, 0, len, protectionDomain};
			}
			Class<?> clazz = (Class<?>) method.invoke(_classLoader, args);
			return clazz;
		} catch (RuntimeException e) {
			throw e;
		} catch (java.lang.reflect.InvocationTargetException e) {
			throw new RuntimeException(e.getTargetException());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClassLoader getClassLoader(Class<?> proxiedBeanType) {
		return _classLoader;
	}

	@Override
	public Class<?> loadBeanClass(String className) {
		return classForName(className);
	}

	@Override
	public URL getResource(String name) {
		return _classLoader.getResource(name);
	}

	@Override
	public Collection<URL> getResources(String name) {
		try {
			return Collections.list(_classLoader.getResources(name));
		}
		catch (IOException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public Class<?> loadClass(Class<?> originalClass, String className) throws ClassNotFoundException {
		return _classLoader.loadClass(className);
	}

	@Override
	public boolean supportsClassDefining() {
		return true;
	}

	private static final String ERROR_LOADING_CLASS = "Error loading class ";

	private final ClassLoader _classLoader;

}