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

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

public class MpConfigTests extends SlimTestCase {

	@Ignore
	@Test
	public void testConfigIsSet() throws Exception {
		Bundle tb2Bundle = installBundle("tb16.jar", false);

		tb2Bundle.start();

		ServiceTracker<Pojo, Pojo> st = new ServiceTracker<Pojo, Pojo>(
			bundleContext, Pojo.class, null);
		st.open(true);

		try {
			Pojo pojo = st.waitForService(timeout);
			assertNotNull(pojo);
			assertEquals("[foo]", pojo.foo(null));
		}
		finally {
			st.close();
			tb2Bundle.uninstall();
		}
	}

}
