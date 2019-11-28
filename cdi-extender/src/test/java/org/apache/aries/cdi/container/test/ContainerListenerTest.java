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
package org.apache.aries.cdi.container.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.container.internal.container.ConfigurationListener;
import org.apache.aries.cdi.container.internal.container.ContainerBootstrap;
import org.apache.aries.cdi.container.internal.container.ContainerState;
import org.apache.aries.cdi.container.internal.model.FactoryComponent;
import org.apache.aries.cdi.container.internal.model.SingleComponent;
import org.apache.aries.cdi.container.internal.spi.ContainerListener;
import org.apache.aries.cdi.container.internal.util.Logs;
import org.apache.aries.cdi.spi.CDIContainerInitializer;
import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.util.tracker.ServiceTracker;

public class ContainerListenerTest extends BaseCDIBundleTest {
    private enum State {
        BEFORE_START, STARTED, STOPPED
    }

    @Test
    public void ensureListenerIsCalledForSuccesses() throws Exception {
        final EmptyCdiContainer cdiContainer = new EmptyCdiContainer();
        doRun(cdiContainer, (containerBootstrap, listenerCalls) -> {
            assertTrue(listenerCalls.isEmpty());
            assertEquals(State.BEFORE_START, cdiContainer.state);
            containerBootstrap.open();
            assertEquals(singletonList("onStartSuccess"), listenerCalls);
            assertEquals(State.STARTED, cdiContainer.state);
            assertTrue(containerBootstrap.close());
            assertEquals(asList("onStartSuccess", "onStopSuccess"), listenerCalls);
            assertEquals(State.STOPPED, cdiContainer.state);
        });
    }

    @Test
    public void ensureListenerIsCalledForFailedStop() throws Exception {
        final EmptyCdiContainer cdiContainer = new EmptyCdiContainer();
        cdiContainer.failAtState = State.STOPPED;
        doRun(cdiContainer, (containerBootstrap, listenerCalls) -> {
            assertTrue(listenerCalls.isEmpty());
            assertEquals(State.BEFORE_START, cdiContainer.state);
            containerBootstrap.open();
            assertEquals(singletonList("onStartSuccess"), listenerCalls);
            assertEquals(State.STARTED, cdiContainer.state);
            assertFalse(containerBootstrap.close());
            assertEquals(asList("onStartSuccess", "onStopError"), listenerCalls);
        });
    }

    @Test
    public void ensureListenerIsCalledForFailedStartup() throws Exception {
        final EmptyCdiContainer cdiContainer = new EmptyCdiContainer();
        cdiContainer.failAtState = State.STARTED;
        doRun(cdiContainer, (containerBootstrap, listenerCalls) -> {
            assertTrue(listenerCalls.isEmpty());
            assertEquals(State.BEFORE_START, cdiContainer.state);
            try {
                containerBootstrap.open();
                fail();
            } catch (final RuntimeException re) {
                // expected
            }
            assertEquals(singletonList("onStartError"), listenerCalls);
        });
    }

    private void doRun(EmptyCdiContainer cdiContainer, final BiConsumer<ContainerBootstrap, List<String>> test) throws Exception {
        ContainerState containerState = new ContainerState(
                bundle, ccrBundle, ccrChangeCount, promiseFactory, TestUtil.mockCaSt(bundle),
                new Logs.Builder(bundle.getBundleContext()).build());

        // ensure it starts, this is not really used in this test but required due to current open() impl
        ComponentDTO componentDTO = new ComponentDTO();
        componentDTO.enabled = false;
        containerState.containerDTO().components.add(componentDTO);

        List<String> listenerCalls = new ArrayList<>();
        ContainerListener listenerSpy = ContainerListener.class.cast(Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{ContainerListener.class},
                (proxy, method, args) -> {
                    listenerCalls.add(method.getName());
                    return null; // all methods return void
                }));

        final MockServiceRegistration<ContainerListener> initializer = new MockServiceRegistration<>(
                new MockServiceReference<>(bundle, listenerSpy, new String[]{CDIContainerInitializer.class.getName()}),
                TestUtil.serviceRegistrations,
                TestUtil.serviceListeners);
        final MockServiceRegistration<ContainerListener> listener = new MockServiceRegistration<>(
                new MockServiceReference<>(bundle, listenerSpy, new String[]{ContainerListener.class.getName()}),
                TestUtil.serviceRegistrations,
                TestUtil.serviceListeners);
        ContainerBootstrap containerBootstrap = new ContainerBootstrap(
                containerState,
                new ServiceTracker<CDIContainerInitializer, ServiceObjects<CDIContainerInitializer>>(
                        bundle.getBundleContext(), CDIContainerInitializer.class, null) {
                    @Override
                    public ServiceObjects<CDIContainerInitializer> getService() {
                        return cdiContainer;
                    }
                },
                new ConfigurationListener.Builder(containerState),
                new SingleComponent.Builder(containerState, null),
                new FactoryComponent.Builder(containerState, null),
                new ServiceTracker<ContainerListener, ContainerListener>(bundle.getBundleContext(), ContainerListener.class, null) {
                    @Override
                    public ServiceReference<ContainerListener>[] getServiceReferences() {
                        return new ServiceReference[] { listener.getReference() };
                    }
                });

        try {
            test.accept(containerBootstrap, listenerCalls);
        } finally {
            Stream.of(initializer, listener).forEach(TestUtil.serviceRegistrations::remove);
        }
    }

    private static class EmptyCdiContainer implements ServiceObjects<CDIContainerInitializer> {
        private State state = State.BEFORE_START;
        private State failAtState;

        @Override
        public CDIContainerInitializer getService() {
            return new CDIContainerInitializer() {
                @Override
                public CDIContainerInitializer addBeanClasses(Class<?>... classes) {
                    return this;
                }

                @Override
                public CDIContainerInitializer addBeanXmls(URL... beanXmls) {
                    return this;
                }

                @Override
                public CDIContainerInitializer addExtension(Extension extension, Map<String, Object> properties) {
                    return this;
                }

                @Override
                public CDIContainerInitializer addProperty(String key, Object value) {
                    return this;
                }

                @Override
                public CDIContainerInitializer setClassLoader(SpiLoader spiLoader) {
                    return this;
                }

                @Override
                public CDIContainerInitializer setBundleContext(BundleContext bundleContext) {
                    return this;
                }

                @Override
                public AutoCloseable initialize() {
                    state = State.STARTED;
                    if (failAtState != null && failAtState == State.STARTED) {
                        throw new RuntimeException("failed for test");
                    }
                    return () -> {
                        if (failAtState != null && failAtState == State.STOPPED) {
                            throw new RuntimeException("failed for test");
                        }
                        state = State.STOPPED;
                    };
                }
            };
        }

        @Override
        public void ungetService(final CDIContainerInitializer service) {
            // no-op
        }

        @Override
        public ServiceReference<CDIContainerInitializer> getServiceReference() {
            return null;
        }
    }
}
