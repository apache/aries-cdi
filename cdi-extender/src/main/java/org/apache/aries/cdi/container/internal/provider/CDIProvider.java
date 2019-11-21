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

package org.apache.aries.cdi.container.internal.provider;

import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;

public class CDIProvider implements javax.enterprise.inject.spi.CDIProvider {

	private static class CdiExtenderCDI extends CDI<Object> {

		@Override
		public void destroy(Object instance) {
			// NOOP
		}

		@Override
		public Object get() {
			return this;
		}

		@Override
		public BeanManager getBeanManager() {
			final Map<ClassLoader, WebBeansContext> contexts = getContextMap();
			final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			return ofNullable(contexts.get(contextClassLoader))
					.map(WebBeansContext::getBeanManagerImpl)
					.orElseThrow(() -> new IllegalStateException("No WebBeansContext for classloader: " + contextClassLoader));
		}

		@Override
		public boolean isAmbiguous() {
			return false;
		}

		@Override
		public boolean isUnsatisfied() {
			return false;
		}

		@Override
		public Iterator<Object> iterator() {
			return Collections.singleton((Object)this).iterator();
		}

		@Override
		public Instance<Object> select(Annotation... qualifiers) {
			return getBeanManager().createInstance().select(qualifiers);
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			return getBeanManager().createInstance().select(subtype, qualifiers);
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			return getBeanManager().createInstance().select(subtype, qualifiers);
		}

	}

	private static Map<ClassLoader, WebBeansContext> getContextMap() {
		// ensure expected type
		DefaultSingletonService.class.cast(WebBeansFinder.getSingletonService());
		final Field singletonMap;
		try {
			singletonMap = DefaultSingletonService.class.getDeclaredField("singletonMap");
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException("Unexpected openwebbeans version", e);
		}
		if (!singletonMap.isAccessible()) {
			singletonMap.setAccessible(true);
		}
		try {
			return (Map<ClassLoader, WebBeansContext>) Map.class.cast(singletonMap.get(singletonMap));
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final CDI<Object> _cdi = new CdiExtenderCDI();

	@Override
	public CDI<Object> getCDI() {
		return _cdi;
	}

}
