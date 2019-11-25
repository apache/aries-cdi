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

package org.apache.aries.cdi.container.internal.container;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.aries.cdi.container.internal.annotated.AnnotatedTypeImpl;
import org.apache.aries.cdi.container.internal.model.BeansModel;
import org.apache.aries.cdi.container.internal.model.ComponentPropertiesModel;
import org.apache.aries.cdi.container.internal.model.ExtendedActivationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedComponentTemplateDTO;
import org.apache.aries.cdi.container.internal.model.ExtendedConfigurationTemplateDTO;
import org.apache.aries.cdi.container.internal.model.OSGiBean;
import org.apache.aries.cdi.container.internal.model.ReferenceModel;
import org.apache.aries.cdi.container.internal.model.ReferenceModel.Builder;
import org.apache.aries.cdi.container.internal.util.Annotates;
import org.apache.aries.cdi.container.internal.util.Reflection;
import org.osgi.service.cdi.ComponentType;
import org.osgi.service.cdi.ConfigurationPolicy;
import org.osgi.service.cdi.MaximumCardinality;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.ComponentProperties;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.FactoryComponent;
import org.osgi.service.cdi.annotations.PID;
import org.osgi.service.cdi.annotations.Reference;
import org.osgi.service.cdi.annotations.SingleComponent;
import org.osgi.service.cdi.reference.BindBeanServiceObjects;
import org.osgi.service.cdi.reference.BindService;
import org.osgi.service.cdi.reference.BindServiceReference;
import org.osgi.service.cdi.runtime.dto.template.ComponentTemplateDTO;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.lib.exceptions.Exceptions;

public class Discovery {

	private static final List<Type> BIND_TYPES = Arrays.asList(BindService.class, BindBeanServiceObjects.class, BindServiceReference.class);

	static final DocumentBuilderFactory	dbf	= DocumentBuilderFactory.newInstance();
	static final XPathFactory			xpf	= XPathFactory.newInstance();
	static final XPathExpression		trimExpression;

	static {
		try {
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
			XPath xPath = xpf.newXPath();
			trimExpression = xPath.compile("boolean(/beans/trim)");
		} catch (Throwable t) {
			throw Exceptions.duck(t);
		}
	}

	public Discovery(ContainerState containerState) {
		_containerState = containerState;
		_beansModel = _containerState.beansModel();
		_containerTemplate = _containerState.containerDTO().template.components.get(0);

		AtomicBoolean trim = new AtomicBoolean();

		_excludes = new ArrayList<>();

		_beansModel.getBeansXml().stream().map(this::readXMLResource).forEach(doc -> {
			if (!trim.get()) trim.set(checkTrim(doc));
		});

		_trim = trim.get();
	}

	public void discover() {
		_beansModel.getOSGiBeans().stream().forEach(osgiBean -> {
			osgiBean.found(true);

			AnnotatedType<?> annotatedType = new AnnotatedTypeImpl<>(osgiBean.getBeanClass());

			if (trimIt(annotatedType)) {
				return;
			}

			try {
				String beanName = Annotates.beanName(annotatedType);
				Class<? extends Annotation> beanScope = Annotates.beanScope(annotatedType);
				Map<String, Object> componentProperties = Annotates.componentProperties(annotatedType);
				ServiceScope serviceScope = Annotates.serviceScope(annotatedType);
				List<String> serviceTypes = Annotates.serviceClassNames(annotatedType);

				if (annotatedType.isAnnotationPresent(SingleComponent.class)) {
					doFactoryOrSingleComponent(osgiBean, osgiBean.getBeanClass(), annotatedType, beanName, serviceTypes, serviceScope, componentProperties, ComponentType.SINGLE);
				}
				else if (annotatedType.isAnnotationPresent(FactoryComponent.class)) {
					doFactoryOrSingleComponent(osgiBean, osgiBean.getBeanClass(), annotatedType, beanName, serviceTypes, serviceScope, componentProperties, ComponentType.FACTORY);
				}
				else if (annotatedType.isAnnotationPresent(ComponentScoped.class)) {
					_componentScoped.add(osgiBean);
				}
				else {
					discoverActivations(osgiBean, osgiBean.getBeanClass(), annotatedType, null, beanScope, serviceTypes, serviceScope, componentProperties);
				}
			}
			catch (Exception e) {
				_containerState.error(e);

				return;
			}

			annotatedType.getConstructors().stream().filter(this::isInject).flatMap(annotatedConstructor -> annotatedConstructor.getParameters().stream()).forEach(
				annotatedParameter ->
					processAnnotated(annotatedParameter, annotatedParameter.getBaseType(), Annotates.qualifiers(annotatedParameter), osgiBean)
			);

			annotatedType.getFields().stream().filter(this::isInject).forEach(
				annotatedField ->
					processAnnotated(annotatedField, annotatedField.getBaseType(), Annotates.qualifiers(annotatedField), osgiBean)
			);

			annotatedType.getFields().stream().filter(this::isProduces).forEach(
				annotatedField -> {
					Class<? extends Annotation> beanScope = Annotates.beanScope(annotatedField);
					Map<String, Object> componentProperties = Annotates.componentProperties(annotatedField);
					ServiceScope serviceScope = Annotates.serviceScope(annotatedField);
					List<String> serviceTypes = Annotates.serviceClassNames(annotatedField);

					discoverActivations(osgiBean, osgiBean.getBeanClass(), annotatedField, annotatedField, beanScope, serviceTypes, serviceScope, componentProperties);
				}
			);

			annotatedType.getMethods().stream().forEach(annotatedMethod -> {
				if (isInjectOrProduces(annotatedMethod)) {
					annotatedMethod.getParameters().stream().forEach(
						annotatedParameter -> processAnnotated(annotatedParameter, annotatedParameter.getBaseType(), Annotates.qualifiers(annotatedParameter), osgiBean)
					);

					if (isProduces(annotatedMethod)) {
						Class<? extends Annotation> beanScope = Annotates.beanScope(annotatedMethod);
						Map<String, Object> componentProperties = Annotates.componentProperties(annotatedMethod);
						ServiceScope serviceScope = Annotates.serviceScope(annotatedMethod);
						List<String> serviceTypes = Annotates.serviceClassNames(annotatedMethod);

						discoverActivations(osgiBean, osgiBean.getBeanClass(), annotatedMethod, annotatedMethod, beanScope, serviceTypes, serviceScope, componentProperties);
					}
				}
				else if (isDisposeOrObserves(annotatedMethod)) {
					annotatedMethod.getParameters().subList(1, annotatedMethod.getParameters().size()).stream().forEach(
						annotatedParameter -> processAnnotated(annotatedParameter, annotatedParameter.getBaseType(), Annotates.qualifiers(annotatedParameter), osgiBean)
					);
				}
			});
		});

		postProcessComponentScopedBeans();
	}

	boolean trimIt(AnnotatedType<?> annotatedType) {
		// See https://docs.jboss.org/cdi/spec/2.0/cdi-spec.html#trimmed_bean_archive
		if (!_trim) return false;

		if (Annotates.hasBeanDefiningAnnotations(annotatedType)) return false;

		// or any scope annotation
		if (Annotates.beanScope(annotatedType, null) != null) return false;

		return true;
	}

	<X> boolean isInject(AnnotatedMember<X> annotatedMember) {
		return (annotatedMember.isAnnotationPresent(Inject.class) && !annotatedMember.isStatic());
	}

	<X> boolean isInjectOrProduces(AnnotatedMember<X> annotatedMember) {
		return (annotatedMember.isAnnotationPresent(Inject.class) && !annotatedMember.isStatic()) ||
			annotatedMember.isAnnotationPresent(Produces.class);
	}

	<X> boolean isProduces(AnnotatedMember<X> annotatedMember) {
		return annotatedMember.isAnnotationPresent(Produces.class);
	}

	<X> boolean isDisposeOrObserves(AnnotatedMethod<X> annotatedMethod) {
		return !annotatedMethod.getParameters().isEmpty() && (annotatedMethod.getParameters().get(0).isAnnotationPresent(Disposes.class) ||
			annotatedMethod.getParameters().get(0).isAnnotationPresent(Observes.class) ||
			annotatedMethod.getParameters().get(0).isAnnotationPresent(ObservesAsync.class));
	}

	<X> void processAnnotated(Annotated annotated, Type injectionPointType, Set<Annotation> qualifiers, OSGiBean osgiBean) {
		if (BIND_TYPES.contains(Reflection.getRawType(annotated.getBaseType()))) {
			Builder builder = new ReferenceModel.Builder(annotated);

			try {
				ReferenceModel referenceModel = builder.type(injectionPointType).build();

				osgiBean.addReference(referenceModel.toDTO());
			}
			catch (Exception e) {
				_containerState.error(e);
			}
		}
		else {
			Reference reference = annotated.getAnnotation(Reference.class);
			ComponentProperties componentProperties = annotated.getAnnotation(ComponentProperties.class);

			if (reference != null) {
				doReference(osgiBean, annotated, injectionPointType, reference, componentProperties);
			}
			else if (componentProperties != null) {
				doComponentProperties(osgiBean, injectionPointType, qualifiers);
			}
		}
	}

	void postProcessComponentScopedBeans() {
		_containerState.containerDTO().template.components.stream().filter(
			template -> template.type != ComponentType.CONTAINER
		).map(
			template -> (ExtendedComponentTemplateDTO)template
		).forEach(
			template -> {

				_componentScoped.forEach(
					osgiBean -> {
						if (osgiBean.getComponent() == null) {
							osgiBean.setComponent(_containerState, template);
						}

						String className = osgiBean.getBeanClass().getName();
						if (!template.beans.contains(className)) {
							template.beans.add(className);
						}
					}
				);
			}
		);
	}

	void doComponentProperties(OSGiBean osgiBean, Type injectionPointType, Set<Annotation> qualifiers) {
		try {
			ComponentPropertiesModel configurationModel = new ComponentPropertiesModel.Builder(
				injectionPointType
			).declaringClass(
				osgiBean.getBeanClass()
			).qualifiers(
				qualifiers
			).build();

			osgiBean.addConfiguration(_containerState, configurationModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	void discoverActivations(OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, AnnotatedMember<?> producer, Class<? extends Annotation> scope, List<String> serviceTypeNames, ServiceScope serviceScope, Map<String, Object> componentProperties) {
		String className = declaringClass.getName();

		if (!_containerTemplate.beans.contains(className)) {
			_containerTemplate.beans.add(className);
		}

		if (!serviceTypeNames.isEmpty()) {
			if (!scope.equals(ApplicationScoped.class) &&
				!scope.equals(Dependent.class)) {

				_containerState.error(
					new IllegalStateException(
						String.format(
							"@Service can only be used on @ApplicationScoped, @Dependent, @SingleComponent, and @FactoryComponent: %s",
							osgiBean.getBeanClass())));

				return;
			}

			ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
			activationTemplate.cdiScope = scope;
			activationTemplate.declaringClass = declaringClass;
			activationTemplate.producer = producer;
			activationTemplate.properties = componentProperties;
			activationTemplate.scope = serviceScope;
			activationTemplate.serviceClasses = serviceTypeNames;

			_containerTemplate.activations.add(activationTemplate);
		}

		osgiBean.setComponent(_containerState, _containerTemplate);
	}

	void doFactoryOrSingleComponent(
			OSGiBean osgiBean, Class<?> declaringClass, Annotated annotated, String beanName,
			List<String> serviceTypes, ServiceScope serviceScope,
			Map<String, Object> componentProperties, ComponentType componentType) {

		Set<Annotation> qualifiers = Annotates.qualifiers(annotated);
		qualifiers.removeIf(Named.class::isInstance);

		ExtendedComponentTemplateDTO componentTemplate = new ExtendedComponentTemplateDTO();
		componentTemplate.activations = new CopyOnWriteArrayList<>();

		ExtendedActivationTemplateDTO activationTemplate = new ExtendedActivationTemplateDTO();
		activationTemplate.declaringClass = declaringClass;
		activationTemplate.properties = Collections.emptyMap();
		activationTemplate.scope = serviceScope;
		activationTemplate.serviceClasses = serviceTypes;

		componentTemplate.activations.add(activationTemplate);

		componentTemplate.beanClass = declaringClass;
		componentTemplate.qualifiers = qualifiers;
		componentTemplate.beans = new CopyOnWriteArrayList<>();
		componentTemplate.configurations = new CopyOnWriteArrayList<>();
		componentTemplate.name = beanName;
		componentTemplate.properties = componentProperties;
		componentTemplate.references = new CopyOnWriteArrayList<>();
		componentTemplate.type = componentType;

		annotated.getAnnotations(PID.class).stream().forEach(
			PID -> {
				String pid = PID.value();

				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.declaringClass = declaringClass;
				configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
				configurationTemplate.pid = Optional.of(pid).map(
					s -> (s.equals("$") || s.equals("")) ? componentTemplate.name : s
				).orElse(componentTemplate.name);

				if (componentType == ComponentType.SINGLE) {
					configurationTemplate.pid = (pid.equals("$") || pid.equals("")) ? componentTemplate.name : pid;
				}

				configurationTemplate.policy = PID.policy();

				componentTemplate.configurations.add(configurationTemplate);
			}
		);

		if (componentType == ComponentType.SINGLE) {
			if (componentTemplate.configurations.isEmpty()) {
				ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

				configurationTemplate.declaringClass = declaringClass;
				configurationTemplate.maximumCardinality = MaximumCardinality.ONE;
				configurationTemplate.pid = componentTemplate.name;
				configurationTemplate.policy = ConfigurationPolicy.OPTIONAL;

				componentTemplate.configurations.add(configurationTemplate);
			}
		}
		else {
			ExtendedConfigurationTemplateDTO configurationTemplate = new ExtendedConfigurationTemplateDTO();

			configurationTemplate.declaringClass = declaringClass;
			configurationTemplate.maximumCardinality = MaximumCardinality.MANY;
			configurationTemplate.pid = Optional.ofNullable(
				annotated.getAnnotation(FactoryComponent.class)
			).map(FactoryComponent::value).map(
				v -> (v.equals("$") || v.equals("")) ? componentTemplate.name : v
			).orElse(componentTemplate.name);
			configurationTemplate.policy = ConfigurationPolicy.REQUIRED;

			componentTemplate.configurations.add(configurationTemplate);
		}

		componentTemplate.beans.add(declaringClass.getName());

		_containerState.containerDTO().template.components.add(componentTemplate);

		osgiBean.setComponent(_containerState, componentTemplate);
	}

	void doReference(OSGiBean osgiBean, Annotated annotated, Type injectionPointType, Reference reference, ComponentProperties componentProperties) {
		if (componentProperties != null) {
			_containerState.error(
				new IllegalArgumentException(
					String.format(
						"Cannot use @Reference and @Configuration on the same injection point {}",
						injectionPointType))
			);

			return;
		}

		Builder builder = null;

		if (annotated instanceof AnnotatedParameter) {
			builder = new ReferenceModel.Builder(annotated);
		}
		else {
			builder = new ReferenceModel.Builder(annotated);
		}

		try {
			ReferenceModel referenceModel = builder.type(injectionPointType).build();

			osgiBean.addReference(referenceModel.toDTO());
		}
		catch (Exception e) {
			_containerState.error(e);
		}
	}

	boolean checkTrim(Document document) {
		try {
			return Boolean.class.cast(trimExpression.evaluate(document, XPathConstants.BOOLEAN));
		} catch (XPathExpressionException e) {
			throw Exceptions.duck(e);
		}
	}

	Document readXMLResource(URL resource) {
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			try (InputStream is = resource.openStream()) {
				return db.parse(is);
			} catch (Throwable t) {
				return db.newDocument();
			}
		}
		catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private static final String NS = "http://xmlns.jcp.org/xml/ns/javaee";

	private final BeansModel _beansModel;
	private final Set<OSGiBean> _componentScoped = new HashSet<>();
	private final ComponentTemplateDTO _containerTemplate;
	private final ContainerState _containerState;
	private final boolean _trim;

}
