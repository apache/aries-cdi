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

import javax.enterprise.context.spi.Context;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class Test152_3 extends SlimBaseTestCase {

	@Test
	public void componentScopeContext() throws Exception {
		Bundle tbBundle = bcr.installBundle("tb152_3.jar");

		try (CloseableTracker<Object, Object> oneTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "one");
				CloseableTracker<Object, Object> twoTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "two")) {

			getBeanManager(tbBundle);

			Object service = oneTracker.waitForService(timeout);
			twoTracker.waitForService(timeout);

			assertThat(service).isNotNull();
			@SuppressWarnings("unchecked")
			BeanService<Context> bs = (BeanService<Context>)service;
			Context context = bs.get();
			assertThat(context).isNotNull();
		}
	}

}
