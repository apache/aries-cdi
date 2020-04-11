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
import static org.junit.Assert.assertTrue;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Test;
import org.osgi.framework.Constants;

public class ProducerTest extends SlimBaseTestCase {

	@Test
	public void checkProducersAreProperlyHandled() throws Exception {
		installBundle.installBundle("tb12.jar");

		try (CloseableTracker<Pojo, Pojo> track = track("(&(objectClass=%s)(component.name=integerManager))", Pojo.class.getName())) {
			Pojo pojo = track.waitForService(5000);

			assertNotNull(pojo);
			assertEquals(4, pojo.getCount());
			assertNotNull(pojo.getMap());
			assertTrue(pojo.getMap().containsKey(Constants.SERVICE_RANKING));
			assertEquals(100000, pojo.getMap().get(Constants.SERVICE_RANKING));
		}
	}

}
