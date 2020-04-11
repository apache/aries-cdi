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

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class EventsTests extends SlimBaseTestCase {

	@Test
	public void testContainerComponentReferenceEventHandler() throws Exception {
		Bundle tb = installBundle.installBundle("tb9.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Pojo pojo = tracker.waitForService(timeout);

			assertEquals(0, pojo.getCount());
			assertEquals("[]", pojo.foo(null));

			ContainerDTO containerDTO = getContainerDTO(tb);

			long changeCount = containerDTO.changeCount;

			ServiceRegistration<Integer> int1 = bcr.getBundleContext().registerService(Integer.class, new Integer(12), null);

			try {
				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(1, pojo.getCount());
				assertEquals("[ADDED]", pojo.foo(null));

				changeCount = containerDTO.changeCount;

				Dictionary<String, Object> properties = int1.getReference().getProperties();
				properties.put("foo", "bar");
				int1.setProperties(properties);

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals("[UPDATED]", pojo.foo(null));
			}
			finally {
				changeCount = containerDTO.changeCount;

				int1.unregister();

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(0, pojo.getCount());
				assertEquals("[]", pojo.foo(null));
			}
		}
	}

	@Test
	public void testSingleComponentReferenceEventHandler() throws Exception {
		Bundle tb = installBundle.installBundle("tb10.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Pojo pojo = tracker.waitForService(timeout);

			assertEquals(0, pojo.getCount());
			assertEquals("[]", pojo.foo(null));

			ContainerDTO containerDTO = getContainerDTO(tb);

			long changeCount = containerDTO.changeCount;

			ServiceRegistration<Integer> int1 = bcr.getBundleContext().registerService(Integer.class, new Integer(12), null);

			try {
				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(1, pojo.getCount());
				assertEquals("[ADDED]", pojo.foo(null));

				changeCount = containerDTO.changeCount;

				Dictionary<String, Object> properties = int1.getReference().getProperties();
				properties.put("foo", "bar");
				int1.setProperties(properties);

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals("[UPDATED]", pojo.foo(null));
			}
			finally {
				changeCount = containerDTO.changeCount;

				int1.unregister();

				for (long i = 10; i > 0 && (getContainerDTO(tb).changeCount == changeCount); i--) {
					Thread.sleep(20);
				}

				assertEquals(0, pojo.getCount());
				assertEquals("[]", pojo.foo(null));
			}
		}
	}

}
