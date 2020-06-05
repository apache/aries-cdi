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
package org.apache.aries.cdi.extension.spi.adapt;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

/**
 * Close of {@link javax.enterprise.inject.spi.WithAnnotations} but for {@link ProcessPotentialService} event.
 * Enables to filter the observed annotated types.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface FiltersOn {

	/**
	 * @return the types to filter the event on using {@link Class#isAssignableFrom(Class)}.
	 */
	Class<?>[] types() default {FiltersOn.class};

	@SuppressWarnings("serial")
	class Literal extends AnnotationLiteral<FiltersOn> implements FiltersOn {
		public static final Literal INSTANCE = new Literal();

		@SuppressWarnings("unchecked")
		private final Class<? extends Annotation>[] defaultArray = new Class[0];

		@Override
		public Class<? extends Annotation>[] annotations() {
			return defaultArray;
		}

		@Override
		public Class<?>[] types() {
			return defaultArray;
		}
	}

	/**
	 * @return the annotations the {@link javax.enterprise.inject.spi.ProcessAnnotatedType} should get filtered for.
	 */
	Class<? extends Annotation>[] annotations() default {FiltersOn.class};

}

