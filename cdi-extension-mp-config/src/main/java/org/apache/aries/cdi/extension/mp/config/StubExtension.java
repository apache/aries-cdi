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

package org.apache.aries.cdi.extension.mp.config;

import static org.apache.aries.cdi.extension.mp.config.StubExtension.EXTENSION_NAME;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.apache.geronimo.config.cdi.ConfigExtension;
import org.apache.geronimo.config.cdi.ConfigInjectionProducer;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(
	attribute = {
		CDI_EXTENSION_PROPERTY + "=" + EXTENSION_NAME,
		"service.scope=prototype",
		"service.vendor=Apache Software Foundation",
		"version:Version=1.3.0"
	},
	effective = "active",
	uses = Extension.class,
	value = Extension.class
)
public class StubExtension extends ConfigExtension {

	public final static String EXTENSION_NAME = "eclipse.microprofile.config";

	@Override
	public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		abd.addBean().beanClass(ConfigInjectionProducer.class).createWith(c -> new ConfigInjectionProducer());

		super.registerConfigProducer(abd, bm);
	}

}
