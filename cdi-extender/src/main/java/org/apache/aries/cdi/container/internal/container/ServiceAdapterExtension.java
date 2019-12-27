/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.container.internal.container;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.apache.aries.cdi.extension.spi.adapt.FiltersOn;
import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extension.spi.adapt.ProcessPotentialService;
import org.apache.aries.cdi.extension.spi.adapt.RegisterExtension;
import org.apache.aries.cdi.extension.spi.annotation.AdaptedService;
import org.osgi.service.cdi.annotations.Service;

public class ServiceAdapterExtension implements Extension {
    private static final Predicate<AnnotatedType<?>> ANNOTATED_TYPE_TRUE_PREDICATE = at -> true;

    private boolean started;

    private Collection<Entry<Predicate<AnnotatedType<?>>, BiConsumer<BeanManager, ProcessAnnotatedType<?>>>> forwardingObservers = new ArrayList<>();

    void capturePotentialServiceObservers(@Observes final RegisterExtension registerExtension) {
        if (started) {
            throw new IllegalStateException("Container already started");
        }

        // declarative mode
        forwardingObservers.addAll(forMethods(registerExtension.getExtension().getClass())
                .map(method -> new SimpleImmutableEntry<>(registerExtension.getExtension(), method))
                .filter(method -> Stream.of(method.getValue().getParameters())
                        .anyMatch(p -> p.isAnnotationPresent(Observes.class) && p.getType() == ProcessPotentialService.class))
                .map(e -> new SimpleImmutableEntry<>(toPredicate(e.getValue()), toConsumer(e.getValue(), e.getKey())))
                .collect(toList()));

        // functional mode
        forwardingObservers.addAll(registerExtension.getBuilders().stream()
                .map(builder -> new SimpleImmutableEntry<>(
                        toPredicate(builder.getAnnotations(), builder.getTypes()),
                        (BiConsumer<BeanManager, ProcessAnnotatedType<?>>) (bm, pat) ->
                                builder.getConsumer().accept(bm, new ProcessPotentialService(pat))))
                .collect(toList()));
    }

    <T> void forwardToObservers(@Observes final ProcessAnnotatedType<T> pat, final BeanManager beanManager) {
        AnnotatedType<T> annotatedType;
        if (!forwardingObservers.isEmpty() && !(annotatedType = pat.getAnnotatedType()).isAnnotationPresent(Service.class)) {
            // simulate that but avoid a tons of events for all possible combinations of qualifiers which would be too slow
            // beanManager.fireEvent(new ProcessPotentialService(pat), createAllPotentialQualifiers(pat));

            forwardingObservers.stream()
                    .filter(predicateWithObserver -> predicateWithObserver.getKey().test(annotatedType))
                    .map(Entry::getValue)
                    .forEach(observer -> observer.accept(beanManager, pat));
        }
    }

    void mergeAdaptedServiceTypes(@Observes final MergeServiceTypes mergeServiceTypes) {
        if (started) {
            throw new IllegalStateException("Container already started");
        }

        final AdaptedService adaptedService = mergeServiceTypes.getProcessAnnotatedType()
                .getAnnotatedType().getAnnotation(AdaptedService.class);
        final AnnotatedTypeConfigurator<?> configurator = mergeServiceTypes.getProcessAnnotatedType().configureAnnotatedType();

        final Class<?>[] services;
        if (adaptedService != null) {
            configurator.remove(a -> a.annotationType() == AdaptedService.class);
            services = Stream.concat(
                    Stream.of(mergeServiceTypes.getTypes()),
                    Stream.of(adaptedService.value()))
                    .distinct()
                    .toArray(Class[]::new);
        } else {
            services = mergeServiceTypes.getTypes();
        }
        configurator.add(AdaptedService.Literal.of(services));
    }

    void setStarted(@Observes final AfterDeploymentValidation afterDeploymentValidation) {
        started = true;
    }

    // using reflection since we are too early in CDI lifecycle to use CDI model
    private Predicate<AnnotatedType<?>> toPredicate(final Method method) {
        return Stream.of(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(Observes.class))
                .findFirst()
                .map(parameter -> parameter.getAnnotation(FiltersOn.class))
                .map(filters -> toPredicate(asList(filters.annotations()), asList(filters.types())))
                .orElse(ANNOTATED_TYPE_TRUE_PREDICATE);
    }

    private Predicate<AnnotatedType<?>> toPredicate(final Collection<Class<?>> annotations, final Collection<Class<?>> types) {
        return filterWithAnnotations(annotations).and(filterWithTypes(types));
    }

    private BiConsumer<BeanManager, ProcessAnnotatedType<?>> toConsumer(final Method method, final Extension instance) {
        final BiFunction<BeanManager, ProcessAnnotatedType<?>, Object>[] argsFactory = Stream.of(method.getParameters())
                .map(parameter -> lookupMethod(method, parameter))
                .toArray(BiFunction[]::new);
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return (bm, pat) -> {
            try {
                method.invoke(
                        instance,
                        Stream.of(argsFactory).map(fn -> fn.apply(bm, pat)).toArray(Object[]::new));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            }
        };
    }

    private BiFunction<BeanManager, ProcessAnnotatedType<?>, Object> lookupMethod(
            final Method method, final Parameter parameter) {
        if (BeanManager.class == parameter.getType()) {
            return (bm, pat) -> bm;
        }
        if (ProcessPotentialService.class == parameter.getType()) {
            return (bm, pat) -> new ProcessPotentialService(pat);
        }
        throw new IllegalArgumentException(
                "Unsupported type: " + parameter.getType() + " on " + method);
    }

    private Predicate<AnnotatedType<?>> filterWithTypes(final Collection<Class<?>> filterOnTypes) {
        return Optional.of(filterOnTypes)
                .filter(this::shouldNotIgnore)
                .map(types -> (Predicate<AnnotatedType<?>>) annotatedType -> types.stream()
                        .anyMatch(t -> annotatedType.getJavaClass().isAssignableFrom(t)))
                .orElse(ANNOTATED_TYPE_TRUE_PREDICATE);
    }

    private Predicate<AnnotatedType<?>> filterWithAnnotations(final Collection<Class<?>> filterOnAnnotations) {
        return Optional.of(filterOnAnnotations)
                .filter(this::shouldNotIgnore)
                .map(withAnnotations -> (Predicate<AnnotatedType<?>>) annotatedType -> Stream.of(
                        Stream.of(annotatedType.getAnnotations()), // class
                        annotatedType.getFields().stream().map(Annotated::getAnnotations), // fields
                        Stream.concat( // (constructors + methods) x (self + parameters)
                                annotatedType.getMethods().stream(),
                                annotatedType.getConstructors().stream())
                                .flatMap(m -> Stream.concat(
                                        Stream.of(m.getAnnotations()),
                                        m.getParameters().stream().map(Annotated::getAnnotations))))
                        .flatMap(identity())
                        .anyMatch(annotations -> annotations.stream().anyMatch(annotation ->
                                withAnnotations.stream().anyMatch(withAnnotation -> Stream.concat(
                                        Stream.of(annotation.annotationType()),
                                        Stream.of(annotation.annotationType().getAnnotations()).map(Annotation::annotationType))
                                        .anyMatch(withAnnotation::isAssignableFrom)))))
                .orElse(ANNOTATED_TYPE_TRUE_PREDICATE);
    }

    private boolean shouldNotIgnore(final Collection<Class<?>> annotations) {
        return annotations.size() != 1 || FiltersOn.class != annotations.iterator().next();
    }

    private Stream<Method> forMethods(final Class<?> clazz) {
        return clazz == Object.class || clazz == null ?
                Stream.empty() :
                Stream.concat(Stream.of(clazz.getDeclaredMethods()), forMethods(clazz.getSuperclass()));
    }
}
