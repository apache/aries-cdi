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

package org.apache.aries.cdi.extension.jaxrs;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.spi.Annotated;
import javax.inject.Scope;

public class Util {

	private static final Predicate<Annotation> isScope = annotation ->
		annotation.annotationType().isAnnotationPresent(Scope.class) ||
		annotation.annotationType().isAnnotationPresent(NormalScope.class);

	public static Class<? extends Annotation> beanScope(Annotated annotated, Class<? extends Annotation> defaultValue) {
		Class<? extends Annotation> scope = collect(annotated.getAnnotations()).stream().filter(isScope).map(Annotation::annotationType).findFirst().orElse(null);

		return (scope == null) ? defaultValue : scope;
	}

	private static List<Annotation> collect(Collection<Annotation> annotations) {
		List<Annotation> list = new ArrayList<>();
		for (Annotation a1 : annotations) {
			if (a1.annotationType().getName().startsWith("java.lang.annotation.")) continue;
			list.add(a1);
		}
		list.addAll(inherit(list));
		return list;
	}

	private static List<Annotation> inherit(Collection<Annotation> annotations) {
		List<Annotation> list = new ArrayList<>();
		for (Annotation a1 : annotations) {
			for (Annotation a2 : collect(Arrays.asList(a1.annotationType().getAnnotations()))) {
				if (list.contains(a2)) continue;
				list.add(a2);
			}
		}
		return list;
	}

}
