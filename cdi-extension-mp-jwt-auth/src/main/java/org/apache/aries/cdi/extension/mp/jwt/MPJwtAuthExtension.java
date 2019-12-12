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
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_SCOPE;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_NAME;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET;

import java.io.IOException;
import java.util.Dictionary;
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
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
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

import org.apache.aries.cdi.spi.configuration.Configuration;
import org.apache.geronimo.microprofile.impl.jwtauth.cdi.GeronimoJwtAuthExtension;
import org.apache.geronimo.microprofile.impl.jwtauth.config.GeronimoJwtAuthConfig;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.GeronimoJwtAuthExceptionMapper;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.GroupMapper;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.JAXRSRequestForwarder;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.ResponseBuilder;
import org.apache.geronimo.microprofile.impl.jwtauth.jaxrs.RolesAllowedFeature;
import org.apache.geronimo.microprofile.impl.jwtauth.jwt.ContextualJsonWebToken;
import org.apache.geronimo.microprofile.impl.jwtauth.jwt.DateValidator;
import org.apache.geronimo.microprofile.impl.jwtauth.jwt.JwtParser;
import org.apache.geronimo.microprofile.impl.jwtauth.jwt.KidMapper;
import org.apache.geronimo.microprofile.impl.jwtauth.jwt.SignatureValidator;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.GeronimoJwtAuthFilter;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.JwtRequest;
import org.apache.geronimo.microprofile.impl.jwtauth.servlet.TokenAccessor;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.jwt.ClaimValue;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.jwt.config.Names;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

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

	private volatile BundleContext bundleContext;
	private volatile Configuration configuration;

	void getBundleContext(@Observes BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void addBeans(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
		// MP
		bbd.addAnnotatedType(bm.createAnnotatedType(ClaimValue.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JsonWebToken.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(Names.class));

		// Geronimo
		bbd.addAnnotatedType(bm.createAnnotatedType(ContextualJsonWebToken.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(DateValidator.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(GeronimoJwtAuthConfig.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(GeronimoJwtAuthExceptionMapper.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(GeronimoJwtAuthFilter.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(GroupMapper.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JAXRSRequestForwarder.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JwtParser.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(JwtRequest.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(KidMapper.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(ResponseBuilder.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(RolesAllowedFeature.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(SignatureValidator.class));
		bbd.addAnnotatedType(bm.createAnnotatedType(TokenAccessor.class));
	}

	final List<AnnotatedType<? extends Application>> applications = new CopyOnWriteArrayList<>();

	void addLoginConfigs(@Observes @WithAnnotations(LoginConfig.class) ProcessAnnotatedType<? extends Application> pat) {
		LoginConfig loginConfig = pat.getAnnotatedType().getAnnotation(LoginConfig.class);

		if ("MP-JWT".equalsIgnoreCase(loginConfig.authMethod())) {
			applications.add(pat.getAnnotatedType());
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

	void registerMetricsEndpoint(
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
		unregister(_applicationRegistration);
		unregister(_jwtAuthFilterRegistration);
		_cccs.forEach(CreationalContext::release);
	}

	void registerHttpWhiteboardJwtAuthFilter(BeanManager beanManager) {
		if (applications.size() > 1) {
			// TODO If and when we have a real JAX-RS extension we should handle this issue there as well, or maybe instead.
			throw new DeploymentException(
				"More than one javax.ws.rs.core.Application annotated types were found in the CDI bundle.");
		}

		final GeronimoJwtAuthConfig config = GeronimoJwtAuthConfig.create();
		final boolean forceSetup = "true".equalsIgnoreCase(config.read("filter.active", "false"));
		if (forceSetup) {
			registerJwtAuthFilter(config, beanManager);
		}

		applications.stream().forEach(app -> {
			registerJwtAuthFilter(config, beanManager);

			Dictionary<String, Object> properties = new Hashtable<>();

			properties.put(SERVICE_DESCRIPTION, "Aries CDI - MP JWT Enabled Application");
			properties.put(SERVICE_VENDOR, "Apache Software Foundation");
			properties.put(JAX_RS_APPLICATION_BASE, ofNullable(configuration.get(JAX_RS_APPLICATION_BASE)).orElse("/"));
			properties.put(JAX_RS_WHITEBOARD_TARGET, ofNullable(configuration.get(JAX_RS_WHITEBOARD_TARGET)).orElse("(!(geronimo.mp.jwt.app=false))"));
			properties.put(JAX_RS_NAME, ofNullable(configuration.get(JAX_RS_NAME)).orElse(".default"));
			properties.put(JAX_RS_EXTENSION_SELECT, new String[] {
				format("(%s=%s)", JAX_RS_NAME, "jwt.roles.allowed"),
				format("(%s=%s)", JAX_RS_NAME, "jwt.request.forwarder"),
				format("(%s=%s)", JAX_RS_NAME, "jwt.exception.mapper"),
				format("(&(objectClass=%s)(%s=%s))", MessageBodyReader.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON),
				format("(&(objectClass=%s)(%s=%s))", MessageBodyWriter.class.getName(), JAX_RS_MEDIA_TYPE, APPLICATION_JSON)
			});

			Application application = get(Application.class, beanManager);

			_applicationRegistration = bundleContext.registerService(
				Application.class, application, properties);
		});
	}

	void registerJwtAuthFilter(GeronimoJwtAuthConfig config, BeanManager beanManager) {
		if (_jwtAuthFilterRegistration != null) {
			return;
		}

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
			_applicationRegistration
		).map(
			ServiceRegistration::getReference
		).map(
			sr -> (Long)sr.getProperty(Constants.SERVICE_ID)
		).map(
			id ->  String.format("(service.id=%d)", id)
		).orElseGet(
			() -> ofNullable(
				configuration.get(JAX_RS_APPLICATION_SELECT)
			).map(
				String.class::cast
			).orElse(
				defaultValue
			)
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

	private volatile ServiceRegistration<?> _applicationRegistration;
	private volatile ServiceRegistration<?> _exceptionMapperRegistration;
	private volatile ServiceRegistration<?> _jwtAuthFilterRegistration;
	private volatile ServiceRegistration<?> _requestForwarderRegistration;
	private volatile ServiceRegistration<?> _rolesAllowedRegistration;

}
