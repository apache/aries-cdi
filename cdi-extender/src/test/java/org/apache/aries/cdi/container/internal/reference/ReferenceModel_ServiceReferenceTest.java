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

package org.apache.aries.cdi.container.internal.reference;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.aries.cdi.container.internal.model.CollectionType;
import org.apache.aries.cdi.container.internal.reference.ReferenceModel;
import org.apache.aries.cdi.container.test.MockInjectionPoint;
import org.apache.aries.cdi.container.test.beans.Foo;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cdi.annotations.ReferenceCardinality;
import org.osgi.util.converter.TypeReference;

public class ReferenceModel_ServiceReferenceTest {

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			ServiceReference
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			ServiceReference<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutServiceDefined_wildcardExt() throws Exception {
		Type type = new TypeReference<
			ServiceReference<? extends Callable<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void withServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			ServiceReference
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test
	public void withServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			ServiceReference<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection<ServiceReference>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void collectionWithoutServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<ServiceReference<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void collectionWithServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Collection<ServiceReference>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test
	public void collectionWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Collection<ServiceReference<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List<ServiceReference>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void listWithoutServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<ServiceReference<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void listWithServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			List<ServiceReference>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test
	public void listWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			List<ServiceReference<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Instance<ServiceReference>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void instanceWithoutServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Instance<ServiceReference<?>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();
	}

	@Test
	public void instanceWithServiceDefined() throws Exception {
		@SuppressWarnings("rawtypes")
		Type type = new TypeReference<
			Instance<ServiceReference>
		>(){}.getType();
		@SuppressWarnings("rawtypes")
		Type injectionPointType = new TypeReference<
			ServiceReference
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test
	public void instanceWithServiceDefined_wildcard() throws Exception {
		Type type = new TypeReference<
			Instance<ServiceReference<?>>
		>(){}.getType();
		Type injectionPointType = new TypeReference<
			ServiceReference<?>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Callable.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	///////////////////////////////////////////////////////////////////////////////

	@Test
	public void typed_withoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			ServiceReference<Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test
	public void typed_withoutServiceDefined_wildcardExt() throws Exception {
		Type type = new TypeReference<
			ServiceReference<? extends Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MANDATORY, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_withServiceDefined() throws Exception {
		Type type = new TypeReference<
			ServiceReference<Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();
	}

	@Test
	public void typed_collectionWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<ServiceReference<Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();

		assertEquals(Collection.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_collectionWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Collection<ServiceReference<Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();
	}

	@Test
	public void typed_listWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<ServiceReference<Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();

		assertEquals(List.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceClass());
		assertEquals(type, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_listWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			List<ServiceReference<Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();
	}

	@Test
	public void typed_instanceWithoutServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<ServiceReference<Foo>>
		>(){}.getType();
		Type injectionPointType = new TypeReference<
			ServiceReference<Foo>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		ReferenceModel referenceModel = new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).build();

		assertEquals(ServiceReference.class, referenceModel.getBeanClass());
		assertEquals(Foo.class, referenceModel.getServiceClass());
		assertEquals(injectionPointType, referenceModel.getInjectionPointType());
		assertEquals(ReferenceCardinality.MULTIPLE, referenceModel.getCardinality());
		assertEquals(CollectionType.REFERENCE, referenceModel.getCollectionType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_instanceWithServiceDefined() throws Exception {
		Type type = new TypeReference<
			Instance<ServiceReference<Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void typed_instanceWithServiceDefined_wildcardExt() throws Exception {
		Type type = new TypeReference<
			Instance<ServiceReference<? extends Foo>>
		>(){}.getType();

		InjectionPoint injectionPoint = new MockInjectionPoint(type);

		new ReferenceModel.Builder(injectionPoint.getQualifiers()).annotated(injectionPoint.getAnnotated()).service(Callable.class).build();
	}

}