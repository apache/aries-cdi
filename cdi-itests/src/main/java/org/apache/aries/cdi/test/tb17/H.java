package org.apache.aries.cdi.test.tb17;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.aries.cdi.test.interfaces.Pojo;
import org.osgi.service.log.Logger;

@Interceptor
public class H {

	@Inject Logger log;
	@Inject Pojo pojo;

	@AroundInvoke
	public Object authorize(InvocationContext ic) throws Exception {
		if (pojo.getCount() > 0) {
			log.debug("Pojo Count {}", pojo.getCount());
		}
		else {
			log.debug("Pojo has no count");
		}
		return ic.proceed();
	}
}
