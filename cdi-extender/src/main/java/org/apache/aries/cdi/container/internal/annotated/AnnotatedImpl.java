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

import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

import org.apache.aries.cdi.container.internal.util.Reflection;

public class AnnotatedImpl<X> implements Annotated {

	private final Type _baseType;
	private final AnnotatedElement _annotatedElement;
	private final Set<Type> _typeClosure;

	public AnnotatedImpl(final Type baseType, final AnnotatedElement annotatedElement) {
		_baseType = baseType;
		_annotatedElement = annotatedElement;
		_typeClosure = Reflection.getTypes(_baseType);
	}

	@Override
	public Type getBaseType() {
		return _baseType;
	}

	@Override
	public Set<Type> getTypeClosure() {
		return _typeClosure;
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return _annotatedElement.getAnnotation(annotationType);
	}

	@Override
	public <T extends Annotation> Set<T> getAnnotations(Class<T> annotationType) {
		return Arrays.stream(_annotatedElement.getAnnotationsByType(annotationType)).collect(toSet());
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return Arrays.stream(_annotatedElement.getAnnotations()).collect(toSet());
	}

	@Override
	public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
		return _annotatedElement.isAnnotationPresent(annotationType);
	}

	@Override
	public String toString() {
		return _baseType.getTypeName();
	}

}
