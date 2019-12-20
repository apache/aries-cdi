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

package org.apache.aries.cdi.test.cases.base;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.assertj.core.util.Arrays;
import org.junit.Rule;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.test.junit4.service.ServiceUseRule;

public abstract class HttpBaseTestCase extends SlimBaseTestCase {

	@Rule
	public ServiceUseRule<HttpServiceRuntime> hsrr = new ServiceUseRule.Builder<HttpServiceRuntime>(HttpServiceRuntime.class, bcr).build();
	@Rule
	public ServiceUseRule<HttpClientBuilderFactory> hcbfr = new ServiceUseRule.Builder<HttpClientBuilderFactory>(HttpClientBuilderFactory.class, bcr).build();

	public String getHttpEndpoint() {
		String[] endpoints = (String[])hsrr.getServiceReference().getProperty("osgi.http.endpoint");

		if (endpoints == null || endpoints.length == 0) {
			String port = (String)hsrr.getServiceReference().getProperty("org.osgi.service.http.port");
			return "http://localhost:" + port;
		}

		return endpoints[0];
	}

	public String read(HttpEntity entity) throws Exception {
		if (entity == null) {
			return null;
		}

		try (InputStream in = entity.getContent();
			java.util.Scanner s = new java.util.Scanner(in)) {

			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

	public ServletDTO waitFor(String path) throws InterruptedException {
		return waitFor(path, 20);
	}

	public ServletDTO waitFor(String path, int intervals) throws InterruptedException {
		for (int j = intervals; j > 0; j--) {
			for (ServletContextDTO scDTO : hsrr.getService().getRuntimeDTO().servletContextDTOs) {
				if (scDTO.name.equals(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME)) {
					for (ServletDTO sDTO : scDTO.servletDTOs) {
						if (Arrays.asList(sDTO.patterns).contains(path)) {
							return sDTO;
						}
					}
				}
			}

			Thread.sleep(50);
		}

		assertTrue(String.format("%s not found in time", path), false);

		return null;
	}

}
