package org.apache.aries.cdi.test.tb17;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.cdi.annotations.Service;
import org.osgi.service.cdi.annotations.SingleComponent;

@Service
@SingleComponent
public class C implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "C" + fooInput;
	}

	@Override
	public int getCount() {
		return 1;
	}

}
