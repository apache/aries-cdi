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

package org.apache.aries.cdi.container.internal.util;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServicePermission;
import org.osgi.service.log.LoggerFactory;

public class Perms {

	private static final ServicePermission beanManagerPermission = new ServicePermission(BeanManager.class.getName(), ServicePermission.REGISTER);
	private static final ServicePermission extensionPermission = new ServicePermission(Extension.class.getName(), ServicePermission.GET);
	private static final ServicePermission loggerFactoryPermission = new ServicePermission(LoggerFactory.class.getName(), ServicePermission.GET);

	public static boolean hasBeanManagerServicePermission(BundleContext bundleContext) {
		if (System.getSecurityManager() == null) return true;

		return bundleContext.getBundle().hasPermission(
			beanManagerPermission);
	}

	public static boolean hasExtensionServicePermission(BundleContext bundleContext) {
		if (System.getSecurityManager() == null) return true;

		return bundleContext.getBundle().hasPermission(
			extensionPermission);
	}

	public static boolean hasGetServicePermission(String serviceType, BundleContext bundleContext) {
		if (System.getSecurityManager() == null) return true;

		return bundleContext.getBundle().hasPermission(
			new ServicePermission(serviceType, ServicePermission.GET));
	}

	public static boolean hasLoggerFactoryServicePermission(BundleContext bundleContext) {
		if (System.getSecurityManager() == null) return true;

		return bundleContext.getBundle().hasPermission(
			loggerFactoryPermission);
	}

	public static boolean hasRegisterServicePermission(String serviceType, BundleContext bundleContext) {
		if (System.getSecurityManager() == null) return true;

		return bundleContext.getBundle().hasPermission(
			new ServicePermission(serviceType, ServicePermission.REGISTER));
	}

}
