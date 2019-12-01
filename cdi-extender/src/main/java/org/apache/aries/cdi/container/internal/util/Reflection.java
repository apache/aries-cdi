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

package org.apache.aries.cdi.container.internal.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class Reflection {

	private Reflection() {
		// no instances
	}

	public static Stream<Constructor<?>> allConstructors(Class<?> declaringClass) {
		Set<Constructor<?>> allconstructors = new HashSet<>();
		allconstructors.addAll(Arrays.asList(declaringClass.getConstructors()));
		allconstructors.addAll(Arrays.asList(declaringClass.getDeclaredConstructors()));
		return allconstructors.stream().distinct();
	}

	public static Stream<Field> allFields(Class<?> declaringClass) {
		Set<Field> allfields = new HashSet<>();
		allfields.addAll(Arrays.asList(declaringClass.getFields()));
		allfields.addAll(Arrays.asList(declaringClass.getDeclaredFields()));
		return allfields.stream().distinct();
	}

	public static Stream<Method> allMethods(Class<?> declaringClass) {
		Set<Method> allmethods = new HashSet<>();
		allmethods.addAll(Arrays.asList(declaringClass.getMethods()));
		allmethods.addAll(Arrays.asList(declaringClass.getDeclaredMethods()));
		return allmethods.stream().distinct();
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj) {
		return (T) obj;
	}

	public static Set<Type> getTypes(Type type) {
		Set<Type> types = new HashSet<>();
		types.add(type);
		Class<?> rawType = getRawType(type);
		if (rawType != null) {
			for (Type iface : rawType.getGenericInterfaces()) {
				types.addAll(getTypes(iface));
			}
			types.addAll(getTypes(rawType.getGenericSuperclass()));
		}
		types.remove(java.io.Serializable.class);
		return types;
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getRawType(Type type) {
		if (type instanceof Class<?>) {
			return (Class<T>) type;
		}
		if (type instanceof ParameterizedType) {
			if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
				return (Class<T>) ((ParameterizedType) type).getRawType();
			}
		}
		if (type instanceof TypeVariable<?>) {
			TypeVariable<?> variable = (TypeVariable<?>) type;
			Type[] bounds = variable.getBounds();
			return getBound(bounds);
		}
		if (type instanceof WildcardType) {
			WildcardType wildcard = (WildcardType) type;
			return getBound(wildcard.getUpperBounds());
		}
		if (type instanceof GenericArrayType) {
			GenericArrayType genericArrayType = (GenericArrayType) type;
			Class<?> rawType = getRawType(genericArrayType.getGenericComponentType());
			if (rawType != null) {
				return (Class<T>) Array.newInstance(rawType, 0).getClass();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static <T> Class<T> getBound(Type[] bounds) {
		if (bounds.length == 0) {
			return (Class<T>) Object.class;
		} else {
			return getRawType(bounds[0]);
		}
	}

}
