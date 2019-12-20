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

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.test.cases.base.BaseTestCase;
import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class OSGiBeanDescriptorTests extends BaseTestCase {

	@Test
	public void testServices() throws Exception {
		bcr.installBundle("tb2.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Pojo pojo = tracker.waitForService(timeout);
			assertNotNull(pojo);
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testReferences() throws Exception {
		Bundle tb1Bundle = bcr.installBundle("tb1.jar");
		bcr.installBundle("tb2.jar");

		BeanManager beanManager = getBeanManager(tb1Bundle);
		Set<Bean<?>> beans = beanManager.getBeans("beanimpl");
		Bean<?> bean = beanManager.resolve(beans);
		CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
		BeanService<?> beanService = (BeanService<?>)beanManager.getReference(
			bean, new TypeLiteral<BeanService<?>>() {}.getType(), ctx);

		assertNotNull(beanService);
		assertEquals("POJO-IMPLBEAN-IMPL", beanService.doSomething());
	}

}
