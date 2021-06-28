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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

public class JndiExtensionTests extends SlimBaseTestCase {

	@Test
	public void testGetBeanManagerThroughJNDI() throws Exception {
		Bundle testBundle = bundleInstaller.installBundle("tb21.jar");

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
		assertFalse(bundleTracker.isEmpty());


		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Bundle extensionBundle = bundleTracker.getBundles()[0];

			Bundle testBundle = bundleInstaller.installBundle("tb21.jar", false);

			try (CloseableTracker<BeanManager, BeanManager> bmTracker = trackBM(testBundle.getBundleId());) {
				assertThat(bmTracker).matches(CloseableTracker::isEmpty, "BeanManager tracker is empty");

				int trackingCount = bmTracker.getTrackingCount();

				testBundle.start();

				for (int i = 1000; (i > 0) && bmTracker.getTrackingCount() == trackingCount; i--) {
					Thread.sleep(20);
				}

				assertThat(bmTracker).matches(t -> !t.isEmpty(), "BeanManager tracker is not empty");

				trackingCount = bmTracker.getTrackingCount();

				extensionBundle.stop();

				for (int i = 1000; (i > 0) && bmTracker.getTrackingCount() == trackingCount; i--) {
					Thread.sleep(20);
				}

				assertThat(extensionBundle).matches(b -> (b.getState() & Bundle.ACTIVE) == 0, "JNDI Extension Bundle is not Active");

				assertThat(bmTracker).matches(
					CloseableTracker::isEmpty, String.format("Is empty: <%s>", Arrays.toString(bmTracker.getServiceReferences()))
				);

				extensionBundle.start();

				for (int i = 100; (i > 0) && bmTracker.isEmpty(); i--) {
					Thread.sleep(100);
				}

				assertThat(bmTracker).matches(c -> !c.isEmpty(), "Not empty");
			}
		}
		finally {
			bundleTracker.close();
		}
	}

}