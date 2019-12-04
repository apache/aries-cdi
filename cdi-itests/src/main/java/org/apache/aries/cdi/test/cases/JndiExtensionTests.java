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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;

public class JndiExtensionTests extends SlimTestCase {

	@Ignore("I think there's an issue with Aries JNDI. It doesn't work well with service objects")
	@Test
	public void testGetBeanManagerThroughJNDI() throws Exception {
		Bundle testBundle = installBundle("tb21.jar", false);

		testBundle.start();

		assertNotNull(getBeanManager(testBundle));

		Thread currentThread = Thread.currentThread();
		ClassLoader contextClassLoader = currentThread.getContextClassLoader();
		try {
			BundleWiring bundleWiring = testBundle.adapt(BundleWiring.class);
			currentThread.setContextClassLoader(bundleWiring.getClassLoader());

			BeanManager beanManager = (BeanManager)InitialContext.doLookup("java:comp/BeanManager");

			assertNotNull(beanManager);
			assertBeanExists(Pojo.class, beanManager);
		}
		finally {
			currentThread.setContextClassLoader(contextClassLoader);
		}
	}

	@Ignore("Github Actions simply does not like this test... :(")
	@Test
	public void testDisableExtensionAndCDIContainerWaits() throws Exception {
		BundleTracker<Bundle> bundleTracker = new BundleTracker<Bundle>(bundleContext, Bundle.ACTIVE, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (bundle.getSymbolicName().equals("org.apache.aries.cdi.extension.jndi")) {
					return bundle;
				}
				return null;
			}
		};
		bundleTracker.open();

		Bundle testBundle = installBundle("tb21.jar", false);

		testBundle.start();

		ServiceTracker<Pojo, Pojo> st = new ServiceTracker<Pojo, Pojo>(
			bundleContext, Pojo.class, null);
		st.open(true);

		try {
			assertFalse(bundleTracker.isEmpty());

			Bundle extensionBundle = bundleTracker.getBundles()[0];

			try (CloseableTracker<BeanManager, BeanManager> bmTracker = trackBM(testBundle);) {
				assertNotNull(bmTracker.waitForService(timeout));

				extensionBundle.stop();

				for (int i = 100; (i > 0) && !bmTracker.isEmpty(); i--) {
					Thread.sleep(100);
				}

				assertThat(bmTracker).matches(CloseableTracker::isEmpty, "Is empty");

				extensionBundle.start();

				for (int i = 100; (i > 0) && bmTracker.isEmpty(); i--) {
					Thread.sleep(100);
				}

				assertThat(bmTracker).matches(c -> !c.isEmpty(), "Not empty");
			}
		}
		finally {
			testBundle.uninstall();
			bundleTracker.close();
		}
	}

}