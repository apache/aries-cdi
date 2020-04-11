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

import org.apache.aries.cdi.test.cases.base.BaseTestCase;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.dto.ContainerDTO;

public class Test152_2 extends BaseTestCase {

	@Test
	public void checkUniqueComponentNames() throws Exception {
		Bundle tb = installBundle.installBundle("tb152_2.jar");

		getBeanManager(tb);

		ContainerDTO containerDTO = getContainerDTO(tb);
		assertThat(containerDTO).isNotNull();
		assertThat(containerDTO.errors).isNotNull().asList().isNotEmpty();
	}

	@Test
	public void checkUniqueComponentNames_b() throws Exception {
		Bundle tb = installBundle.installBundle("tb152_2b.jar");

		getBeanManager(tb);

		ContainerDTO containerDTO = getContainerDTO(tb);
		assertThat(containerDTO).isNotNull();
		assertThat(containerDTO.errors).isNotNull().asList().isNotEmpty();
	}

}
