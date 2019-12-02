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

package org.apache.aries.cdi.spi.loader;

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

public abstract class SpiLoader extends ClassLoader implements BundleReference {

	public abstract List<Bundle> getBundles();

	public abstract Class<?> getOrRegister(
		final String proxyClassName, final byte[] proxyBytes,
		final Package pck, final ProtectionDomain protectionDomain);

	public abstract SpiLoader handleResources(
		final Predicate<String> predicate,
		final Function<String, Enumeration<URL>> function);

	public abstract SpiLoader findClass(
		final Predicate<String> predicate,
		final Function<String, Class<?>> function);

}
