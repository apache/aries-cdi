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

package org.apache.aries.cdi.test.cases.jaxrs;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;

import org.apache.aries.cdi.test.cases.base.JaxrsBaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;

public class ResourceTests extends JaxrsBaseTestCase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		cdiBundle = bundleInstaller.installBundle("tb24.jar");

		int count = 100;
		RuntimeDTO runtimeDTO;
		while ((runtimeDTO = jsr.getRuntimeDTO()).defaultApplication.resourceDTOs.length < 1 && (count > 0)) {
			count--;
			Thread.sleep(100);
		}

		assertThat(runtimeDTO.defaultApplication.resourceDTOs).extracting("name").contains(
			"A");
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
	}

	@Test
	public void test() throws Exception {
		cb.connectTimeout(1000, TimeUnit.SECONDS);
		cb.readTimeout(1000, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String serverToken = client.target(getJaxrsEndpoint())
					.path("a")
					.request(TEXT_PLAIN_TYPE)
					.get(String.class);
			assertEquals("a", serverToken);
		} finally {
			client.close();
		}
	}

}
