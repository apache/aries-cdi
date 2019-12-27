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
package org.apache.aries.cdi.extension.spi.adapt;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * If fired (using the {@link javax.enterprise.inject.spi.BeanManager})
 * during a {@link javax.enterprise.inject.spi.BeforeBeanDiscovery} event,
 * it will enable to filter {@link ProcessAnnotatedType} event and convert them to {@link ProcessPotentialService}
 * event with {@link FiltersOn} support.
 */
public class RegisterExtension {
	private final Extension extension;
	private final Collection<ObserverBuilder> builders = new ArrayList<>();

	public RegisterExtension(final Extension extension) {
		this.extension = extension;
	}

	public Extension getExtension() {
		return extension;
	}

	public Collection<ObserverBuilder> getBuilders() {
		return builders;
	}

	public ObserverBuilder registerObserver() {
		return new ObserverBuilder(this);
	}

	public static class ObserverBuilder {
		private final RegisterExtension parent;

		// defaults aligned on the annotation to have a single impl
		private List<Class<?>> types = new ArrayList<>(singletonList(FiltersOn.class));
		private List<Class<? extends Annotation>> annotations = new ArrayList<>(singletonList(FiltersOn.class));
		private BiConsumer<BeanManager, ProcessPotentialService> consumer;

		private ObserverBuilder(final RegisterExtension parent) {
			this.parent = parent;
		}

		public ObserverBuilder forTypes(final Class<?>... types) {
			if (types.length > 0) {
				this.types.remove(FiltersOn.class);
			}
			this.types.addAll(asList(types));
			return this;
		}

		public ObserverBuilder forAnnotations(final Class<? extends Annotation>... annotations) {
			if (annotations.length > 0) {
				this.annotations.remove(FiltersOn.class);
			}
			this.annotations.addAll(asList(annotations));
			return this;
		}

		public ObserverBuilder execute(final BiConsumer<BeanManager, ProcessPotentialService> consumer) {
			this.consumer = consumer;
			return this;
		}

		public Collection<Class<?>> getTypes() {
			return unmodifiableList(types);
		}

		public Collection<Class<?>> getAnnotations() {
			return unmodifiableList(annotations);
		}

		public BiConsumer<BeanManager, ProcessPotentialService> getConsumer() {
			return consumer;
		}

		public RegisterExtension done() {
			if (consumer == null) {
				throw new IllegalArgumentException("No consumer registered on observer builder");
			}
			parent.builders.add(this);
			return parent;
		}
	}
}
