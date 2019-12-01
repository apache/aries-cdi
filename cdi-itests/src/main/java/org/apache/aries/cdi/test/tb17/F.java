package org.apache.aries.cdi.test.tb17;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Reference;

@Decorator
public class F implements Pojo {

	@Inject @Delegate @Reference Pojo pojo;

	@Override
	public String foo(String fooInput) {
		return "F" + pojo.foo(fooInput);
	}

	@Override
	public int getCount() {
		return pojo.getCount();
	}

}
