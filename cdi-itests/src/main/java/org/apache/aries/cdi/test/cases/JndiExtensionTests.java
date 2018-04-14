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

import static org.junit.Assert.*;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;

public class JndiExtensionTests extends AbstractTestCase {

	@Ignore("I think there's an issue with Aries JNDI. It doesn't work well with service objects")
	@Test
	public void testGetBeanManagerThroughJNDI() throws Exception {
		assertNotNull(getBeanManager(cdiBundle));

		Thread currentThread = Thread.currentThread();
		ClassLoader contextClassLoader = currentThread.getContextClassLoader();
		try {
			BundleWiring bundleWiring = cdiBundle.adapt(BundleWiring.class);
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
		ServiceTracker<Extension, Extension> et = track(
			"(&(objectClass=%s)(osgi.cdi.extension=aries.cdi.jndi))",
			Extension.class.getName());

		assertFalse(et.isEmpty());

		Bundle extensionBundle = et.getServiceReference().getBundle();

		ServiceTracker<BeanManager, BeanManager> bmTracker = getServiceTracker(cdiBundle);

		assertNotNull(bmTracker.waitForService(timeout));

		int trackingCount = bmTracker.getTrackingCount();

		extensionBundle.stop();

		for (int i = 10; (i > 0) && (bmTracker.getTrackingCount() == trackingCount); i--) {
			Thread.sleep(20);
		}

		assertNull(bmTracker.getService());

		trackingCount = bmTracker.getTrackingCount();

		extensionBundle.start();

		for (int i = 20; (i > 0) && (bmTracker.getTrackingCount() == trackingCount); i--) {
			Thread.sleep(20);
		}

		assertNotNull(bmTracker.getService());
	}

}