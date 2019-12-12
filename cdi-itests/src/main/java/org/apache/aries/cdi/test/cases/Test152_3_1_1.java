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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Success;
import org.osgi.util.tracker.ServiceTracker;

public class Test152_3_1_1 extends SlimTestCase {

	@Override
	public void setUp() throws Exception {
		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
		adminTracker.open();
		configurationAdmin = adminTracker.getService();
		assertThat(bundleContext.getBundle().getRegisteredServices()).isNull();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		adminTracker.close();
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void prototypeFactory() throws Exception {
		AtomicReference<Deferred<Object[]>> a = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> b = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> c = new AtomicReference<>(new Deferred<>());

		Consumer<Object[]> onInitialized = (o) -> {try {a.get().resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.get().resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.get().resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1l.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

			try (CloseableTracker<Object, ServiceReference<Object>> tracker = trackSR("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNull();

				// we didn't do a "get" yet so this should fail
				a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				configuration = configurationAdmin.getConfiguration("prototypeFactory", "?");

				// this will trigger the onInitialized because of the tracker
				configuration.update(new Hashtable() {{put("foo", "bar");}});

				// should only work with single configuration instances
				assertThat(tracker.waitForService(50)).isNull();

				configuration.delete();

				configuration = configurationAdmin.getFactoryConfiguration("prototypeFactory", "one", "?");
				configuration.update(new Hashtable() {{put("foo", "bar");}});

				assertThat(tracker.waitForService(50)).isNotNull();

				ServiceObjects<Object> serviceObjects = bundleContext.getServiceObjects(tracker.getService());

				a.set(new Deferred<>());
				b.set(new Deferred<>());
				c.set(new Deferred<>());

				Object service = serviceObjects.getService();
				assertThat(service).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						).contains(
							entry("service.factoryPid", "prototypeFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("prototypeFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				a.set(new Deferred<>());

				Object other = serviceObjects.getService();
				assertThat(other).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(other).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						).contains(
							entry("service.factoryPid", "prototypeFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("prototypeFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				assertThat(service).isNotEqualTo(other);

				serviceObjects.ungetService(service);

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						).contains(
							entry("service.factoryPid", "prototypeFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("prototypeFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				b.set(new Deferred<>());
				c.set(new Deferred<>());

				configuration.delete();

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						).contains(
							entry("service.factoryPid", "prototypeFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("prototypeFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeFactory")
						).contains(
							entry("service.factoryPid", "prototypeFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("prototypeFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void prototypeSingle_C() throws Exception {
		AtomicReference<Deferred<Object[]>> a = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> b = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> c = new AtomicReference<>(new Deferred<>());

		Consumer<Object[]> onInitialized = (o) -> a.get().resolve(o);
		Consumer<Object[]> onBeforeDestroyed = (o) -> b.get().resolve(o);
		Consumer<Object[]> onDestroyed = (o) -> c.get().resolve(o);

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1k.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

			try (CloseableTracker<Object, ServiceReference<Object>> tracker = trackSR("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNull();

				// we didn't do a "get" yet so this should fail
				a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				configuration = configurationAdmin.getConfiguration("prototypeSingle_C", "?");

				configuration.update(new Hashtable() {{put("foo", "bar");}});

				a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				ServiceObjects<Object> serviceObjects = bundleContext.getServiceObjects(tracker.getService());

				Object instance1 = serviceObjects.getService();
				assertThat(instance1).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(instance1).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				reset(a, b, c);

				Object instance2 = serviceObjects.getService();
				assertThat(instance2).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(instance2).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				reset(a, b, c);

				assertThat(instance1).isNotEqualTo(instance2);

				serviceObjects.ungetService(instance1);

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(instance1).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(instance1).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				reset(a, b, c);

				configuration.delete();

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle_C")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void prototypeSingle() throws Exception {
		AtomicReference<Deferred<Object[]>> a = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> b = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> c = new AtomicReference<>(new Deferred<>());

		Consumer<Object[]> onInitialized = (o) -> {try {a.get().resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.get().resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.get().resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1j.jar");

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

			try (CloseableTracker<Object, ServiceReference<Object>> tracker = trackSR("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNotNull();

				ServiceObjects<Object> serviceObjects = bundleContext.getServiceObjects(tracker.getService());

				a.set(new Deferred<>());

				Object service = serviceObjects.getService();
				assertThat(service).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				a.set(new Deferred<>());

				Object other = serviceObjects.getService();
				assertThat(other).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(other).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				b.get().getPromise().timeout(timeout).then(assertFailed).getFailure();
				c.get().getPromise().timeout(timeout).then(assertFailed).getFailure();

				assertThat(service).isNotEqualTo(other);

				serviceObjects.ungetService(service);

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(service).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				b.set(new Deferred<>());
				c.set(new Deferred<>());

				serviceObjects.ungetService(other);

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(other).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat(other).isEqualTo(values[0]);
						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "prototypeSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
		}
		finally {
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void bundleFactory() throws Exception {
		AtomicReference<Deferred<Object[]>> a = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> b = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> c = new AtomicReference<>(new Deferred<>());

		Consumer<Object[]> onInitialized = (o) -> a.get().resolve(o);
		Consumer<Object[]> onBeforeDestroyed = (o) -> b.get().resolve(o);
		Consumer<Object[]> onDestroyed = (o) -> c.get().resolve(o);

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1i.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			assertPromiseIsNotResolved(a);

			configuration = configurationAdmin.getConfiguration("bundleFactory", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			// only accept factory configuration instances
			try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNull();
			}

			assertPromiseIsNotResolved(a);
			assertPromiseIsNotResolved(b);
			assertPromiseIsNotResolved(c);

			configuration.delete();

			configuration = configurationAdmin.getFactoryConfiguration("bundleFactory", "one", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			// Even with configuration, there's still no instance until a "get" is performed
			assertPromiseIsNotResolved(a);

			// This automatically also ungets the service
			try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNotNull();

				a.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "bundleFactory")
						).contains(
							entry("service.factoryPid", "bundleFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("bundleFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				assertPromiseIsNotResolved(b);
				assertPromiseIsNotResolved(c);

				reset(a);

				// this must terminate all bundle instances
				configuration.delete();

				assertPromiseIsNotResolved(a);

				b.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "bundleFactory")
						).contains(
							entry("service.factoryPid", "bundleFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("bundleFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
				c.get().getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "bundleFactory")
						).contains(
							entry("service.factoryPid", "bundleFactory")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("bundleFactory~one"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				reset(a, b, c);
			}

			assertPromiseIsNotResolved(a);
			assertPromiseIsNotResolved(b);
			assertPromiseIsNotResolved(c);
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void bundleSingle_C() throws Exception {
		AtomicReference<Deferred<Object[]>> a = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> b = new AtomicReference<>(new Deferred<>());
		AtomicReference<Deferred<Object[]>> c = new AtomicReference<>(new Deferred<>());

		Consumer<Object[]> onInitialized = (o) -> {
			a.get().resolve(o);
		};
		Consumer<Object[]> onBeforeDestroyed = (o) -> b.get().resolve(o);
		Consumer<Object[]> onDestroyed = (o) -> c.get().resolve(o);

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1h.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			assertPromiseIsNotResolved(a);

			configuration = configurationAdmin.getConfiguration("bundleSingle_C", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			assertPromiseIsNotResolved(a);

			try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNotNull();
			}

			a.get().getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "bundleSingle_C")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("bundleSingle_C"))
					).contains(
						entry("foo", "bar")
					);

					return s;
				},
				f -> fail(f.toString())
			).getValue();

			b.get().getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "bundleSingle_C")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("bundleSingle_C"))
					).contains(
						entry("foo", "bar")
					);

					return s;
				},
				f -> fail(f.toString())
			).getValue();
			c.get().getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "bundleSingle_C")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("bundleSingle_C"))
					).contains(
						entry("foo", "bar")
					);

					return s;
				},
				f -> fail(f.toString())
			).getValue();

			reset(a, b, c);

			configuration.delete();

			assertPromiseIsNotResolved(b);
			assertPromiseIsNotResolved(c);
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	private void assertPromiseIsNotResolved(AtomicReference<Deferred<Object[]>> a) throws Exception {
		Throwable throwable = a.get().getPromise().timeout(timeout).getFailure();

		if (throwable == null) {
			Object[] value = a.get().getPromise().getValue();
			throw new AssertionError("Is resolved! " + value[1]);
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void bundleSingle() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1g.jar");

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
				assertThat(tracker.waitForService(50)).isNotNull();
			}

			a.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "bundleSingle")
					);

					return s;
				},
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			try {
				tbBundle.uninstall();

				try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
					assertThat(tracker.waitForService(50)).isNull();
				}

				b.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "bundleSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "bundleSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void singletonFactory() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1f.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration = configurationAdmin.getConfiguration("singletonFactory", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			// only accept factory configuration instances
			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			configuration = configurationAdmin.getFactoryConfiguration("singletonFactory", "one", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			Success<Object[], Object[]> assertSucceeded = s -> {
				Object[] values = s.getValue();

				assertThat((Map<String, Object>)values[1]).contains(
					entry("component.name", "singletonFactory")
				).contains(
					entry("service.factoryPid", "singletonFactory")
				).contains(
					entry(Constants.SERVICE_PID, Arrays.asList("singletonFactory~one"))
				).contains(
					entry("foo", "bar")
				);

				try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
					assertThat(tracker.waitForService(50)).isNotNull();
				}

				return s;
			};

			a.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();

			b.getPromise().timeout(timeout).then(assertFailed).getFailure();
			c.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			b.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "singletonFactory")
					).contains(
						entry("service.factoryPid", "singletonFactory")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("singletonFactory~one"))
					).contains(
						entry("foo", "bar")
					);

					try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
						assertThat(tracker.waitForService(50)).isNull();
					}

					return s;
				},
				f -> fail(f.toString())
			).getValue();
			c.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "singletonFactory")
					).contains(
						entry("service.factoryPid", "singletonFactory")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("singletonFactory~one"))
					).contains(
						entry("foo", "bar")
					);

					return s;
				},
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();

			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void singletonSingle_C() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1e.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration = configurationAdmin.getConfiguration("singletonSingle_C", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			Success<Object[], Object[]> assertSucceeded = s -> {
				Object[] values = s.getValue();

				assertThat((Map<String, Object>)values[1]).contains(
					entry("component.name", "singletonSingle_C")
				).contains(
					entry(Constants.SERVICE_PID, Arrays.asList("singletonSingle_C"))
				).contains(
					entry("foo", "bar")
				);

				try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
					assertThat(tracker.waitForService(50)).isNotNull();
				}

				return s;
			};

			a.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();

			b.getPromise().timeout(timeout).then(assertFailed).getFailure();
			c.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			b.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "singletonSingle_C")
					).contains(
						entry(Constants.SERVICE_PID, Arrays.asList("singletonSingle_C"))
					).contains(
						entry("foo", "bar")
					);

					try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
						assertThat(tracker.waitForService(50)).isNull();
					}

					return s;
				},
				f -> fail(f.toString())
			).getValue();
			c.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "singletonSingle_C")
						).contains(
							entry(Constants.SERVICE_PID, Arrays.asList("singletonSingle_C"))
						).contains(
							entry("foo", "bar")
						);

						return s;
					},
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void singletonSingle() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1d.jar");

		try {
			getBeanManager(tbBundle);

			a.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "singletonSingle")
					);

					try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
						assertThat(tracker.waitForService(50)).isNotNull();
					}

					return s;
				},
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			try {
				tbBundle.uninstall();

				b.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "singletonSingle")
						);

						try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
							assertThat(tracker.waitForService(50)).isNull();
						}

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "singletonSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void immediateFactory() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1c.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration = configurationAdmin.getConfiguration("immediateFactory", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			// only accept factory configuration instances
			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			configuration = configurationAdmin.getFactoryConfiguration("immediateFactory", "one", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			Success<Object[], Object[]> assertSucceeded = s -> {
				Object[] values = s.getValue();

				assertThat((Map<String, Object>)values[1]).contains(
					entry("component.name", "immediateFactory")
				).contains(
					entry("service.factoryPid", "immediateFactory")
				).contains(
					entry(Constants.SERVICE_PID, Arrays.asList("immediateFactory~one"))
				).contains(
					entry("foo", "bar")
				);

				try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
					assertThat(tracker.waitForService(50)).isNull();
				}

				return s;
			};

			a.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();

			b.getPromise().timeout(timeout).then(assertFailed).getFailure();
			c.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			b.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();
			c.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				tbBundle.uninstall();

			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void immediateSingle_C() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1b.jar");
		Configuration configuration = null;

		try {
			getBeanManager(tbBundle);

			Success<Object[], Object[]> assertFailed = s -> {
				fail("shouldn't have have succeeded");
				return s;
			};

			a.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration = configurationAdmin.getConfiguration("immediateSingle_C", "?");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			Success<Object[], Object[]> assertSucceeded = s -> {
				Object[] values = s.getValue();

				assertThat((Map<String, Object>)values[1]).contains(
					entry("component.name", "immediateSingle_C")
				).contains(
					entry(Constants.SERVICE_PID, Arrays.asList("immediateSingle_C"))
				).contains(
					entry("foo", "bar")
				);

				try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
					assertThat(tracker.waitForService(50)).isNull();
				}

				return s;
			};

			a.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();

			b.getPromise().timeout(timeout).then(assertFailed).getFailure();
			c.getPromise().timeout(timeout).then(assertFailed).getFailure();

			configuration.delete();

			b.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();
			c.getPromise().timeout(timeout).then(
				assertSucceeded,
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			if (configuration != null) {
				try {
					configuration.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			onInitializedReg.unregister();
			onBeforeDestroyedReg.unregister();
			onDestroyedReg.unregister();
			tbBundle.uninstall();
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void immediateSingle() throws Exception {
		Deferred<Object[]> a = new Deferred<>();
		Deferred<Object[]> b = new Deferred<>();
		Deferred<Object[]> c = new Deferred<>();

		Consumer<Object[]> onInitialized = (o) -> {try {a.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onBeforeDestroyed = (o) -> {try {b.resolve(o);} catch (Exception e) {}};
		Consumer<Object[]> onDestroyed = (o) -> {try {c.resolve(o);} catch (Exception e) {}};

		ServiceRegistration<Consumer> onInitializedReg = bundleContext.registerService(
			Consumer.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Consumer> onBeforeDestroyedReg = bundleContext.registerService(
			Consumer.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Consumer> onDestroyedReg = bundleContext.registerService(
			Consumer.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Bundle tbBundle = installBundle("tb152_3_1_1a.jar");

		try {
			getBeanManager(tbBundle);

			a.getPromise().timeout(timeout).then(
				s -> {
					Object[] values = s.getValue();

					assertThat((Map<String, Object>)values[1]).contains(
						entry("component.name", "immediateSingle")
					);

					try (CloseableTracker<Object, Object> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
						assertThat(tracker.waitForService(50)).isNull();
					}

					return s;
				},
				f -> fail(f.toString())
			).getValue();
		}
		finally {
			try {
				tbBundle.uninstall();

				b.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "immediateSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();

				c.getPromise().timeout(timeout).then(
					s -> {
						Object[] values = s.getValue();

						assertThat((Map<String, Object>)values[1]).contains(
							entry("component.name", "immediateSingle")
						);

						return s;
					},
					f -> fail(f.toString())
				).getValue();
			}
			finally {
				onInitializedReg.unregister();
				onBeforeDestroyedReg.unregister();
				onDestroyedReg.unregister();
			}
		}
	}

	private void reset(@SuppressWarnings("unchecked") AtomicReference<Deferred<Object[]>> ... refs) {
		for (AtomicReference<Deferred<Object[]>> ref : refs) {
			ref.set(new Deferred<Object[]>());
		}
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
