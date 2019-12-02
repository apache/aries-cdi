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
import java.util.Map;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;

@ProviderType
public abstract class CDIContainerInitializer {

	/**
	 * Adds the listed classes to the resulting synthetic bean archive.
	 * <p>
	 * Contents will be added to those added during previous calls.
	 *
	 * @param classes
	 * @return this
	 */
	public abstract CDIContainerInitializer addBeanClasses(Class<?>... classes);

	/**
	 * Adds the listed bean.xml files the resulting synthetic bean archive
	 * <p>
	 * Contents will be added to those added during previous calls.
	 *
	 * @param beanXmls
	 * @return this
	 */
	public abstract CDIContainerInitializer addBeanXmls(URL... beanXmls);

	/**
	 * Adds the given Extension instance along with any service properties available to the synthetic bean archive
	 * <p>
	 * Contents will be added to those added during previous calls.
	 *
	 * @param extension
	 * @param properties
	 * @return this
	 */
	public abstract CDIContainerInitializer addExtension(Extension extension, Map<String, Object> properties);

	/**
	 * Adds a configuration property to the container
	 * <p>
	 * Contents will be added to those added during previous calls.
	 *
	 * @param key
	 * @param value
	 * @return this
	 */
	public abstract CDIContainerInitializer addProperty(String key, Object value);

	/**
	 * Sets the SpiLoader for this synthetic bean archive
	 * <p>
	 * The last such call will win.
	 *
	 * @param spiLoader
	 * @return this
	 */
	public abstract CDIContainerInitializer setClassLoader(SpiLoader spiLoader);

	/**
	 * Sets the {@link BundleContext} for which this synthetic bean archive is created
	 * <p>
	 * The last such call will win.
	 *
	 * @param bundleContext
	 * @return this
	 */
	public abstract CDIContainerInitializer setBundleContext(BundleContext bundleContext);

	/**
	 * Bootstraps the container that has been built from this CDIContainerInitializer
	 *
	 * @return a new SeContainer representing this synthetic bean archive
	 */
	public abstract AutoCloseable initialize();

}
