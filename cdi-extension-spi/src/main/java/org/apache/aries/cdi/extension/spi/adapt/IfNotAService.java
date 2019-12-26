/*
 * Copyright (c) OSGi Alliance (2018). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.extension.spi.adapt;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.apache.aries.cdi.extension.spi.annotation.AdaptedService;
import org.osgi.service.cdi.annotations.Service;

public class IfNotAService {

	private IfNotAService() {
		//
	}

	public static <X, R> R run(final ProcessAnnotatedType<X> pat,
							   final Function<ServiceTypeManager<X>, R> task,
							   final Supplier<R> defaultValue) {
		if (!pat.getAnnotatedType().isAnnotationPresent(Service.class)) {
			return task.apply(serviceTypes -> {
				final AdaptedService adaptedService = pat.getAnnotatedType().getAnnotation(AdaptedService.class);
				final AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();
				final Class<?>[] services;
				if (adaptedService != null) {
					configurator.remove(a -> a.annotationType() == AdaptedService.class);
					services = Stream.concat(
							Stream.of(serviceTypes),
							Stream.of(adaptedService.value()))
							.toArray(Class[]::new);
				} else {
					services = serviceTypes;
				}
				configurator.add(AdaptedService.Literal.of(services));
				return configurator;
			});
		}
		return defaultValue.get();
	}

	public static <X> void run(final ProcessAnnotatedType<X> pat,
							   final Consumer<ServiceTypeManager<X>> task) {
		run(pat, m -> {
			task.accept(m);
			return null;
		}, () -> null);
	}

	public interface ServiceTypeManager<X> {
		AnnotatedTypeConfigurator<X> mergeWith(Class<?>... serviceTypes);
	}
}
