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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.aries.cdi.container.internal.util.Reflection.getRawType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.decorator.Decorator;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessProducerField;
import javax.enterprise.inject.spi.ProcessProducerMethod;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.ProcessSyntheticBean;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.interceptor.Interceptor;

import org.apache.aries.cdi.container.internal.annotated.CachingAnnotated;
import org.apache.aries.cdi.extension.spi.annotation.AdaptedService;
import org.osgi.service.cdi.ServiceScope;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.ServiceInstance;

public class Annotates {

	private Annotates() {
		// no instances
	}

	private static final Predicate<Annotation> isBeanDefining = annotation ->
	{
		// sun.reflect.annotation.AnnotationParser.annotationForMap is a proxy so locally cache the type
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		return ApplicationScoped.class.isAssignableFrom(annotationType) ||
		ConversationScoped.class.isAssignableFrom(annotationType) ||
		Decorator.class.isAssignableFrom(annotationType) ||
		Dependent.class.isAssignableFrom(annotationType) ||
		Interceptor.class.isAssignableFrom(annotationType) ||
		RequestScoped.class.isAssignableFrom(annotationType) ||
		SessionScoped.class.isAssignableFrom(annotationType) ||
		Stereotype.class.isAssignableFrom(annotationType);
	};

	private static final Predicate<Annotation> isQualifier = annotation ->
	{
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		return !annotationType.equals(Qualifier.class) &&
		annotationType.isAnnotationPresent(Qualifier.class);
	};

	private static final Predicate<Annotation> isScope = annotation ->
	{
		final Class<? extends Annotation> type = annotation.annotationType();
		return type.isAnnotationPresent(Scope.class) ||
		type.isAnnotationPresent(NormalScope.class);
	};

	public static Map<String, Object> componentProperties(Annotated annotated) {
		return Maps.merge(annotated.getAnnotations());
	}

	@SuppressWarnings("unchecked")
	public static <X> Class<X> declaringClass(Object instance) {
		Class<?> declaringClass = null;

		if (instance instanceof AnnotatedMember) {
			AnnotatedMember<?> af = (AnnotatedMember<?>)instance;

			declaringClass = af.getDeclaringType().getJavaClass();
		}
		else if (instance instanceof AnnotatedParameter) {
			AnnotatedParameter<?> ap = (AnnotatedParameter<?>)instance;

			Parameter javaParameter = ap.getJavaParameter();

			Executable executable = javaParameter.getDeclaringExecutable();

			declaringClass = executable.getDeclaringClass();
		}
		else if (instance instanceof AnnotatedType) {
			AnnotatedType<?> annotatedType = (AnnotatedType<?>)instance;

			declaringClass = annotatedType.getJavaClass();
		}
		else if (instance instanceof Annotated) {
			Annotated annotated = (Annotated)instance;
			declaringClass = getRawType(annotated.getBaseType());
		}
		else if (instance instanceof ProcessManagedBean) {
			ProcessManagedBean<?> bean = (ProcessManagedBean<?>)instance;

			declaringClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (instance instanceof ProcessSessionBean) {
			ProcessSessionBean<?> bean = (ProcessSessionBean<?>)instance;

			declaringClass = bean.getAnnotatedBeanClass().getJavaClass();
		}
		else if (instance instanceof ProcessProducerMethod) {
			ProcessProducerMethod<?, ?> producer = (ProcessProducerMethod<?, ?>)instance;

			declaringClass = producer.getAnnotatedProducerMethod().getDeclaringType().getJavaClass();
		}
		else if (instance instanceof ProcessProducerField) {
			ProcessProducerField<?, ?> producer = (ProcessProducerField<?, ?>)instance;

			declaringClass = producer.getAnnotatedProducerField().getDeclaringType().getJavaClass();
		}
		else if (instance instanceof ProcessSyntheticBean) {
			ProcessSyntheticBean<?> synthetic = (ProcessSyntheticBean<?>)instance;

			declaringClass = synthetic.getBean().getBeanClass();
		}
		else if (instance instanceof ProcessBean) {
			ProcessBean<?> processBean = (ProcessBean<?>)instance;

			declaringClass = processBean.getBean().getBeanClass();
		}

		return (Class<X>)declaringClass;
	}

	public static Set<Annotation> qualifiers(Annotated annotated) {
		return collect(annotated, isQualifier);
	}

	public static Set<Annotation> collect(Annotated annotated, Predicate<Annotation> predicate) {
		CachingAnnotated cachingAnnotated = CachingAnnotated.class.isInstance(annotated) ?
				CachingAnnotated.class.cast(annotated) : null;
		List<Annotation> cached = cachingAnnotated == null ? null : cachingAnnotated.getCollectedAnnotations();
		if (cached != null) {
			return cached.stream().filter(predicate).collect(Collectors.toSet());
		}
		List<Annotation> collected = collect(annotated.getAnnotations());
		if (cachingAnnotated != null) {
			cachingAnnotated.setCollectedAnnotations(collected);
		}
		return collected.stream().filter(predicate).collect(Collectors.toSet());
	}

	private static List<Annotation> collect(Collection<Annotation> annotations) {
		return annotations.stream()
				.flatMap(it -> Stream.concat(
						Stream.of(it), Stream.of(it.annotationType().getAnnotations()))
						.filter(a -> !a.annotationType().getName().startsWith("java.lang.annotation."))
						.distinct())
				.distinct()
				.collect(toList());
	}

	public static List<Class<?>> serviceClasses(Annotated annotated) {
		List<Class<?>> serviceTypes = new ArrayList<>();

		List<java.lang.reflect.AnnotatedType> ats = new ArrayList<>();

		if (annotated instanceof AnnotatedType) {
			Class<?> annotatedClass = ((AnnotatedType<?>)annotated).getJavaClass();
			ofNullable(annotatedClass.getAnnotatedSuperclass()).ifPresent(at -> ats.add(at));
			ats.addAll(asList(annotatedClass.getAnnotatedInterfaces()));

			for (java.lang.reflect.AnnotatedType at : ats) {
				ofNullable(at.getAnnotation(Service.class)).ifPresent(
					service -> {
						if (service.value().length > 0) {
							throw new IllegalArgumentException(
								String.format(
									"@Service on type_use must not specify a value: %s",
									annotatedClass));
						}

						Type type = at.getType();

						if (!(type instanceof Class)) {
							throw new IllegalArgumentException(
								String.format(
									"@Service on type_use must only be specified on non-generic types: %s",
									annotatedClass));
						}

						serviceTypes.add((Class<?>)type);
					}
				);
			}

			ofNullable(
				annotated.getAnnotation(AdaptedService.class)
			).map(AdaptedService::value).map(Stream::of).orElseGet(Stream::empty).forEach(serviceTypes::add);

			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			if (!serviceTypes.isEmpty()) {
				throw new IllegalArgumentException(
					String.format(
						"@Service must not be applied to type and type_use: %s",
						annotated));
			}

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (annotatedClass.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(annotatedClass.getInterfaces()));
			}
			else {
				serviceTypes.add(annotatedClass);
			}
		}
		else if (annotated instanceof AnnotatedMethod) {
			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			Class<?> returnType = ((AnnotatedMethod<?>)annotated).getJavaMember().getReturnType();

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (returnType.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(returnType.getInterfaces()));
			}
			else {
				serviceTypes.add(returnType);
			}
		}
		else if (annotated instanceof AnnotatedField) {
			Service service = annotated.getAnnotation(Service.class);

			if (service == null) {
				return serviceTypes;
			}

			Class<?> fieldType = ((AnnotatedField<?>)annotated).getJavaMember().getType();

			if (service.value().length > 0) {
				serviceTypes.addAll(Arrays.asList(service.value()));
			}
			else if (fieldType.getInterfaces().length > 0) {
				serviceTypes.addAll(Arrays.asList(fieldType.getInterfaces()));
			}
			else {
				serviceTypes.add(fieldType);
			}
		}

		return serviceTypes;
	}

	public static List<String> serviceClassNames(Annotated annotated) {
		return serviceClasses(annotated).stream().map(Class::getName).sorted().collect(Collectors.toList());
	}

	public static ServiceScope serviceScope(Annotated annotated) {
		ServiceInstance serviceInstance = annotated.getAnnotation(ServiceInstance.class);

		if (serviceInstance != null) {
			return serviceInstance.value();
		}

		return ServiceScope.SINGLETON;
	}

	public static String beanName(Annotated annotated) {
		return collect(annotated.getAnnotations()).stream().filter(Named.class::isInstance).map(Named.class::cast).findFirst().map(
			named -> {
				if (named.value().isEmpty()) {
					if (annotated instanceof AnnotatedMethod) {
						AnnotatedMethod<?> annotatedMethod = (AnnotatedMethod<?>)annotated;
						String name = annotatedMethod.getJavaMember().getName();
						if (name.startsWith("get")) {
							name = name.substring(3);
						}
						else if (name.startsWith("is")) {
							name = name.substring(2);
						}
						char c[] = name.toCharArray();
						c[0] = Character.toLowerCase(c[0]);
						return new String(c);
					}
					else if (annotated instanceof AnnotatedField) {
						AnnotatedField<?> annotatedField = (AnnotatedField<?>)annotated;
						return annotatedField.getJavaMember().getName();
					}
					else {
						char c[] = Reflection.getRawType(annotated.getBaseType()).getSimpleName().toCharArray();
						c[0] = Character.toLowerCase(c[0]);
						return new String(c);
					}
				}

				return named.value();
			}
		).orElse(null);
	}

	public static Class<? extends Annotation> beanScope(Annotated annotated) {
		return beanScope(annotated, Dependent.class);
	}

	public static Class<? extends Annotation> beanScope(Annotated annotated, Class<? extends Annotation> defaultValue) {
		CachingAnnotated cachingAnnotated = CachingAnnotated.class.isInstance(annotated) ?
				CachingAnnotated.class.cast(annotated) : null;
		Class<? extends Annotation> cached = cachingAnnotated == null ? null : cachingAnnotated.getBeanScope();
		if (cached == null) {
			cached = collect(annotated.getAnnotations()).stream().filter(isScope).map(Annotation::annotationType).findFirst().orElse(null);
			if (cachingAnnotated != null) {
				cachingAnnotated.setBeanScope(cached);
			}
		}
		return cached == null ? defaultValue : cached;
	}

	public static boolean hasBeanDefiningAnnotations(AnnotatedType<?> annotatedType) {
		// only on classes, not on producers
		return !collect(annotatedType, isBeanDefining).isEmpty();
	}

}