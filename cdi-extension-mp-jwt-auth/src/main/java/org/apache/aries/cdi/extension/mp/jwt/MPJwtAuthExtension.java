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

package org.apache.aries.cdi.extension.mp.jwt;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.aries.cdi.extension.mp.jwt.MPJwtAuthExtension.EXTENSION_NAME;
import static org.apache.aries.cdi.extension.mp.jwt.MPJwtAuthExtension.EXTENSION_VERSION;
import static org.osgi.framework.Constants.SCOPE_PROTOTYPE;
import static org.osgi.framework.Constants.SERVICE_BUNDLEID;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_SCOPE;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.aries.cdi.extra.propertytypes.JaxrsExtensionSelect;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.apache.geronimo.microprofile.impl.jwtauth.cdi.GeronimoJwtAuthExtension;
import org.apache.geronimo.microprofile.impl.jwtauth.config.GeronimoJwtAuthConfig;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.GeronimoJwtAuthExceptionMapper;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.JAXRSRequestForwarder;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.RolesAllowedFeature;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.GeronimoJwtAuthFilter;
import org.eclipse.microprofile.auth.LoginConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

import aQute.bnd.annotation.spi.ServiceProvider;

@ServiceProvider(
	attribute = {
		CDI_EXTENSION_PROPERTY + '=' + EXTENSION_NAME,
		SERVICE_SCOPE + '=' + SCOPE_PROTOTYPE,
		SERVICE_VENDOR + "=Apache Software Foundation",
		"version:Version=" + EXTENSION_VERSION
	},
	uses = Extension.class,
	value = Extension.class
)
public class MPJwtAuthExtension extends GeronimoJwtAuthExtension implements BiConsumer<HttpServletRequest, Runnable> {

	public final static String EXTENSION_NAME = "eclipse.microprofile.jwt-auth";
	public final static String EXTENSION_VERSION = "1.1.1";

	@SuppressWarnings("serial")
	private final static Set<String> defaultSelects = new HashSet<String>() {{
		add(format("(%s=%s)", JAX_RS_NAME, "jwt.roles.allowed"));
		add(format("(%s=%s)", JAX_RS_NAME, "jwt.request.forwarder"));
		add(format("(%s=%s)", JAX_RS_NAME, "jwt.exception.mapper"));
		add(format("(&(objectClass=%s)(%s=%s))", MessageBodyReader.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON));
		add(format("(&(objectClass=%s)(%s=%s))", MessageBodyWriter.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON));
	}};

	private volatile BundleContext bundleContext;
	private volatile Configuration configuration;

	void getBundleContext(@Observes BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	final List<AnnotatedType<? extends Application>> applications = new CopyOnWriteArrayList<>();

	void addLoginConfigs(@Observes @WithAnnotations(LoginConfig.class) ProcessAnnotatedType<? extends Application> pat) {
		AnnotatedType<? extends Application> annotatedType = pat.getAnnotatedType();

		LoginConfig loginConfig = annotatedType.getAnnotation(LoginConfig.class);

		if ("MP-JWT".equalsIgnoreCase(loginConfig.authMethod())) {
			applications.add(pat.getAnnotatedType());

			AnnotatedTypeConfigurator<? extends Application> configurator = pat.configureAnnotatedType();

			Set<String> selectSet = ofNullable((String[])configuration.get(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT)).map(selects -> {
				Set<String> mergedSelects = new HashSet<>(defaultSelects);
				if (selects.length > 0) {
					mergedSelects.addAll(Arrays.asList(selects));
				}
				return mergedSelects;
			}).orElse(defaultSelects);

			JaxrsExtensionSelect jaxrsExtensionSelect = annotatedType.getAnnotation(JaxrsExtensionSelect.class);

			if (jaxrsExtensionSelect != null) {
				Arrays.asList(jaxrsExtensionSelect.value()).forEach(selectSet::add);
				configurator.remove(jaxrsExtensionSelect::equals);
			}

			configurator.add(JaxrsExtensionSelect.Literal.of(selectSet.toArray(new String[0])));
		}
	}

	@Override
	public void accept(final HttpServletRequest t, final Runnable u) {
		execute(t, new ServletRunnable() {
			@Override
			public void run() throws ServletException, IOException {
				u.run();
			}
		});
	}

	void registerSecurityExtensions(
		@Observes AfterDeploymentValidation adv, BeanManager beanManager) {

		try {
			registerHttpWhiteboardJwtAuthFilter(beanManager);
			registerJaxrsExceptionMapper(beanManager);
			registerJaxrsRequestForwarder(beanManager);
			registerJaxrsRolesAllowed(beanManager);
		}
		catch (Throwable t) {
			adv.addDeploymentProblem(t);
		}
	}

	void beforeShutdown(@Observes BeforeShutdown bs) {
		unregister(_exceptionMapperRegistration);
		unregister(_requestForwarderRegistration);
		unregister(_rolesAllowedRegistration);
		unregister(_jwtAuthFilterRegistration);
		_cccs.forEach(CreationalContext::release);
	}

	void registerHttpWhiteboardJwtAuthFilter(BeanManager beanManager) {
		final GeronimoJwtAuthConfig config = GeronimoJwtAuthConfig.create();
		final boolean forceSetup = "true".equalsIgnoreCase(config.read("filter.active", "false"));
		if (forceSetup || !applications.isEmpty()) {
			registerJwtAuthFilter(config, beanManager);
		}
	}

	void registerJwtAuthFilter(GeronimoJwtAuthConfig config, BeanManager beanManager) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP JWT Auth Servlet Filter");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(HTTP_WHITEBOARD_FILTER_NAME, "geronimo-microprofile-jwt-auth-filter");
		properties.put(HTTP_WHITEBOARD_FILTER_PATTERN, config.read("filter.mapping.default", "/*"));
		properties.put(HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED, true);
		properties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE - 1000);

		final GeronimoJwtAuthFilter jwtAuthFilter = get(GeronimoJwtAuthFilter.class, beanManager);

		_jwtAuthFilterRegistration = bundleContext.registerService(
			Filter.class,
			new Filter() {
				final Filter delegate = jwtAuthFilter;
				final ClassLoader loader = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();

				@Override
				public void init(FilterConfig arg0) throws ServletException {
					Thread currentThread = Thread.currentThread();
					ClassLoader current = currentThread.getContextClassLoader();

					try {
						currentThread.setContextClassLoader(loader);
						delegate.init(arg0);
					}
					finally {
						currentThread.setContextClassLoader(current);
					}
				}

				@Override
				public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
						throws IOException, ServletException {

					Thread currentThread = Thread.currentThread();
					ClassLoader current = currentThread.getContextClassLoader();

					try {
						currentThread.setContextClassLoader(loader);
						delegate.doFilter(arg0, arg1, arg2);
					}
					finally {
						currentThread.setContextClassLoader(current);
					}
				}

				@Override
				public void destroy() {
					Thread currentThread = Thread.currentThread();
					ClassLoader current = currentThread.getContextClassLoader();

					try {
						currentThread.setContextClassLoader(loader);
						delegate.destroy();
					}
					finally {
						currentThread.setContextClassLoader(current);
					}
				}

			}, properties);
	}

	void registerJaxrsRolesAllowed(BeanManager beanManager) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP JWT Auth Roles Allowed");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, applicationSelectFilter("(!(jwt.roles.allowed=false))"));
		properties.put(JAX_RS_EXTENSION, Boolean.TRUE);
		properties.put(JAX_RS_NAME, "jwt.roles.allowed");

		RolesAllowedFeature rolesAllowed = get(RolesAllowedFeature.class, beanManager);

		_rolesAllowedRegistration = bundleContext.registerService(
			DynamicFeature.class, rolesAllowed, properties);
	}

	void registerJaxrsRequestForwarder(BeanManager beanManager) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP JWT Auth Request Forwarder");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, applicationSelectFilter("(!(jwt.request.forwarder=false))"));
		properties.put(JAX_RS_EXTENSION, Boolean.TRUE);
		properties.put(JAX_RS_NAME, "jwt.request.forwarder");

		JAXRSRequestForwarder requestForwarder = get(JAXRSRequestForwarder.class, beanManager);

		_requestForwarderRegistration = bundleContext.registerService(
			ContainerRequestFilter.class, requestForwarder, properties);
	}

	void registerJaxrsExceptionMapper(BeanManager beanManager) {
		Dictionary<String, Object> properties = new Hashtable<>();

		properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP JWT Auth Exception Mapper");
		properties.put(SERVICE_VENDOR, "Apache Software Foundation");
		properties.put(JAX_RS_APPLICATION_SELECT, applicationSelectFilter("(!(jwt.exception.mapper=false))"));
		properties.put(JAX_RS_EXTENSION, Boolean.TRUE);
		properties.put(JAX_RS_NAME, "jwt.exception.mapper");

		GeronimoJwtAuthExceptionMapper exceptionMapper = get(GeronimoJwtAuthExceptionMapper.class, beanManager);

		_exceptionMapperRegistration = bundleContext.registerService(
			ExceptionMapper.class, exceptionMapper, properties);
	}

	String applicationSelectFilter(String defaultValue) {
		return ofNullable(
			configuration.get(JAX_RS_APPLICATION_SELECT)
		).map(
			String.class::cast
		).orElse(
			format("(&(%s=%s)%s)", SERVICE_BUNDLEID, bundleContext.getBundle().getBundleId(), defaultValue)
		);
	}

	static void unregister(ServiceRegistration<?> reg) {
		if (reg != null) {
			try {
				reg.unregister();
			}
			catch (IllegalStateException ise) {
				//
			}
		}
	}

	<T> T get(Class<T> clazz, BeanManager beanManager) {
		Set<Bean<?>> beans = beanManager.getBeans(clazz, Any.Literal.INSTANCE);
		@SuppressWarnings("unchecked")
		Bean<T> bean = (Bean<T>)beanManager.resolve(beans);
		CreationalContext<T> ccc = beanManager.createCreationalContext(bean);
		try {
			return beanManager.getContext(bean.getScope()).get(bean, ccc);
		}
		finally {
			if (!beanManager.isNormalScope(bean.getScope())) {
				_cccs.add(ccc);
			}
		}
	}

	private final List<CreationalContext<?>> _cccs = new CopyOnWriteArrayList<>();

	private volatile ServiceRegistration<?> _exceptionMapperRegistration;
	private volatile ServiceRegistration<?> _jwtAuthFilterRegistration;
	private volatile ServiceRegistration<?> _requestForwarderRegistration;
	private volatile ServiceRegistration<?> _rolesAllowedRegistration;

}
