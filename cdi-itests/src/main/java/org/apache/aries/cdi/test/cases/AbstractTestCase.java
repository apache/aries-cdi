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

package org.apache.aries.cdi.test.cases;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.aries.cdi.extra.RequireCDIExtension;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.annotation.bundle.Requirement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.configurator.annotations.RequireConfigurator;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Requirement(
	effective = "active",
	filter = "(objectClass=org.osgi.service.cm.ConfigurationAdmin)",
	namespace = ServiceNamespace.SERVICE_NAMESPACE
)
@RequireCDIExtension("aries.cdi.http")
@RequireCDIExtension("aries.cdi.jndi")
@RequireCDIExtension("eclipse.microprofile.config")
@RequireCDIExtension("eclipse.microprofile.jwt-auth")
@RequireCDIExtension("eclipse.microprofile.metrics")
@RequireConfigurator
public abstract class AbstractTestCase {

	@Rule
	public TestWatcher watchman= new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			System.out.printf("--------- TEST: %s#%s [%s]%n", description.getTestClass(), description.getMethodName(), "FAILED");
		}

		@Override
		protected void succeeded(Description description) {
			System.out.printf("--------- TEST: %s#%s [%s]%n", description.getTestClass(), description.getMethodName(), "PASSED");
		}
	};

	@BeforeClass
	public static void beforeClass() throws Exception {
		runtimeTracker = new ServiceTracker<>(
				bundleContext, CDIComponentRuntime.class, null);
		runtimeTracker.open();
		servicesBundle = installBundle("services-one.jar", false);
		servicesBundle.start();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		runtimeTracker.close();
		servicesBundle.uninstall();
	}

	@Before
	public void setUp() throws Exception {
		cdiRuntime = runtimeTracker.waitForService(timeout);
		cdiBundle = installBundle("basic-beans.jar", false);
		cdiBundle.start();
	}

	@After
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
	}

	public void assertBeanExists(Class<?> clazz, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(clazz, Any.Literal.INSTANCE);

		assertFalse(beans.isEmpty());
		Iterator<Bean<?>> iterator = beans.iterator();
		Bean<?> bean = iterator.next();
		assertTrue(clazz.isAssignableFrom(bean.getBeanClass()));
		assertFalse(iterator.hasNext());

		bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		Object pojo = clazz.cast(beanManager.getReference(bean, clazz, ctx));
		assertNotNull(pojo);
	}

	public static InputStream getBundle(String name) {
		ClassLoader classLoader = AbstractTestCase.class.getClassLoader();

		return classLoader.getResourceAsStream(name);
	}

	public Bundle getCdiExtenderBundle() {
		BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);

		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE);

		for (BundleWire wire : requiredWires) {
			Map<String, Object> attributes = wire.getCapability().getAttributes();
			String extender = (String)attributes.get(ExtenderNamespace.EXTENDER_NAMESPACE);

			if (CDIConstants.CDI_CAPABILITY_NAME.equals(extender)) {
				return wire.getProvider().getBundle();
			}
		}

		return null;
	}

	public ContainerDTO getContainerDTO(CDIComponentRuntime runtime, Bundle bundle) {
		Iterator<ContainerDTO> iterator;
		ContainerDTO containerDTO = null;
		int attempts = 50;
		while (--attempts > 0) {
			iterator = cdiRuntime.getContainerDTOs(bundle).iterator();
			if (iterator.hasNext()) {
				containerDTO = iterator.next();
				if (containerDTO != null) {
					break;
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertNotNull(containerDTO);
		return containerDTO;
	}

	public static Bundle installBundle(String url) throws Exception {
		return installBundle(url, true);
	}

	public static Bundle installBundle(String bundleName, boolean start) throws Exception {
		Bundle b = bundleContext.installBundle(bundleName, getBundle(bundleName));

		if (start) {
			b.start();
		}

		return b;
	}

	public Filter filter(String pattern, Object... objects) {
		try {
			return FrameworkUtil.createFilter(String.format(pattern, objects));
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public Dictionary<String, Object> getProperties(ServiceReference<Integer> reference) {
		Dictionary<String, Object> properties = new Hashtable<>();
		for (String key : reference.getPropertyKeys()) {
			properties.put(key, reference.getProperty(key));
		}
		return properties;
	}

	public <S,T> CloseableTracker<S, T> track(Filter filter) {
		CloseableTracker<S, T> tracker = new CloseableTracker<>(bundleContext, filter);
		tracker.open();
		return tracker;
	}

	public <S,T> CloseableTracker<S, T> track(String pattern, Object... objects) {
		return track(filter(pattern, objects));
	}

	public <S> CloseableTracker<S, ServiceReference<S>> trackSR(String pattern, Object... objects) {
		return trackSR(filter(pattern, objects));
	}

	public <S> CloseableTracker<S, ServiceReference<S>> trackSR(Filter filter) {
		CloseableTracker<S, ServiceReference<S>> tracker = new CloseableTracker<>(bundleContext, filter, new ServiceTrackerCustomizer<S, ServiceReference<S>>() {

			@Override
			public ServiceReference<S> addingService(ServiceReference<S> reference) {
				return reference;
			}

			@Override
			public void modifiedService(ServiceReference<S> reference, ServiceReference<S> service) {
			}

			@Override
			public void removedService(ServiceReference<S> reference, ServiceReference<S> service) {
			}

		});
		tracker.open();
		return tracker;
	}

	public BeanManager getBeanManager(Bundle bundle) throws Exception {
		return trackBM(bundle).waitForService(timeout);
	}

	public CloseableTracker<BeanManager, BeanManager> trackBM(Bundle bundle) throws Exception {
		CloseableTracker<BeanManager, BeanManager> serviceTracker = new CloseableTracker<>(
			bundle.getBundleContext(),
			filter(
				"(&(objectClass=%s)(service.bundleid=%d))",
				BeanManager.class.getName(),
				bundle.getBundleId()),
			null);
		serviceTracker.open();
		return serviceTracker;
	}

	public long getChangeCount(ServiceReference<?> reference) {
		return Optional.ofNullable(
			reference.getProperty("service.changecount")
		).map(
			v -> (Long)v
		).orElse(
			new Long(-1l)
		).longValue();
	}

	public <T> T with(ClassLoader classLoader, Supplier<T> supplier) {
		Thread currentThread = Thread.currentThread();
		ClassLoader original = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(classLoader);
			return supplier.get();
		}
		finally {
			currentThread.setContextClassLoader(original);
		}
	}

	public static final Bundle bundle = FrameworkUtil.getBundle(CdiBeanTests.class);
	public static final BundleContext bundleContext = bundle.getBundleContext();
	public static final long timeout = 500;
	public static Bundle servicesBundle;
	public static ServiceTracker<CDIComponentRuntime, CDIComponentRuntime> runtimeTracker;

	public Bundle cdiBundle;
	public CDIComponentRuntime cdiRuntime;
	public final PromiseFactory promiseFactory = new PromiseFactory(null);

}
