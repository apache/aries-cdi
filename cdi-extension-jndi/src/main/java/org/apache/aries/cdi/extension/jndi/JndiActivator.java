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

package org.apache.aries.cdi.extension.jndi;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.enterprise.inject.spi.Extension;
import javax.naming.spi.ObjectFactory;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.service.cdi.CDIConstants;
import org.osgi.service.cdi.annotations.RequireCDIImplementation;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.log.LoggerFactory;
import org.osgi.util.tracker.ServiceTracker;

@Capability(
	attribute = "objectClass:List<String>=javax.enterprise.inject.spi.Extension",
	namespace = ServiceNamespace.SERVICE_NAMESPACE,
	uses= {
		javax.enterprise.context.Initialized.class,
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class,
		javax.naming.Context.class,
		javax.naming.spi.ObjectFactory.class,
		org.osgi.service.cdi.CDIConstants.class,
		org.osgi.service.jndi.JNDIConstants.class,
		org.osgi.service.log.LoggerFactory.class,
		org.osgi.util.promise.Promise.class
	}
)
@Capability(
	namespace = CDIConstants.CDI_EXTENSION_PROPERTY,
	name = "aries.cdi.jndi",
	uses= {
		javax.enterprise.context.Initialized.class,
		javax.enterprise.event.Observes.class,
		javax.enterprise.inject.spi.Extension.class,
		javax.naming.Context.class,
		javax.naming.spi.ObjectFactory.class,
		org.osgi.service.cdi.CDIConstants.class,
		org.osgi.service.jndi.JNDIConstants.class,
		org.osgi.service.log.LoggerFactory.class,
		org.osgi.util.promise.Promise.class
	},
	version = "0.0.2"
)
@Header(
	name = Constants.BUNDLE_ACTIVATOR,
	value = "${@class}"
)
@RequireCDIImplementation
public class JndiActivator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		_lft = new ServiceTracker<>(context, LoggerFactory.class, null);
		_lft.open();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(CDIConstants.CDI_EXTENSION_PROPERTY, "aries.cdi.jndi");
		properties.put(JNDIConstants.JNDI_URLSCHEME, "java");
		properties.put(Constants.SERVICE_DESCRIPTION, "Aries CDI - JNDI Portable Extension Factory");
		properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");

		_serviceRegistration = context.registerService(
			new String[] {Extension.class.getName(), ObjectFactory.class.getName()},
			new JndiExtensionFactory(_lft.getService()), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		_serviceRegistration.unregister();
		_lft.close();
	}

	private volatile ServiceTracker<LoggerFactory, LoggerFactory> _lft;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration _serviceRegistration;

}
