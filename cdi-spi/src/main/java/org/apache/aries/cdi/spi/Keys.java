/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.cdi.spi;

import java.net.URL;

import javax.enterprise.inject.se.SeContainerInitializer;

public class Keys {

	private Keys() {}

	private static final String ROOT_PREFIX = "org.apache.aries.cdi.spi.";

	/**
	 * Key used for passing a {@code Collection<URL>} containing the list of {@code beans.xml}
	 * {@link URL urls} using {@link SeContainerInitializer#addProperty(String, Object)} or
	 * {@link SeContainerInitializer#setProperties(java.util.Map)}.
	 */
	public static final String BEANS_XML_PROPERTY = ROOT_PREFIX + "beansXml";

	/**
	 * Key used for passing a {@code BundleContext} of the CDI bundle using
	 * {@link SeContainerInitializer#addProperty(String, Object)} or
	 * {@link SeContainerInitializer#setProperties(java.util.Map)}.
	 */
	public static final String BUNDLECONTEXT_PROPERTY = ROOT_PREFIX + "bundleContext";


}
