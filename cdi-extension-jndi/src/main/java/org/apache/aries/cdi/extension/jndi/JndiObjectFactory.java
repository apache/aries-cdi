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

import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class JndiObjectFactory implements ServiceFactory<ObjectFactory> {

	public JndiObjectFactory(JndiExtensionFactory jndiExtensionFactory) {
		_jndiExtensionFactory = jndiExtensionFactory;
	}

	@Override
	public ObjectFactory getService(Bundle bundle, ServiceRegistration<ObjectFactory> registration) {
		return _jndiExtensionFactory.getObjectFactory(bundle);
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<ObjectFactory> registration, ObjectFactory service) {
	}

	private final JndiExtensionFactory _jndiExtensionFactory;

}