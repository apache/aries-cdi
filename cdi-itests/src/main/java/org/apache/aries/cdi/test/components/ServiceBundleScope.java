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

package org.apache.aries.cdi.test.components;

import org.apache.aries.cdi.test.interfaces.BundleScoped;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(
	property = {
		"fee.fi=fee",
		"fo.fum:Integer=23",
		"complex.enough.key=fum",
		"key=value",
		"simple.annotation=blah"
	},
	scope = ServiceScope.BUNDLE
)
public class ServiceBundleScope implements BundleScoped {

	@Override
	public Object get() {
		return this;
	}

}
