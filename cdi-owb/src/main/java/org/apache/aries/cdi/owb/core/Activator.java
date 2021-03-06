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

package org.apache.aries.cdi.owb.core;

import static org.osgi.framework.Constants.BUNDLE_ACTIVATOR;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.aries.cdi.spi.CDIContainerInitializer;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@Header(
	name = BUNDLE_ACTIVATOR,
	value = "${@class}"
)
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		System.setProperty("openwebbeans.web.sci.active", "false"); // we handle it ourself, disable this jetty feature
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(SERVICE_DESCRIPTION, "Aries CDI - OpenWebBeans CDIContainerInitializer Factory");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");

		_containerInitializer = bundleContext.registerService(
			CDIContainerInitializer.class, new OWBCDIContainerInitializerFactory(bundleContext), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_containerInitializer.unregister();
	}

	private ServiceRegistration<CDIContainerInitializer> _containerInitializer;

}
