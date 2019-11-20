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
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedMemberImpl<X> extends AnnotatedImpl<X> implements AnnotatedMember<X> {

	private final Member _member;
	private final AnnotatedType<X> _declaringType;

	public AnnotatedMemberImpl(final Type baseType, final AnnotatedElement annotatedElement, final AnnotatedType<X> declaringType, final Member member) {
		super(baseType, annotatedElement);

		_declaringType = declaringType;
		_member = member;
	}

	@Override
	public Member getJavaMember() {
		return _member;
	}

	@Override
	public boolean isStatic() {
		return Modifier.isStatic(_member.getModifiers());
	}

	@Override
	public AnnotatedType<X> getDeclaringType() {
		return _declaringType;
	}

}
