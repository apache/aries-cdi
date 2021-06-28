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

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.Configuration;

public class ConfigurationTests extends SlimBaseTestCase {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testConfiguration() throws Exception {
		Bundle tb3Bundle = bundleInstaller.installBundle("tb3.jar");

		Configuration configurationA = null, configurationB = null;

		try {
			int attempts = 50;
			ComponentDTO configurationBeanA = null;

			while (--attempts > 0) {
				ContainerDTO containerDTO = getContainerDTO(tb3Bundle);

				configurationBeanA = containerDTO.components.stream().filter(
					c -> c.template.name.equals("configurationBeanA")
				).findFirst().orElse(null);

				if (configurationBeanA != null) {
					break;
				}
				Thread.sleep(100);
			}

			List<ConfigurationTemplateDTO> requiredConfigs = configurationBeanA.template.configurations.stream().filter(
				tconf -> tconf.policy == ConfigurationPolicy.REQUIRED
			).collect(Collectors.toList());

			assertTrue(
				configurationBeanA.instances.get(0).configurations.stream().noneMatch(
					iconf -> requiredConfigs.stream().anyMatch(rc -> rc == iconf.template)
				)
			);

			configurationA = car.getConfiguration("configurationBeanA", "?");

			Dictionary<String, Object> p1 = new Hashtable<>();
			p1.put("ports", new int[] {12, 4567});
			configurationA.update(p1);

			assertTrue(
				configurationBeanA.instances.get(0).configurations.stream().allMatch(
					iconf -> requiredConfigs.stream().anyMatch(rc -> rc == iconf.template)
				)
			);

			configurationB = car.getConfiguration("configurationBeanB", "?");

			Dictionary<String, Object> p2 = new Hashtable<>();
			p2.put("color", "green");
			p2.put("ports", new int[] {80});
			configurationB.update(p2);

			try (CloseableTracker<BeanService, BeanService> stA = track("(&(objectClass=%s)(bean=A))", BeanService.class.getName())) {
				BeanService<Callable<int[]>> beanService = stA.waitForService(timeout);

				assertNotNull(beanService);

				assertWithRetries(() -> {
					assertEquals("blue", beanService.doSomething());
					try {
						assertArrayEquals(new int[]{12, 4567}, beanService.get().call());
					} catch (final Exception e) {
						fail(e.getMessage());
					}
				});
			}

			try (CloseableTracker<BeanService, BeanService> stB = track("(&(objectClass=%s)(bean=B))", BeanService.class.getName())) {
				final BeanService<Callable<int[]>> beanServiceB = stB.waitForService(timeout);
				assertNotNull(beanServiceB);

				assertWithRetries(() -> {
					assertEquals("green", beanServiceB.doSomething());
					try {
						assertArrayEquals(new int[]{80}, beanServiceB.get().call());
					} catch (final Exception e) {
						fail(e.getMessage());
					}
				});
			}
		}
		finally {
			if (configurationA != null) {
				try {
					configurationA.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
			if (configurationB != null) {
				try {
					configurationB.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
	}

	private void assertWithRetries(final Runnable runnable) throws Exception {
		int retries = 50;
		for (int i = 0; i < retries; i++) { // can take some time to let configuration listener get the event and update the bean
			try {
				runnable.run();
				break;
			} catch (final AssertionError ae) {
				retries--;
				if (retries == 0) {
					throw ae;
				}
				sleep(200);
			}
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testOptionalConfiguration() throws Exception {
		bundleInstaller.installBundle("tb5.jar");

		Configuration configurationC = null;

		try (CloseableTracker<BeanService, BeanService> stC = track("(&(objectClass=%s)(bean=C))", BeanService.class.getName())) {
			BeanService<Callable<int[]>> beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {35777}, beanService.get().call());
		}

		configurationC = car.getConfiguration("foo.bar", "?");

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put("ports", new int[] {12, 4567});
		configurationC.update(properties);

		try (CloseableTracker<BeanService, BeanService> stC = track("(&(objectClass=%s)(bean=C)(ports=12))", BeanService.class.getName())) {
			final BeanService<Callable<int[]>> beanServiceC = stC.waitForService(timeout);

			assertNotNull(beanServiceC);
			assertWithRetries(() -> {
				assertEquals("blue", beanServiceC.doSomething());
				try {
					assertArrayEquals(new int[]{12, 4567}, beanServiceC.get().call());
				} catch (final Exception e) {
					fail(e.getMessage());
				}
			});
		}

		configurationC.delete();

		try (CloseableTracker<BeanService, BeanService> stC = track("(&(objectClass=%s)(bean=C)(!(ports=*)))", BeanService.class.getName())) {
			final BeanService<Callable<int[]>> beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertWithRetries(() -> {
				assertEquals("blue", beanService.doSomething());
				try {
					assertArrayEquals(new int[] {35777}, beanService.get().call());
				} catch (final Exception e) {
					fail(e.getMessage());
				}
			});
		}
		finally {
			if (configurationC != null) {
				try {
					configurationC.delete();
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
	}

}
