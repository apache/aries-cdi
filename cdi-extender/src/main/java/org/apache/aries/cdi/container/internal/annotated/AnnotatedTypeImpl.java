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

import static java.util.stream.Collectors.*;
import static org.apache.aries.cdi.container.internal.util.Reflection.*;

import java.lang.reflect.Constructor;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedTypeImpl<X> extends AnnotatedImpl<X> implements AnnotatedType<X> {

	private final Class<X> _declaringClass;
	private final Set<AnnotatedConstructor<X>> _constructors;
	private final Set<AnnotatedField<? super X>> _fields;
	private final Set<AnnotatedMethod<? super X>> _methods;

	@SuppressWarnings("unchecked")
	public AnnotatedTypeImpl(final Class<X> declaringClass) {
		super(declaringClass, declaringClass);

		_declaringClass = declaringClass;

		_constructors = allConstructors(_declaringClass).map(c -> new AnnotatedConstructorImpl<>(this, (Constructor<X>)c)).collect(toSet());
		_fields = allFields(_declaringClass).map(f -> new AnnotatedFieldImpl<>(this, f)).collect(toSet());
		_methods = allMethods(_declaringClass).map(m -> new AnnotatedMethodImpl<>(this, m)).collect(toSet());
	}

	@Override
	public Class<X> getJavaClass() {
		return _declaringClass;
	}

	@Override
	public Set<AnnotatedConstructor<X>> getConstructors() {
		return _constructors;
	}

	@Override
	public Set<AnnotatedMethod<? super X>> getMethods() {
		return _methods;
	}

	@Override
	public Set<AnnotatedField<? super X>> getFields() {
		return _fields;
	}

}
