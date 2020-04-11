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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;

import javax.enterprise.inject.spi.BeanManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.aries.cdi.extension.mp.metrics.test.interfaces.Pojo;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.test.common.annotation.InjectService;

public class MpMetricsTests extends JaxrsBaseTestCase {

	@InjectService
	ClientBuilder cb;

	@InjectService(filter = "(%s=%s)", filterArguments = {JAX_RS_MEDIA_TYPE, APPLICATION_JSON})
	@SuppressWarnings("rawtypes")
	MessageBodyReader mbr;

	@Test
	public void testMetrics() throws Exception {
		Bundle bundle = installBundle.installBundle("tb01.jar");

		cb.register(mbr);

		try (CloseableTracker<BeanManager, BeanManager> bmt = track(BeanManager.class, "(service.bundleid=%d)", bundle.getBundleId())) {
			assertThat(bmt.waitForService(timeout)).isNotNull();

			try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
				Pojo pojo = tracker.waitForService(timeout);
				assertNotNull(pojo);

				WebTarget webTarget = cb.build().target(getJaxrsEndpoint()).path("/metrics/application");

				Response response = webTarget.request(APPLICATION_JSON_TYPE).get();

				assertEquals(response.readEntity(String.class),200, response.getStatus());

				String result = response.readEntity(String.class);

				assertEquals("{\"org.apache.aries.cdi.extension.mp.metrics.test.tb01.A.applicationCount\":0}", result);

				Assertions.assertThat(pojo.foo("Count: ")).isEqualTo("Count: 1");

				response = webTarget.request(APPLICATION_JSON_TYPE).get();

				assertEquals(200, response.getStatus());

				result = response.readEntity(String.class);

				assertEquals("{\"org.apache.aries.cdi.extension.mp.metrics.test.tb01.A.applicationCount\":1}", result);
			}
		}
	}

}
