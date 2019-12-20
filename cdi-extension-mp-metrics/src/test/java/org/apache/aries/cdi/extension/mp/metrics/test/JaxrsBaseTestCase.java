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

import java.util.Collection;

import javax.ws.rs.client.ClientBuilder;

import org.junit.Rule;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants;
import org.osgi.test.junit4.service.ServiceUseRule;

public abstract class JaxrsBaseTestCase extends HttpBaseTestCase {

	@Rule
	public ServiceUseRule<JaxrsServiceRuntime> jsrr = new ServiceUseRule.Builder<JaxrsServiceRuntime>(JaxrsServiceRuntime.class, bcr).build();
	@Rule
	public ServiceUseRule<ClientBuilder> cbr = new ServiceUseRule.Builder<ClientBuilder>(ClientBuilder.class, bcr).build();

	public String getJaxrsEndpoint() {
		Object endpointsObj = jsrr.getServiceReference().getProperty(
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

}
