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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.cdi.extension.mp.metrics.test.interfaces.Pojo;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class MpMetricsTests extends JaxrsBaseTestCase {

	@Test
	public void testMetrics() throws Exception {
		bcr.installBundle("tb01.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Pojo pojo = tracker.waitForService(timeout);
			assertNotNull(pojo);

			WebTarget webTarget = cbr.getService().build().target(getJaxrsEndpoint()).path("/metrics/application");

			Response response = webTarget.request().get();

			assertEquals(response.readEntity(String.class),200, response.getStatus());

			String result = response.readEntity(String.class);

			assertEquals("{\"org.apache.aries.cdi.extension.mp.metrics.test.tb01.A.applicationCount\":0}", result);

			Assertions.assertThat(pojo.foo("Count: ")).isEqualTo("Count: 1");

			response = webTarget.request().get();

			assertEquals(200, response.getStatus());

			result = response.readEntity(String.class);

			assertEquals("{\"org.apache.aries.cdi.extension.mp.metrics.test.tb01.A.applicationCount\":1}", result);
		}
	}

}
