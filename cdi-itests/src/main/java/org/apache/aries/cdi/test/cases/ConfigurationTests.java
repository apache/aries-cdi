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
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.Assert.*;

import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ComponentDTO;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cdi.runtime.dto.template.ConfigurationTemplateDTO;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

public class ConfigurationTests extends AbstractTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		runtimeTracker = new ServiceTracker<>(
			bundleContext, CDIComponentRuntime.class, null);
		runtimeTracker.open();

		cdiRuntime = runtimeTracker.waitForService(timeout);

		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
		adminTracker.open();
		configurationAdmin = adminTracker.getService();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		runtimeTracker.close();
		adminTracker.close();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testConfiguration() throws Exception {
		Bundle tb3Bundle = installBundle("tb3.jar");

		Configuration configurationA = null, configurationB = null;
		ServiceTracker<BeanService, BeanService> stA = null, stB = null;
		try {
			int attempts = 50;
			ComponentDTO configurationBeanA = null;

			while (--attempts > 0) {
				ContainerDTO containerDTO = getContainerDTO(cdiRuntime, tb3Bundle);

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

			configurationA = configurationAdmin.getConfiguration("configurationBeanA", "?");

			Dictionary<String, Object> p1 = new Hashtable<>();
			p1.put("ports", new int[] {12, 4567});
			configurationA.update(p1);

			assertTrue(
				configurationBeanA.instances.get(0).configurations.stream().allMatch(
					iconf -> requiredConfigs.stream().anyMatch(rc -> rc == iconf.template)
				)
			);

			configurationB = configurationAdmin.getConfiguration("configurationBeanB", "?");

			Dictionary<String, Object> p2 = new Hashtable<>();
			p2.put("color", "green");
			p2.put("ports", new int[] {80});
			configurationB.update(p2);

			stA = new ServiceTracker<>(
					bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=A))"), null);
			stA.open(true);

			BeanService<Callable<int[]>> beanService = stA.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {12, 4567}, beanService.get().call());

			stB = new ServiceTracker<>(
					bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=B))"), null);
			stB.open(true);

			beanService = stB.waitForService(timeout);
			assertNotNull(beanService);

			int retries = 50;
			for (int i = 0; i < retries; i++) { // can take some time to let configuration listener get the event and update the bean
				try {
					assertEquals("green", beanService.doSomething());
					assertArrayEquals(new int[]{80}, beanService.get().call());
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
			if (stA != null) {
				stA.close();
			}
			if (stB != null) {
				stB.close();
			}
			tb3Bundle.uninstall();
		}
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testOptionalConfiguration() throws Exception {
		Bundle tb5Bundle = installBundle("tb5.jar");

		Configuration configurationC = null;
		ServiceTracker<BeanService, BeanService> stC = null;

		try {
			Thread.sleep(1000); // <---- TODO fix this

			stC = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=C))"), null);
			stC.open(true);

			BeanService<Callable<int[]>> beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {35777}, beanService.get().call());

			configurationC = configurationAdmin.getConfiguration("foo.bar", "?");

			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("ports", new int[] {12, 4567});
			configurationC.update(properties);

			stC.close();
			stC = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=C)(ports=12))"), null);
			stC.open(true);

			beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {12, 4567}, beanService.get().call());

			configurationC.delete();

			stC.close();
			stC = new ServiceTracker<BeanService, BeanService>(
				bundleContext, bundleContext.createFilter(
					"(&(objectClass=org.apache.aries.cdi.test.interfaces.BeanService)(bean=C)(!(ports=*)))"), null);
			stC.open(true);
			beanService = stC.waitForService(timeout);

			assertNotNull(beanService);
			assertEquals("blue", beanService.doSomething());
			assertArrayEquals(new int[] {35777}, beanService.get().call());
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
			if (stC != null) {
				stC.close();
			}
			tb5Bundle.uninstall();
		}
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
