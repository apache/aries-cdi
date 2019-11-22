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

package org.apache.aries.cdi.weld;

import javax.enterprise.inject.se.SeContainerInitializer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class WeldSeContainerInitializerFactory implements PrototypeServiceFactory<SeContainerInitializer> {

	public WeldSeContainerInitializerFactory(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public SeContainerInitializer getService(
		Bundle bundle, ServiceRegistration<SeContainerInitializer> registration) {

		return new WeldSeContainerInitializer(bundleContext);
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration<SeContainerInitializer> registration, SeContainerInitializer service) {
	}

	private final BundleContext bundleContext;

}
