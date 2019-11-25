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

import java.lang.reflect.Field;

import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedFieldImpl<X> extends AnnotatedMemberImpl<X> implements AnnotatedField<X> {

	public AnnotatedFieldImpl(final AnnotatedType<X> declaringType, final Field field) {
		super(field.getGenericType(), field, declaringType, field);
	}

	@Override
	public Field getJavaMember() {
		return (Field)super.getJavaMember();
	}

}
