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

package org.apache.aries.cdi.extension.spi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

/**
 * Annotation used to hold the service types when an extension is adapting
 * annotated types for publication as OSGi CDI services.
 * <p>
 * The types in question should be those already known to the CDI bundle.
 * <p>
 * Services to be published from annotated types provided by the extension
 * should be created manually.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdaptedService {

	/**
	 * Support inline instantiation of the {@link AdaptedService} annotation.
	 */
	public static final class Literal extends AnnotationLiteral<AdaptedService>
			implements AdaptedService {

		private static final long serialVersionUID = 1L;

		/**
		 * @param interfaces
		 * @return instance of {@link AdaptedService}
		 */
		public static final Literal of(Class<?>[] interfaces) {
			return new Literal(interfaces);
		}

		private Literal(Class<?>[] interfaces) {
			_interfaces = interfaces;
		}

		@Override
		public Class<?>[] value() {
			return _interfaces;
		}

		private final Class<?>[] _interfaces;
	}

	/**
	 * Override the interfaces under which this service is published.
	 *
	 * @return the service types
	 */
	Class<?>[] value() default {};

}
