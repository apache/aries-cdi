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

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.CDIComponentRuntime;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;

public class Test_discoverByBeansXml extends SlimTestCase {

	@Ignore("Until we get bnd 4.3.0")
	//@Test
	public void componentScopeContext() throws Exception {
		Bundle tbBundle = installBundle("tb14.jar");

		getBeanManager(tbBundle);

		try (CloseableTracker<CDIComponentRuntime, CDIComponentRuntime> ccrTracker = track(
				"(objectClass=%s)", CDIComponentRuntime.class.getName())) {

			CDIComponentRuntime ccr = ccrTracker.waitForService(timeout);

			ContainerTemplateDTO containerTemplateDTO = ccr.getContainerTemplateDTO(tbBundle);

			assertThat(containerTemplateDTO).isNotNull();

			List<String> beans = containerTemplateDTO.components.stream().flatMap(c -> c.beans.stream()).collect(Collectors.toList());

			assertThat(beans).isNotEmpty().contains("org.apache.aries.cdi.test.tb14.ABean");
		}
		finally {
			tbBundle.uninstall();
		}
	}

}
