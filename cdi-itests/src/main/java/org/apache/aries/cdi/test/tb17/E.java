package org.apache.aries.cdi.test.tb17;

import javax.enterprise.context.ApplicationScoped;

import org.apache.aries.cdi.test.interfaces.Pojo;

@ApplicationScoped
public class E implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "E" + fooInput;
	}

	@Override
	public int getCount() {
		return 1;
	}

}
