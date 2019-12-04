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

import java.util.Collection;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.runtime.dto.ResourceMethodInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.util.tracker.ServiceTracker;

public class MpMetricsTests extends SlimTestCase {

	@Test
	public void testMetrics() throws Exception {
		Bundle tb2Bundle = installBundle("tb22.jar", false);

		tb2Bundle.start();

		ServiceTracker<Pojo, Pojo> st = new ServiceTracker<Pojo, Pojo>(
			bundleContext, Pojo.class, null);
		st.open(true);

		try {
			Pojo pojo = st.waitForService(timeout);
			assertNotNull(pojo);

			WebTarget webTarget = cb.build().target(getEndpoint()).path("/metrics/application");

			Response response = webTarget.request().get();

			assertEquals(response.readEntity(String.class),200, response.getStatus());

			String result = response.readEntity(String.class);

			assertEquals("{\"org.apache.aries.cdi.test.tb22.A.applicationCount\":0}", result);

			Assertions.assertThat(pojo.foo("Count: ")).isEqualTo("Count: 1");

			response = webTarget.request().get();

			assertEquals(200, response.getStatus());

			result = response.readEntity(String.class);

			assertEquals("{\"org.apache.aries.cdi.test.tb22.A.applicationCount\":1}", result);
		}
		finally {
			st.close();
			tb2Bundle.uninstall();
		}
	}

	private String getEndpoint() {
		Object endpointsObj = jsrReference.getProperty(
			JaxrsServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT);

		if (endpointsObj instanceof String) {
			return String.valueOf(endpointsObj);
		}
		else if (endpointsObj instanceof String[]) {
			return ((String[])endpointsObj)[0];
		}
		else if (endpointsObj instanceof Collection) {
			return String.valueOf(((Collection<?>)endpointsObj).iterator().next());
		}

		return null;
	}

	private ResourceDTO waitFor(String path) throws InterruptedException {
		return waitFor(path, 20);
	}

	private ResourceDTO waitFor(String path, int intervals) throws InterruptedException {
		for (int j = intervals; j > 0; j--) {
			RuntimeDTO runtimeDTO = jsr.getRuntimeDTO();

			for (ResourceDTO curResourceDTO : runtimeDTO.defaultApplication.resourceDTOs) {
				for (ResourceMethodInfoDTO rmid : curResourceDTO.resourceMethods) {
					if (path.equals(rmid.path)) {
						return curResourceDTO;
					}
				}
			}

			Thread.sleep(50);
		}

		assertTrue(String.format("%s not found in time", path), false);

		return null;
	}

	@Before
	@Override
	public void setUp() throws Exception {
		hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);

		hsrTracker.open();

		hsr = hsrTracker.waitForService(timeout);

		jsrTracker = new ServiceTracker<>(bundleContext, JaxrsServiceRuntime.class, null);

		jsrTracker.open();

		jsr = jsrTracker.waitForService(timeout);

		jsrReference = jsrTracker.getServiceReference();

		cbTracker = new ServiceTracker<>(bundleContext, ClientBuilder.class, null);

		cbTracker.open();

		cb = cbTracker.waitForService(timeout);

		assertNotNull(cb);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		hsrTracker.close();
		jsrTracker.close();
		cbTracker.close();
	}

	private ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);
	private HttpServiceRuntime hsr;
	private ClientBuilder cb;
	private ServiceTracker<ClientBuilder, ClientBuilder> cbTracker;
	private JaxrsServiceRuntime jsr;
	private ServiceReference<JaxrsServiceRuntime> jsrReference;
	private ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime> jsrTracker;

}
