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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedCallableImpl<X> extends AnnotatedMemberImpl<X> implements AnnotatedCallable<X> {

	private final List<AnnotatedParameter<X>> _parameters;

	public AnnotatedCallableImpl(final Type baseType, final AnnotatedElement annotatedElement, final AnnotatedType<X> declaringType, final Executable executable) {
		super(baseType, annotatedElement, declaringType, executable);

		_parameters = IntStream.range(0, executable.getParameterCount())
			.mapToObj(i -> new AnnotatedParameterImpl<X>(executable.getAnnotatedParameterTypes()[i].getType(), executable.getParameterAnnotations()[i], this, i))
			.collect(Collectors.toList());
	}

	@Override
	public List<AnnotatedParameter<X>> getParameters() {
		return _parameters;
	}

}
