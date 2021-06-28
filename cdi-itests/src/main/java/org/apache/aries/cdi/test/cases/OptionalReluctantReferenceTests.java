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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Hashtable;

import org.apache.aries.cdi.test.cases.base.BaseTestCase;
import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.service.cm.Configuration;

public class OptionalReluctantReferenceTests extends BaseTestCase {

	@Test
	public void applicationScoped() throws Exception {
		Bundle tb = bundleInstaller.installBundle("tb11.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(&(objectClass=%s)(bean.id=as))", Pojo.class.getName());) {
			Pojo pojo = tracker.waitForService(timeout);

			assertEquals(-1, pojo.getCount());
			assertEquals("-1", pojo.foo(""));

			ContainerDTO containerDTO = getContainerDTO(tb);

			long changeCount = containerDTO.changeCount;

			ServiceRegistration<Integer> int1 = bundleContext.registerService(
				Integer.class, Integer.valueOf(12),
				new Hashtable<>(Collections.singletonMap("bean.id", "as")));

			try {
				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));

				tb.stop();
				tb.start();

				pojo = tracker.waitForService(timeout);

				assertEquals(12, pojo.getCount());
				assertEquals("12", pojo.foo(""));
			}
			finally {
				changeCount = getContainerDTO(tb).changeCount;

				int1.unregister();

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				pojo = tracker.waitForService(timeout);

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));
			}
		}
	}

	@Test
	public void singleComponent() throws Exception {
		Bundle tb = bundleInstaller.installBundle("tb11.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(&(objectClass=%s)(bean.id=sc))", Pojo.class.getName());) {
			Pojo pojo = tracker.waitForService(timeout);

			assertEquals(-1, pojo.getCount());
			assertEquals("-1", pojo.foo(""));

			ContainerDTO containerDTO = getContainerDTO(tb);

			long changeCount = containerDTO.changeCount;

			ServiceRegistration<Integer> int1 = bundleContext.registerService(
				Integer.class, Integer.valueOf(12),
				new Hashtable<>(Collections.singletonMap("bean.id", "sc")));

			try {
				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));

				tb.stop();
				tb.start();

				pojo = tracker.waitForService(timeout);

				assertEquals(12, pojo.getCount());
				assertEquals("12", pojo.foo(""));
			}
			finally {
				changeCount = getContainerDTO(tb).changeCount;

				int1.unregister();

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				pojo = tracker.waitForService(timeout);

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));
			}
		}
	}

	@Test
	public void factoryComponent() throws Exception {
		Bundle tb = bundleInstaller.installBundle("tb11.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(&(objectClass=%s)(bean.id=fc))", Pojo.class.getName());) {
			Pojo pojo = tracker.waitForService(timeout);

			assertNull(pojo);

			int trackingCount = tracker.getTrackingCount();

			Configuration configuration = car.createFactoryConfiguration("optionalReference_FC");
			configuration.update(new Hashtable<>(Collections.singletonMap("foo", "bar")));

			for (long i = 10; i > 0 && (tracker.getTrackingCount() == trackingCount); i--) {
				Thread.sleep(20);
			}

			pojo = tracker.waitForService(timeout);

			assertEquals(-1, pojo.getCount());
			assertEquals("-1", pojo.foo(""));

			ContainerDTO containerDTO = getContainerDTO(tb);

			long changeCount = containerDTO.changeCount;

			ServiceRegistration<Integer> int1 = bundleContext.registerService(
				Integer.class, Integer.valueOf(12),
				new Hashtable<>(Collections.singletonMap("bean.id", "fc")));

			try {
				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));

				tb.stop();
				tb.start();

				pojo = tracker.waitForService(timeout);

				assertEquals(12, pojo.getCount());
				assertEquals("12", pojo.foo(""));
			}
			finally {
				changeCount = getContainerDTO(tb).changeCount;

				int1.unregister();

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				pojo = tracker.waitForService(timeout);

				assertEquals(-1, pojo.getCount());
				assertEquals("-1", pojo.foo(""));

				configuration.delete();
			}
		}
	}

}
