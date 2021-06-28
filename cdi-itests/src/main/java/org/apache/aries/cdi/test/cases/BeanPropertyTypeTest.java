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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.util.tracker.ServiceTracker;

public class BeanPropertyTypeTest extends SlimBaseTestCase {

	@Test
	public void beanPropertyAnnotationsWereUsed() throws Exception {
		Bundle tbBundle = bundleInstaller.installBundle("tb13.jar");

		getBeanManager(tbBundle);

		ServletContextDTO contextDTO = waitFor("customContext");

		assertThat(contextDTO).isNotNull();
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);

		hsrTracker.open();

		hsr = hsrTracker.waitForService(timeout);

		assertNotNull(hsr);
	}

	@AfterEach
	@Override
	public void tearDown() throws Exception {
		hsrTracker.close();
	}

	private ServletContextDTO waitFor(String context) throws InterruptedException {
		return waitFor(context, 20);
	}

	private ServletContextDTO waitFor(String context, int intervals) throws InterruptedException {
		for (int j = intervals; j > 0; j--) {
			for (ServletContextDTO scDTO : hsr.getRuntimeDTO().servletContextDTOs) {
				if (scDTO.name.equals(context)) {
					return scDTO;
				}
			}

			Thread.sleep(50);
		}

		assertTrue(false, String.format("%s not found in time", context));

		return null;
	}

	private HttpServiceRuntime hsr;
	private ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> hsrTracker;

}
