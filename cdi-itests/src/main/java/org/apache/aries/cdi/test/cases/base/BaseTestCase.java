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

package org.apache.aries.cdi.test.cases.base;

import static java.util.Optional.ofNullable;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.osgi.test.common.filter.Filters.format;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.test.junit4.context.BundleContextRule;
import org.osgi.test.junit4.service.ServiceUseRule;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class BaseTestCase {

	public static final long timeout = 500;
	public Bundle cdiBundle;
	public Bundle servicesBundle;
	public static final PromiseFactory promiseFactory = new PromiseFactory(null);

	@Rule
	public BundleContextRule bcr = new BundleContextRule();
	@Rule
	public ServiceUseRule<CDIComponentRuntime> ccrr = new ServiceUseRule.Builder<CDIComponentRuntime>(CDIComponentRuntime.class, bcr).build();
	@Rule
	public ServiceUseRule<ConfigurationAdmin> car = new ServiceUseRule.Builder<ConfigurationAdmin>(ConfigurationAdmin.class, bcr).build();

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

	@Before
	public void setUp() throws Exception {
		servicesBundle = bcr.installBundle("services-one.jar", false);
		servicesBundle.start();
		cdiBundle = bcr.installBundle("basic-beans.jar", false);
		cdiBundle.start();
	}

	@After
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
		servicesBundle.uninstall();
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
			iterator = ccrr.getService().getContainerDTOs(bundle).iterator();
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

	public <S,T> CloseableTracker<S, T> track(Filter filter) {
		CloseableTracker<S, T> tracker = new CloseableTracker<>(bcr.getBundleContext(), filter);
		tracker.open();
		return tracker;
	}

	public <S,T> CloseableTracker<S, T> track(String pattern, Object... objects) {
		return track(format(pattern, objects));
	}

	public <S> CloseableTracker<S, ServiceReference<S>> trackSR(String pattern, Object... objects) {
		return trackSR(format(pattern, objects));
	}

	public <S> CloseableTracker<S, ServiceReference<S>> trackSR(Filter filter) {
		CloseableTracker<S, ServiceReference<S>> tracker = new CloseableTracker<>(bcr.getBundleContext(), filter, new ServiceTrackerCustomizer<S, ServiceReference<S>>() {

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
			format(
				"(&(objectClass=%s)(service.bundleid=%d))",
				BeanManager.class.getName(),
				bundle.getBundleId()),
			null);
		serviceTracker.open();
		return serviceTracker;
	}

	public long getChangeCount(ServiceReference<?> reference) {
		return ofNullable(
			reference.getProperty("service.changecount")
		).map(
			Long.class::cast
		).orElseGet(
			() -> new Long(-1l)
		).longValue();
	}

	public static <T> T tccl(ClassLoader classLoader, Supplier<T> supplier) {
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

}
