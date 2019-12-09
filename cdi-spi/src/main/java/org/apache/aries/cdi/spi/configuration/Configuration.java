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

package org.apache.aries.cdi.spi.configuration;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An event type fired early by Aries CDI allowing extensions who observe it to get
 * container configuration.
 * <p>
 * <pre>
 * private volatile Configuration configuration;
 * void getConfiguration(@Observes Configuration configuration) {
 *   this.configuration = configuration;
 * }
 * </pre>
 */
@ProviderType
public interface Configuration extends Map<String, Object> {
}
