package org.apache.aries.cdi.test.tb17;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;

@Service
public class A implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "A" + fooInput;
	}

	@Override
	public int getCount() {
		return 1;
	}

}
