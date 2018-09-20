package org.apache.aries.cdi.test.cases;

import static org.assertj.core.api.Assertions.*;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;

import org.apache.aries.cdi.test.interfaces.BeanService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cdi.annotations.ComponentScoped;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Test152_3_1 extends AbstractTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
	}

	@AfterClass
	public static void afterClass() throws Exception {
	}

	@Override
	public void setUp() throws Exception {
		adminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
		adminTracker.open();
		configurationAdmin = adminTracker.getService();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		adminTracker.close();
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void checkSingleComponentContextEvents() throws Exception {
		Bundle tb152_3_1Bundle = installBundle("tb152_3_1.jar");

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

		ServiceRegistration<Function> onInitializedReg = bundleContext.registerService(
			Function.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Function> onBeforeDestroyedReg = bundleContext.registerService(
			Function.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Function> onDestroyedReg = bundleContext.registerService(
			Function.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		try {
			getBeanManager(tb152_3_1Bundle);

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			ServiceTracker<Object, Object> twoTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "two");
			twoTracker.open();
			int trackingCount = twoTracker.getTrackingCount();

			ServiceRegistration<Integer> integerReg = bundleContext.registerService(
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
		finally {
			onInitializedReg.unregister();
			onBeforeDestroyedReg.unregister();
			onDestroyedReg.unregister();
			tb152_3_1Bundle.uninstall();
		}
	}

	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	@Test
	public void checkFactoryComponentContextEvents() throws Exception {
		Bundle tb152_3_1Bundle = installBundle("tb152_3_1.jar");

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

		ServiceRegistration<Function> onInitializedReg = bundleContext.registerService(
			Function.class, onInitialized,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onInitialized");}});

		ServiceRegistration<Function> onBeforeDestroyedReg = bundleContext.registerService(
			Function.class, onBeforeDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onBeforeDestroyed");}});

		ServiceRegistration<Function> onDestroyedReg = bundleContext.registerService(
			Function.class, onDestroyed,
			new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "onDestroyed");}});

		Configuration configuration = null;

		try {
			getBeanManager(tb152_3_1Bundle);

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			ServiceTracker<Object, Object> threeTracker = track("(&(objectClass=%s)(%s=%s))", BeanService.class.getName(), Constants.SERVICE_DESCRIPTION, "three");
			threeTracker.open();
			int trackingCount = threeTracker.getTrackingCount();

			ServiceRegistration<Integer> integerReg = bundleContext.registerService(
				Integer.class, new Integer(45),
				new Hashtable() {{put(Constants.SERVICE_DESCRIPTION, "three");}});

			int count = 10;
			while ((count-- > 0) && trackingCount == threeTracker.getTrackingCount()) {Thread.sleep(50);}
			trackingCount = threeTracker.getTrackingCount();

			assertThat(a.get()).isNull();
			assertThat(b.get()).isNull();
			assertThat(c.get()).isNull();

			configuration = configurationAdmin.createFactoryConfiguration("three");
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
			onInitializedReg.unregister();
			onBeforeDestroyedReg.unregister();
			onDestroyedReg.unregister();
			tb152_3_1Bundle.uninstall();
		}
	}

	private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> adminTracker;
	private ConfigurationAdmin configurationAdmin;

}
