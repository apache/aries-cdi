package org.apache.aries.cdi.test.tb17;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.ComponentScoped;

@ComponentScoped
public class D implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "D" + fooInput;
	}

	@Override
	public int getCount() {
		return 1;
	}

}
