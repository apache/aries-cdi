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

import java.util.stream.Stream;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extension.spi.annotation.AdaptedService;

public class ServiceAdapterExtension implements Extension {
	private boolean started;

	<T> void onMergeServiceTypes(@Observes final MergeServiceTypes<T> mergeServiceTypes) {
		if (started) {
			throw new IllegalStateException("Container already started");
		}

		final AdaptedService adaptedService = mergeServiceTypes.getProcessAnnotatedType()
				.getAnnotatedType().getAnnotation(AdaptedService.class);
		final AnnotatedTypeConfigurator<T> configurator = mergeServiceTypes.getProcessAnnotatedType().configureAnnotatedType();

		final Class<?>[] services;
		if (adaptedService != null) {
			configurator.remove(a -> a.annotationType() == AdaptedService.class);
			services = Stream.concat(
					Stream.of(mergeServiceTypes.getTypes()),
					Stream.of(adaptedService.value()))
					.toArray(Class[]::new);
		} else {
			services = mergeServiceTypes.getTypes();
		}
		configurator.add(AdaptedService.Literal.of(services));
	}

	void started(@Observes final AfterDeploymentValidation afterDeploymentValidation) {
		started = true;
	}
}
