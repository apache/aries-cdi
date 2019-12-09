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

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.Extension;

import org.apache.aries.cdi.spi.CDIContainerInitializer;
import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.xml.BeansXmlParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class WeldCDIContainerInitializer extends CDIContainerInitializer {

	public WeldCDIContainerInitializer(BundleContext bundleContext) {
		weldBundleContext = bundleContext;
	}

	@Override
	public CDIContainerInitializer addBeanClasses(Class<?>... classes) {
		beanClasses.addAll(Arrays.asList(classes));
		return this;
	}

	@Override
	public CDIContainerInitializer addBeanXmls(URL... beanXmls) {
		beanDescriptorURLs.addAll(Arrays.asList(beanXmls));
		return this;
	}

	@Override
	public CDIContainerInitializer addExtension(Extension extension, Map<String, Object> properties) {
		this.extensions.put(extension, properties);
		return this;
	}

	@Override
	public CDIContainerInitializer addProperty(String key, Object value) {
		properties.putIfAbsent(key, value);
		return this;
	}

	@Override
	public CDIContainerInitializer setBundleContext(BundleContext bundleContext) {
		clientBundleContext = bundleContext;
		return this;
	}

	@Override
	public CDIContainerInitializer setClassLoader(SpiLoader spiLoader) {
		this.spiLoader = spiLoader;
		return this;
	}

	@Override
	public AutoCloseable initialize() {
		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();
		BundleResourcesLoader resourceLoader = new BundleResourcesLoader(requireNonNull(spiLoader), weldBundleContext.getBundle());

		try {
			currentThread.setContextClassLoader(spiLoader);

			// Add external extensions
			List<Metadata<Extension>> metaExtensions = extensions.entrySet().stream().map(
				e -> new ExtensionMetadata(e.getKey(), location(e.getValue()))
			).collect(Collectors.toList());

			BeansXml beansXml = BeansXml.EMPTY_BEANS_XML;

			if (!beanDescriptorURLs.isEmpty()) {
				BeansXmlParser beansXmlParser = new BeansXmlParser();
				beansXml = beansXmlParser.parse(beanDescriptorURLs);
			}

			String id = clientBundleContext.getBundle().toString();

			bootstrap = new WeldBootstrap();

			beanDeploymentArchive = new ContainerDeploymentArchive(
				resourceLoader,
				id,
				beanClasses.stream().map(Class::getName).collect(Collectors.toList()),
				beansXml);

			Deployment deployment = new ContainerDeployment(metaExtensions, beanDeploymentArchive);

			bootstrap.startExtensions(metaExtensions);
			bootstrap.startContainer(id, new ContainerEnvironment(), deployment);
			bootstrap.startInitialization();
			bootstrap.deployBeans();
			bootstrap.validateBeans();
			bootstrap.endInitialization();

			return new WeldSeContainer();
		}
		finally {
			currentThread.setContextClassLoader(current);
		}
	}

	private String location(Map<String, Object> extensionProperties) {
		return String.valueOf(extensionProperties.get(Constants.SERVICE_ID));
	}

	private volatile BeanDeploymentArchive beanDeploymentArchive;
	private final List<URL> beanDescriptorURLs = new ArrayList<>();
	private volatile WeldBootstrap bootstrap;
	private volatile BundleContext clientBundleContext;
	private volatile SpiLoader spiLoader;
	private final Set<Class<?>> beanClasses = new HashSet<>();
	private final Map<Extension, Map<String, Object>> extensions = new IdentityHashMap<>();
	private final BundleContext weldBundleContext;
	private final Properties properties = new Properties();

	private class WeldSeContainer implements AutoCloseable {

		@Override
		public void close() {
			Thread currentThread = Thread.currentThread();
			ClassLoader current = currentThread.getContextClassLoader();
			try {
				currentThread.setContextClassLoader(requireNonNull(spiLoader));
				bootstrap.shutdown();
			}
			finally {
				currentThread.setContextClassLoader(current);
			}
		}

	}

}
