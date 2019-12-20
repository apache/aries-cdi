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

package org.apache.aries.cdi.test.cases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;

import org.apache.aries.cdi.test.cases.base.CloseableTracker;
import org.apache.aries.cdi.test.cases.base.SlimBaseTestCase;
import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cm.Configuration;

public class Test152_3_1 extends SlimBaseTestCase {

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void checkSingleComponentContextEvents() throws Exception {
		Bundle tb152_3_1Bundle = bcr.installBundle("tb152_3_1.jar");

		AtomicReference<Object[]> a = new AtomicReference<>();
		AtomicReference<Object[]> b = new AtomicReference<>();
		AtomicReference<Object[]> c = new AtomicReference<>();

		Function onInitialized = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			a.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};
		Function onBeforeDestroyed = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			b.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};
		Function onDestroyed = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			c.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};

		bcr.getBundleContext().registerService(
			Function.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		bcr.getBundleContext().registerService(
			Function.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		bcr.getBundleContext().registerService(
			Function.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		try (CloseableTracker<Object, Object> twoTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "two")) {
			getBeanManager(tb152_3_1Bundle);

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			twoTracker.open();
			int trackingCount = twoTracker.getTrackingCount();

			ServiceRegistration<Integer> integerReg = bcr.getBundleContext().registerService(
				Integer.class, new Integer(45),
				new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "two");}});

			while (trackingCount == twoTracker.getTrackingCount()) {Thread.sleep(50);}
			trackingCount = twoTracker.getTrackingCount();

			assertThat(a.get()).isNotNull();
			assertThat(a.get()[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Two"));
			assertThat(a.get()[1]).isEqualTo(true);
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			integerReg.unregister();

			while (trackingCount == twoTracker.getTrackingCount()) {Thread.sleep(50);}

			Object[] objects = a.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("POST_CONSTRUCTED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Two"));
			assertThat(objects[1]).isEqualTo(true);
			EventMetadata eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));

			objects = b.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("POST_CONSTRUCTED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Two"));
			assertThat(objects[1]).isEqualTo(true);
			eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));

			objects = c.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("DESTROYED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Two"));
			assertThat(objects[1]).isEqualTo(true);
			eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void checkFactoryComponentContextEvents() throws Exception {
		Bundle tb152_3_1Bundle = bcr.installBundle("tb152_3_1.jar");

		AtomicReference<Object[]> a = new AtomicReference<>();
		AtomicReference<Object[]> b = new AtomicReference<>();
		AtomicReference<Object[]> c = new AtomicReference<>();

		Function onInitialized = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			a.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};
		Function onBeforeDestroyed = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			b.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};
		Function onDestroyed = (o) -> {
			Object[] values = (Object[])o;
			BeanManager bm = (BeanManager)values[1];
			c.set(new Object[] {values[0], bm.getContext(ComponentScoped.class).isActive(), values[2]});
			return null;
		};

		bcr.getBundleContext().registerService(
			Function.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		bcr.getBundleContext().registerService(
			Function.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		bcr.getBundleContext().registerService(
			Function.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Configuration configuration = null;

		try (CloseableTracker<Object, Object> threeTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "three")) {
			getBeanManager(tb152_3_1Bundle);

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			threeTracker.open();
			int trackingCount = threeTracker.getTrackingCount();

			ServiceRegistration<Integer> integerReg = bcr.getBundleContext().registerService(
				Integer.class, new Integer(45),
				new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "three");}});

			int count = 10;
			while ((count-- > 0) && trackingCount == threeTracker.getTrackingCount()) {Thread.sleep(50);}
			trackingCount = threeTracker.getTrackingCount();

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			configuration = car.getService().createFactoryConfiguration("three");
			configuration.update(new Hashtable() {{put("foo", "bar");}});

			count = 10;
			while ((count-- > 0) && trackingCount == threeTracker.getTrackingCount()) {Thread.sleep(50);}
			trackingCount = threeTracker.getTrackingCount();

			assertThat(a.get()).isNotNull();
			assertThat(a.get()[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Three"));
			assertThat(a.get()[1]).isEqualTo(true);
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			integerReg.unregister();

			while (trackingCount == threeTracker.getTrackingCount()) {Thread.sleep(50);}

			Object[] objects = a.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("POST_CONSTRUCTED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Three"));
			assertThat(objects[1]).isEqualTo(true);
			EventMetadata eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));

			objects = b.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("POST_CONSTRUCTED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Three"));
			assertThat(objects[1]).isEqualTo(true);
			eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));

			objects = c.get();
			assertThat(objects).isNotNull();
			assertThat(objects.length).isEqualTo(3);
			assertThat((BeanService<Integer>)objects[0]).satisfies(bs -> bs.doSomething().equals("DESTROYED"));
			assertThat(objects[0]).satisfies(o -> o.getClass().getName().equals("org.apache.aries.cdi.test.tb152_3_1.Three"));
			assertThat(objects[1]).isEqualTo(true);
			eventMetadata = (EventMetadata)objects[2];
			assertThat(eventMetadata.getQualifiers()).contains(Service.Literal.of(new Class<?>[0]));
		}
		finally {
			configuration.delete();
		}
	}

}
