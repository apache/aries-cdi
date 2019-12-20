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

package org.apache.aries.cdi.extension.mp.config.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.aries.cdi.extension.mp.config.test.interfaces.Pojo;
import org.junit.Test;

public class MpConfigTests extends BaseTestCase {

	@Test
	public void testConfigIsSet() throws Exception {
		bcr.installBundle("tb01.jar");

		try (CloseableTracker<Pojo, Pojo> tracker = track("(objectClass=%s)", Pojo.class.getName())) {
			Pojo pojo = tracker.waitForService(timeout);

			assertThat(pojo).extracting(it -> it.foo(null)).isEqualTo("[foo]");
		}
	}

}
