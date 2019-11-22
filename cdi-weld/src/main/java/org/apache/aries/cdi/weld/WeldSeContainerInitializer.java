package org.apache.aries.cdi.weld;

import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static org.apache.aries.cdi.spi.Keys.BEANS_XML_PROPERTY;
import static org.apache.aries.cdi.spi.Keys.BUNDLECONTEXT_PROPERTY;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.spi.loader.SpiLoader;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.xml.BeansXmlParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;

public class WeldSeContainerInitializer extends SeContainerInitializer {

	public WeldSeContainerInitializer(BundleContext bundleContext) {
		weldBundleContext = bundleContext;
	}

	@Override
	public SeContainerInitializer addBeanClasses(Class<?>... classes) {
		beanClasses.addAll(Arrays.asList(classes));
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(Class<?>... packageClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Class<?>... packageClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(Package... packages) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addPackages(boolean scanRecursively, Package... packages) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addExtensions(Extension... extensions) {
		this.extensions.addAll(Arrays.asList(extensions));
		return this;
	}

	@Override
	public SeContainerInitializer addExtensions(Class<? extends Extension>... extensions) {
		this.extensionClasses.addAll(Arrays.asList(extensions));
		return this;
	}

	@Override
	public SeContainerInitializer enableInterceptors(Class<?>... interceptorClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer enableDecorators(Class<?>... decoratorClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer selectAlternatives(Class<?>... alternativeClasses) {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer selectAlternativeStereotypes(
		Class<? extends Annotation>... alternativeStereotypeClasses) {

		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer addProperty(String key, Object value) {
		properties.putIfAbsent(key, value);
		return this;
	}

	@Override
	public SeContainerInitializer setProperties(Map<String, Object> properties) {
		properties.putAll(properties);
		return this;
	}

	@Override
	public SeContainerInitializer disableDiscovery() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public SeContainerInitializer setClassLoader(ClassLoader classLoader) {
		this.classLoader = (SpiLoader)classLoader;
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SeContainer initialize() {
		Thread currentThread = Thread.currentThread();
		ClassLoader current = currentThread.getContextClassLoader();
		BundleResourcesLoader resourceLoader = new BundleResourcesLoader(requireNonNull(classLoader), weldBundleContext.getBundle());

		try {
			currentThread.setContextClassLoader(classLoader);
			clientBundleContext = requireNonNull(BundleContext.class.cast(properties.get(BUNDLECONTEXT_PROPERTY)));

			Optional.ofNullable(clientBundleContext.getBundle().getHeaders()).ifPresent(
				headers -> list(headers.elements()).stream()
					.filter(it -> it.startsWith("org.jboss.weld."))
					.forEach(key -> properties.setProperty(key, headers.get(key))
				)
			);

			// Add external extensions
			List<Metadata<Extension>> metaExtensions = extensions.stream().map(
				extension -> {
					Bundle extensionBundle = BundleReference.class.cast(extension.getClass().getClassLoader()).getBundle();
					classLoader.getBundles().add(extensionBundle);
					return new ExtensionMetadata(extension, extensionBundle.toString());
				}
			).collect(Collectors.toList());

			BeansXml beansXml = BeansXml.EMPTY_BEANS_XML;

			Collection<URL> beanDescriptorURLs = Collection.class.cast(properties.get(BEANS_XML_PROPERTY));

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

	private volatile BeanDeploymentArchive beanDeploymentArchive;
	private volatile WeldBootstrap bootstrap;
	private volatile BundleContext clientBundleContext;
	private volatile SpiLoader classLoader;
	private final Set<Class<?>> beanClasses = new HashSet<>();
	private final List<Extension> extensions = new ArrayList<>();
	private final List<Class<? extends Extension>> extensionClasses = new ArrayList<>();
	private final BundleContext weldBundleContext;
	private final Properties properties = new Properties();

	private class WeldSeContainer implements SeContainer {

		private volatile boolean running = true;

		@Override
		public void close() {
			running = false;
			Thread currentThread = Thread.currentThread();
			ClassLoader current = currentThread.getContextClassLoader();
			try {
				currentThread.setContextClassLoader(requireNonNull(classLoader));
				bootstrap.shutdown();
			}
			finally {
				currentThread.setContextClassLoader(current);
			}
		}

		@Override
		public Instance<Object> select(Annotation... qualifiers) {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().select(qualifiers);
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().select(subtype, qualifiers);
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().select(subtype, qualifiers);
		}

		@Override
		public boolean isUnsatisfied() {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().isUnsatisfied();
		}

		@Override
		public boolean isAmbiguous() {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().isAmbiguous();
		}

		@Override
		public void destroy(Object instance) {
			bootstrap.getManager(beanDeploymentArchive).createInstance().destroy(instance);
		}

		@Override
		public Iterator<Object> iterator() {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().iterator();
		}

		@Override
		public boolean isRunning() {
			return running;
		}

		@Override
		public BeanManager getBeanManager() {
			return bootstrap.getManager(beanDeploymentArchive);
		}

		@Override
		public Object get() {
			return bootstrap.getManager(beanDeploymentArchive).createInstance().get();
		}

	}

}
