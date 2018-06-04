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

package org.apache.aries.cdi.test.tb11;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.BeanId;
import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.Reluctant;
import org.osgi.service.cdi.annotations.Service;

@BeanId("as")
@ApplicationScoped
@Service
public class OptionalReference_AS implements Pojo {

	@Inject
	@Reluctant
	@Reference
	@BeanId("as")
	Optional<Integer> service;

	@Override
	public String foo(String fooInput) {
		return fooInput + service.orElse(-1);
	}

	@Override
	public int getCount() {
		return service.orElse(-1);
	}

}
