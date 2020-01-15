/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.cdi.owb.spi;

import static java.util.Collections.emptyMap;

import java.util.Map;
import aQute.bnd.annotation.baseline.BaselineIgnore;

/**
 * Enables to customize the OWB context creation.
 * @param <T> the type of start object provided.
 */
public interface StartObjectSupplier<T> {
	/**
	 * @return cdi start instance (instance which can be observed when appscope is initialized).
	 */
	T getStartObject();

	/**
	 * @return enable to select the supplier to use when ambiguous (like ranking a bit), higher is selected.
	 */
	default int ordinal() {
		return 0;
	}

	/**
	 * TIP: this enables to customize the context from a bundle extension and not a service.
	 *
	 * @return a set of OWB properties.
	 */
	@BaselineIgnore("1.1.1") // does not break backward compat but baseline does not see "default"
	default Map<String, String> properties() {
		return emptyMap();
	}
}
