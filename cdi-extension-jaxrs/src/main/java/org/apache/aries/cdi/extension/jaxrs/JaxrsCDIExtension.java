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

package org.apache.aries.cdi.extension.jaxrs;

import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.apache.aries.cdi.extension.spi.adapt.Adapted;
import org.apache.aries.cdi.extra.propertytypes.JaxrsApplicationBase;
import org.apache.aries.cdi.extra.propertytypes.JaxrsApplicationSelect;
import org.apache.aries.cdi.extra.propertytypes.JaxrsExtension;
import org.apache.aries.cdi.extra.propertytypes.JaxrsExtensionSelect;
import org.apache.aries.cdi.extra.propertytypes.JaxrsName;
import org.apache.aries.cdi.extra.propertytypes.JaxrsResource;
import org.apache.aries.cdi.extra.propertytypes.JaxrsWhiteboardTarget;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ServiceInstance;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

public class JaxrsCDIExtension implements Extension {

	private volatile Configuration configuration;
	private final List<AnnotatedType<? extends Application>> applications = new CopyOnWriteArrayList<>();


	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void application(
		@Observes @WithAnnotations(ApplicationPath.class)
		ProcessAnnotatedType<? extends Application> pat) {

		AnnotatedType<? extends Application> annotatedType = pat.getAnnotatedType();

		applications.add(annotatedType);

		AnnotatedTypeConfigurator<? extends Application> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, Application.class, true)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsApplicationBase.class)) {
			configurator.add(
				JaxrsApplicationBase.Literal.of(
					annotatedType.getAnnotation(ApplicationPath.class).value()));
		}
	}

	<X> void resource(
		@Observes @WithAnnotations({Path.class, DELETE.class, GET.class, HEAD.class, OPTIONS.class, PATCH.class, POST.class, PUT.class})
		ProcessAnnotatedType<X> pat) {

		AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, Object.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsResource.class)) {
			configurator.add(JaxrsResource.Literal.INSTANCE);
		}
	}

	void containerRequestFilter(
		@Observes ProcessAnnotatedType<? extends ContainerRequestFilter> pat) {

		AnnotatedType<? extends ContainerRequestFilter> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ContainerRequestFilter> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ContainerRequestFilter.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void containerResponseFilter(
		@Observes ProcessAnnotatedType<? extends ContainerResponseFilter> pat) {

		AnnotatedType<? extends ContainerResponseFilter> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ContainerResponseFilter> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ContainerResponseFilter.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void readerInterceptor(
		@Observes ProcessAnnotatedType<? extends ReaderInterceptor> pat) {

		AnnotatedType<? extends ReaderInterceptor> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ReaderInterceptor> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ReaderInterceptor.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void writerInterceptor(
		@Observes ProcessAnnotatedType<? extends WriterInterceptor> pat) {

		AnnotatedType<? extends WriterInterceptor> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends WriterInterceptor> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, WriterInterceptor.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	@SuppressWarnings("rawtypes")
	void messageBodyReader(
		@Observes ProcessAnnotatedType<? extends MessageBodyReader> pat) {

		AnnotatedType<? extends MessageBodyReader> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends MessageBodyReader> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, MessageBodyReader.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	@SuppressWarnings("rawtypes")
	void messageBodyWriter(
		@Observes ProcessAnnotatedType<? extends MessageBodyWriter> pat) {

		AnnotatedType<? extends MessageBodyWriter> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends MessageBodyWriter> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, MessageBodyWriter.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	@SuppressWarnings("rawtypes")
	void contextResolver(
		@Observes ProcessAnnotatedType<? extends ContextResolver> pat) {

		AnnotatedType<? extends ContextResolver> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ContextResolver> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ContextResolver.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	@SuppressWarnings("rawtypes")
	void exceptionMapper(
		@Observes ProcessAnnotatedType<? extends ExceptionMapper> pat) {

		AnnotatedType<? extends ExceptionMapper> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ExceptionMapper> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ExceptionMapper.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void paramConverterProvider(
		@Observes ProcessAnnotatedType<? extends ParamConverterProvider> pat) {

		AnnotatedType<? extends ParamConverterProvider> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends ParamConverterProvider> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, ParamConverterProvider.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void feature(
		@Observes ProcessAnnotatedType<? extends Feature> pat) {

		AnnotatedType<? extends Feature> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends Feature> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, Feature.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	void dynamicFeature(
		@Observes ProcessAnnotatedType<? extends DynamicFeature> pat) {

		AnnotatedType<? extends DynamicFeature> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<? extends DynamicFeature> configurator = pat.configureAnnotatedType();

		if (!commonProperties(annotatedType, configurator, DynamicFeature.class, false)) {
			return;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
			configurator.add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	/*
	 * @return true if common properties were added (i.e. if no @Service was found)
	 */
	boolean commonProperties(
		AnnotatedType<?> annotatedType, AnnotatedTypeConfigurator<?> configurator,
		Class<?> serviceType, boolean application) {

		if (!Adapted.withServiceTypes(configurator, serviceType)) {
			return false;
		}

		if (!annotatedType.isAnnotationPresent(JaxrsName.class)) {
			if (application) {
				configurator.add(
					JaxrsName.Literal.of(
						ofNullable((String)configuration.get(JaxrsWhiteboardConstants.JAX_RS_NAME)).orElse(
							JaxrsWhiteboardConstants.JAX_RS_DEFAULT_APPLICATION
						)
					)
				);
			}
			else {
				configurator.add(JaxrsName.Literal.of(annotatedType.getJavaClass().getSimpleName()));
			}
		}

		if (!application && !annotatedType.isAnnotationPresent(JaxrsApplicationSelect.class)) {
			ofNullable((String)configuration.get(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_SELECT)).ifPresent(
				select -> configurator.add(JaxrsApplicationSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(JaxrsExtensionSelect.class)) {
			ofNullable((String[])configuration.get(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT)).ifPresent(selects -> {
				if (selects.length > 0) {
					configurator.add(JaxrsExtensionSelect.Literal.of(selects));
				}
			});
		}

		if (!annotatedType.isAnnotationPresent(JaxrsWhiteboardTarget.class)) {
			ofNullable((String)configuration.get(JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET)).ifPresent(
				target -> configurator.add(JaxrsWhiteboardTarget.Literal.of(target))
			);
		}

		if (!annotatedType.isAnnotationPresent(ServiceInstance.class)) {
			Class<? extends Annotation> beanScope = Util.beanScope(annotatedType, Dependent.class);

			if (Dependent.class.equals(beanScope)) {
				configurator.add(ServiceInstance.Literal.of(ServiceScope.PROTOTYPE));
			}
		}

		return true;
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv) {
		if (applications.size() > 1) {
			adv.addDeploymentProblem(
				new DeploymentException(
					"More than one javax.ws.rs.core.Application annotated types were found in the CDI bundle."));
		}
	}

}
