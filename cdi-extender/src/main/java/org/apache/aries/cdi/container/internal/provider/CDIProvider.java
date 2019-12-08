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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import org.apache.aries.cdi.container.internal.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

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
			Bundle bundle = ofNullable(
				Thread.currentThread().getContextClassLoader()
			).map(BundleReference.class::cast).map(BundleReference::getBundle).orElseThrow(
				() -> new IllegalStateException(
					"No Bundle found for Thread.ContextClassLoader " + Thread.currentThread())
			);

			return ofNullable(Activator.get(bundle)).orElseThrow(
				() -> new IllegalStateException("No BeanManager for bundle " + bundle)
			);
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
			return createInstance().select(qualifiers);
		}

		@Override
		public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
			return createInstance().select(subtype, qualifiers);
		}

		@Override
		public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
			return createInstance().select(subtype, qualifiers);
		}

		private Instance<Object> createInstance() {
			BeanManager beanManager = getBeanManager();
			Set<Bean<?>> beans = beanManager.getBeans(Instance.class);
			@SuppressWarnings("unchecked")
			Bean<Instance<Object>> instanceBean = (Bean<Instance<Object>>)beanManager.resolve(beans);
			Instance<Object> instance = instanceBean.create(beanManager.createCreationalContext(instanceBean));
			return instance;
		}

	}

	private static final CDI<Object> _cdi = new CdiExtenderCDI();

	@Override
	public CDI<Object> getCDI() {
		return _cdi;
	}

}
