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

package org.apache.aries.cdi.test.tb1;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.apache.aries.cdi.test.interfaces.Pojo;

@Named("beanimpl")
public class BeanImpl implements BeanService<BeanImpl> {

	@Override
	public String doSomething() {
		return pojo.foo("BEAN-IMPL");
	}

	@Override
	public BeanImpl get() {
		return this;
	}

	@Inject
	Pojo pojo;

}
