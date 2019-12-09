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

package org.apache.aries.cdi.test.tb17;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.log.Logger;

@Interceptor
public class H {

	@Inject Logger log;
	@Inject Pojo pojo;

	@AroundInvoke
	public Object authorize(InvocationContext ic) throws Exception {
		if (pojo.getCount() > 0) {
			log.debug("Pojo Count {}", pojo.getCount());
		}
		else {
			log.debug("Pojo has no count");
		}
		return ic.proceed();
	}
}
