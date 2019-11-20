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

package org.apache.aries.cdi.container.internal.annotated;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

public class AnnotatedParameterImpl<X> extends AnnotatedImpl<X> implements AnnotatedParameter<X> {

	private final AnnotatedCallable<X> _annotatedCallable;
	private final int _position;

	public AnnotatedParameterImpl(final Type baseType, final Annotation[] parameterAnnotations, final AnnotatedCallable<X> annotatedCallable, final int position) {
		super(baseType, newAnnotatedElement(parameterAnnotations));

		_annotatedCallable = annotatedCallable;
		_position = position;
	}

	@Override
	public int getPosition() {
		return _position;
	}

	@Override
	public AnnotatedCallable<X> getDeclaringCallable() {
		return _annotatedCallable;
	}

	private static AnnotatedElement newAnnotatedElement(final Annotation[] parameterAnnotations) {
		return new AnnotatedElement() {

			@Override
			public Annotation[] getDeclaredAnnotations() {
				return parameterAnnotations;
			}

			@Override
			public Annotation[] getAnnotations() {
				return parameterAnnotations;
			}

			@Override
			public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
				return Arrays.stream(parameterAnnotations).filter(annotationType::isInstance).map(annotationType::cast).findFirst().orElse(null);
			}

		};
	}

}
