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

package org.apache.aries.cdi.extension.mp.metrics.test;

import static java.util.Optional.ofNullable;
import static org.osgi.test.common.filter.Filters.format;

import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectInstallBundle;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.install.InstallBundle;
import org.osgi.test.junit4.context.BundleContextRule;
import org.osgi.test.junit4.service.ServiceRule;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class BaseTestCase {

	public static final long timeout = 5000;

	@Rule
	public BundleContextRule bcr = new BundleContextRule();
	@Rule
	public ServiceRule sr = new ServiceRule();

	@InjectBundleContext
	BundleContext bundleContext;
	@InjectInstallBundle
	InstallBundle installBundle;
	@InjectService
	CDIComponentRuntime ccrr;

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

	public <S,T> CloseableTracker<S, T> track(Class<S> typeToTrack) {
		CloseableTracker<S, T> tracker = new CloseableTracker<>(bundleContext, typeToTrack);
		tracker.open();
		return tracker;
	}

	public <S,T> CloseableTracker<S, T> track(Class<S> typeToTrack, String pattern, Object... objects) {
		CloseableTracker<S, T> tracker = new CloseableTracker<>(bundleContext, format("(&(objectClass=%s)%s)", typeToTrack.getName(), format(pattern, objects)));
		tracker.open();
		return tracker;
	}

	public <S,T> CloseableTracker<S, T> track(Filter filter) {
		CloseableTracker<S, T> tracker = new CloseableTracker<>(bundleContext, filter);
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

	public static class CloseableTracker<S, T> extends ServiceTracker<S, T> implements AutoCloseable {

		public CloseableTracker(BundleContext context, Class<S> typeToTrack) {
			super(context, typeToTrack, null);
		}

		public CloseableTracker(BundleContext context, Filter filter) {
			super(context, filter, null);
		}

		public CloseableTracker(BundleContext context, Filter filter, ServiceTrackerCustomizer<S, T> customizer) {
			super(context, filter, customizer);
		}

		@Override
		public void close() {
			super.close();
		}

	}

}
