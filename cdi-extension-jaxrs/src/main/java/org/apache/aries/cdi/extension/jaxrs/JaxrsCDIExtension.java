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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
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

import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extra.propertytypes.JaxrsApplicationBase;
import org.apache.aries.cdi.extra.propertytypes.JaxrsApplicationSelect;
import org.apache.aries.cdi.extra.propertytypes.JaxrsExtension;
import org.apache.aries.cdi.extra.propertytypes.JaxrsExtensionSelect;
import org.apache.aries.cdi.extra.propertytypes.JaxrsName;
import org.apache.aries.cdi.extra.propertytypes.JaxrsResource;
import org.apache.aries.cdi.extra.propertytypes.JaxrsWhiteboardTarget;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.Service;
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
		ProcessAnnotatedType<? extends Application> pat, BeanManager beanManager) {

		AnnotatedType<? extends Application> annotatedType = pat.getAnnotatedType();

		applications.add(annotatedType);

		commonProperties(pat, Application.class, true, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsApplicationBase.class)) {
				configurator.add(
						JaxrsApplicationBase.Literal.of(
								annotatedType.getAnnotation(ApplicationPath.class).value()));
			}
		});
	}

	<X> void resource(
		@Observes @WithAnnotations({Path.class, DELETE.class, GET.class, HEAD.class, OPTIONS.class, PATCH.class, POST.class, PUT.class})
		ProcessAnnotatedType<X> pat, BeanManager beanManager) {

		AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, Object.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsResource.class)) {
				configurator.add(JaxrsResource.Literal.INSTANCE);
			}
		});
	}

	void containerRequestFilter(
		@Observes ProcessAnnotatedType<? extends ContainerRequestFilter> pat, BeanManager beanManager) {

		AnnotatedType<? extends ContainerRequestFilter> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ContainerRequestFilter.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void containerResponseFilter(
		@Observes ProcessAnnotatedType<? extends ContainerResponseFilter> pat, BeanManager beanManager) {

		AnnotatedType<? extends ContainerResponseFilter> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ContainerResponseFilter.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void readerInterceptor(
		@Observes ProcessAnnotatedType<? extends ReaderInterceptor> pat, BeanManager beanManager) {

		AnnotatedType<? extends ReaderInterceptor> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ReaderInterceptor.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void writerInterceptor(
		@Observes ProcessAnnotatedType<? extends WriterInterceptor> pat, BeanManager beanManager) {

		AnnotatedType<? extends WriterInterceptor> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, WriterInterceptor.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	void messageBodyReader(
		@Observes ProcessAnnotatedType<? extends MessageBodyReader> pat, BeanManager beanManager) {

		AnnotatedType<? extends MessageBodyReader> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, MessageBodyReader.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	void messageBodyWriter(
		@Observes ProcessAnnotatedType<? extends MessageBodyWriter> pat, BeanManager beanManager) {

		AnnotatedType<? extends MessageBodyWriter> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, MessageBodyWriter.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}

		});
	}

	@SuppressWarnings("rawtypes")
	void contextResolver(
		@Observes ProcessAnnotatedType<? extends ContextResolver> pat, BeanManager beanManager) {

		AnnotatedType<? extends ContextResolver> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ContextResolver.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	void exceptionMapper(
		@Observes ProcessAnnotatedType<? extends ExceptionMapper> pat, BeanManager beanManager) {

		AnnotatedType<? extends ExceptionMapper> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ExceptionMapper.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void paramConverterProvider(
		@Observes ProcessAnnotatedType<? extends ParamConverterProvider> pat, BeanManager beanManager) {

		AnnotatedType<? extends ParamConverterProvider> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, ParamConverterProvider.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void feature(
		@Observes ProcessAnnotatedType<? extends Feature> pat, BeanManager beanManager) {

		AnnotatedType<? extends Feature> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, Feature.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	void dynamicFeature(
		@Observes ProcessAnnotatedType<? extends DynamicFeature> pat, BeanManager beanManager) {

		AnnotatedType<? extends DynamicFeature> annotatedType = pat.getAnnotatedType();

		commonProperties(pat, DynamicFeature.class, false, beanManager).ifPresent(configurator -> {
			if (!annotatedType.isAnnotationPresent(JaxrsExtension.class)) {
				configurator.add(JaxrsExtension.Literal.INSTANCE);
			}
		});
	}

	/*
	 * @return true if common properties were added (i.e. if no @Service was found)
	 */
	private <X> Optional<AnnotatedTypeConfigurator<X>> commonProperties(
			ProcessAnnotatedType<X> pat, Class<?> serviceType, boolean application, BeanManager beanManager) {
		if (pat.getAnnotatedType().isAnnotationPresent(Service.class)) {
			return empty();
		}
		beanManager.fireEvent(new MergeServiceTypes<>(pat, serviceType));
		final AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();
		final AnnotatedType<?> annotatedType = pat.getAnnotatedType();
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
		return of(configurator);
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv) {
		if (applications.size() > 1) {
			adv.addDeploymentProblem(
				new DeploymentException(
					"More than one javax.ws.rs.core.Application annotated types were found in the CDI bundle."));
		}
	}

}
