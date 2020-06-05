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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
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

import org.apache.aries.cdi.extension.spi.adapt.FiltersOn;
import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extension.spi.adapt.ProcessPotentialService;
import org.apache.aries.cdi.extension.spi.adapt.RegisterExtension;
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
	private final List<AnnotatedType<?>> applications = new CopyOnWriteArrayList<>();

	void register(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager manager) {
		manager.fireEvent(new RegisterExtension(this));
	}

	void getConfiguration(@Observes Configuration configuration) {
		this.configuration = configuration;
	}

	void application(
		@Observes @FiltersOn(annotations = ApplicationPath.class)
		ProcessPotentialService pat, BeanManager beanManager) {

		AnnotatedType<?> annotatedType = pat.getAnnotatedType();

		applications.add(annotatedType);

		commonProperties(pat, Application.class, true, beanManager);
		if (!annotatedType.isAnnotationPresent(JaxrsApplicationBase.class)) {
			pat.configureAnnotatedType().add(
				JaxrsApplicationBase.Literal.of(
					annotatedType.getAnnotation(ApplicationPath.class).value()));
		}
	}

	void resource(
		@Observes
		@FiltersOn(annotations = {Path.class, DELETE.class, GET.class, HEAD.class, OPTIONS.class, PATCH.class, POST.class, PUT.class})
		ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, Object.class, false, beanManager);
		if (!pat.getAnnotatedType().isAnnotationPresent(JaxrsResource.class)) {
			pat.configureAnnotatedType().add(JaxrsResource.Literal.INSTANCE);
		}
	}

	void containerRequestFilter(
		@Observes @FiltersOn(types = ContainerRequestFilter.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ContainerRequestFilter.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void containerResponseFilter(
		@Observes @FiltersOn(types = ContainerResponseFilter.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ContainerResponseFilter.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void readerInterceptor(
		@Observes @FiltersOn(types = ReaderInterceptor.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ReaderInterceptor.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void writerInterceptor(
		@Observes @FiltersOn(types = WriterInterceptor.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, WriterInterceptor.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void messageBodyReader(
		@Observes @FiltersOn(types = MessageBodyReader.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, MessageBodyReader.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void messageBodyWriter(
		@Observes @FiltersOn(types = MessageBodyWriter.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, MessageBodyWriter.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void contextResolver(
		@Observes @FiltersOn(types = ContextResolver.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ContextResolver.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void exceptionMapper(
		@Observes @FiltersOn(types = ExceptionMapper.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ExceptionMapper.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void paramConverterProvider(
		@Observes @FiltersOn(types = ParamConverterProvider.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, ParamConverterProvider.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void feature(
		@Observes @FiltersOn(types = Feature.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, Feature.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	void dynamicFeature(
		@Observes @FiltersOn(types = DynamicFeature.class) ProcessPotentialService pat, BeanManager beanManager) {

		commonProperties(pat, DynamicFeature.class, false, beanManager);
		addJaxRsExtension(pat);
	}

	private void addJaxRsExtension(final ProcessPotentialService pat) {
		if (!pat.getAnnotatedType().isAnnotationPresent(JaxrsExtension.class)) {
			pat.configureAnnotatedType().add(JaxrsExtension.Literal.INSTANCE);
		}
	}

	/*
	 * @return true if common properties were added (i.e. if no @Service was found)
	 */
	private void commonProperties(
		ProcessPotentialService pat, Class<?> serviceType, boolean application, BeanManager beanManager) {

		beanManager.fireEvent(MergeServiceTypes.forEvent(pat).withTypes(serviceType).build());

		final AnnotatedTypeConfigurator<?> configurator = pat.configureAnnotatedType();
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
	}

	void afterDeploymentValidation(@Observes AfterDeploymentValidation adv) {
		if (applications.size() > 1) {
			// adv.addDeploymentProblem(
			//	new DeploymentException(
			//		"More than one javax.ws.rs.core.Application annotated types were found in the CDI bundle."));
		}
	}

}
