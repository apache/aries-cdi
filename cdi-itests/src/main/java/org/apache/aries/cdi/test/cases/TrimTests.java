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
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;
import org.osgi.util.tracker.ServiceTracker;

public class TrimTests extends AbstractTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
		runtimeTracker = new ServiceTracker<>(
				bundleContext, CDIComponentRuntime.class, null);
		runtimeTracker.open();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		runtimeTracker.close();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		cdiRuntime = runtimeTracker.waitForService(timeout);
	}

	@Override
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTrimmed() throws Exception {
		Bundle tb2Bundle = installBundle("tb17.jar", false);

		tb2Bundle.start();

		try {
			ContainerDTO containerDTO = getContainerDTO(cdiRuntime, tb2Bundle);
			assertNotNull(containerDTO);

			assertEquals(5, containerDTO.template.components.get(0).beans.size());

			assertEquals(2, containerDTO.template.components.size());

			assertEquals(2, containerDTO.template.components.get(1).beans.size());
		}
		finally {
			tb2Bundle.uninstall();
		}
	}

}
