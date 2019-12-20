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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.apache.aries.cdi.extension.spi.annotation.AdaptedService;
import org.osgi.service.cdi.annotations.Service;

public class Adapted {

	private Adapted() {
		//
	}

	/**
	 * Adapt the annotated type associated with the configurator with additional
	 * types in order to publish OSGi services with those types.
	 * <p>
	 * The annotated type will not be adapted if it is already annotated with {@link Service @Service}.
	 *
	 * @param <X> the type of the annotated type
	 * @param configurator the configurator
	 * @param serviceTypes the additional service types
	 * @return true if the annotated type was adapted
	 */
	public static <X> boolean withServiceTypes(AnnotatedTypeConfigurator<X> configurator, Collection<Class<?>> serviceTypes) {
		return withServiceTypes(configurator, serviceTypes, false);
	}

	/**
	 * Adapt the annotated type associated with the configurator with additional
	 * types in order to publish OSGi services with those types.
	 * <p>
	 * The annotated type will not be adapted if it is already annotated with {@link Service @Service}.
	 *
	 * @param <X> the type of the annotated type
	 * @param configurator the configurator
	 * @param serviceTypes the additional service types
	 * @return true if the annotated type was adapted
	 */
	public static <X> boolean withServiceTypes(AnnotatedTypeConfigurator<X> configurator, Class<?>... serviceTypes) {
		return withServiceTypes(configurator, Arrays.asList(serviceTypes), false);
	}

	/**
	 * Adapt the annotated type associated with the configurator with additional
	 * types in order to publish OSGi services with those types.
	 * <p>
	 * The annotated type will not be adapted if it is already annotated with {@link Service @Service}.
	 *
	 * @param <X> the type of the annotated type
	 * @param configurator the configurator
	 * @param serviceTypes the additional service types
	 * @param replace if true do not merge with previous types
	 * @return true if the annotated type was adapted
	 */
	public static <X> boolean withServiceTypes(AnnotatedTypeConfigurator<X> configurator, Collection<Class<?>> serviceTypes, boolean replace) {
		if (configurator.getAnnotated().isAnnotationPresent(Service.class)) {
			return false;
		}

		Set<Class<?>> servicesSet = new HashSet<>(serviceTypes);

		AdaptedService adaptedService = configurator.getAnnotated().getAnnotation(AdaptedService.class);

		if (adaptedService != null) {
			configurator.remove(adaptedService::equals);
			if (!replace) {
				servicesSet.addAll(Arrays.asList(adaptedService.value()));
			}
		}

		configurator.add(
			AdaptedService.Literal.of(servicesSet.toArray(new Class<?>[0])));

		return true;
	}

}
