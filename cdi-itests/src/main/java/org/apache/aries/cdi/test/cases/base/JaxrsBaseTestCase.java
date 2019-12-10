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

import java.util.Collection;

import javax.ws.rs.client.ClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxrsServiceRuntimeConstants;
import org.osgi.util.tracker.ServiceTracker;

public abstract class JaxrsBaseTestCase extends SlimTestCase {

	public String getEndpoint() {
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

		cbSO = bundleContext.getServiceObjects(cbTracker.getServiceReference());

		assertThat(cb).isNotNull();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		hsrTracker.close();
		jsrTracker.close();
		cbTracker.close();
	}

	protected ClientBuilder cb;
	protected ServiceObjects<ClientBuilder> cbSO;
	protected ServiceTracker<ClientBuilder, ClientBuilder> cbTracker;
	protected HttpServiceRuntime hsr;
	protected ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);
	protected JaxrsServiceRuntime jsr;
	protected ServiceReference<JaxrsServiceRuntime> jsrReference;
	protected ServiceTracker<JaxrsServiceRuntime, JaxrsServiceRuntime> jsrTracker;

}
