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

import java.util.List;
import java.util.stream.Collectors;

import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.cdi.runtime.dto.template.ContainerTemplateDTO;

public class Test_discoverByBeansXml extends SlimBaseTestCase {

	@Test
	public void componentScopeContext() throws Exception {
		Bundle tbBundle = bcr.installBundle("tb14.jar");

		getBeanManager(tbBundle);

		ContainerTemplateDTO containerTemplateDTO = ccrr.getService().getContainerTemplateDTO(tbBundle);

		assertThat(containerTemplateDTO).isNotNull();

		List<String> beans = containerTemplateDTO.components.stream().flatMap(c -> c.beans.stream()).collect(Collectors.toList());

		assertThat(beans).isNotEmpty().contains("org.apache.aries.cdi.test.tb14.ABean");
	}

}
