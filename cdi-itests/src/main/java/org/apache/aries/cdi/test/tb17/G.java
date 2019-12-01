package org.apache.aries.cdi.test.tb17;

import javax.enterprise.context.ConversationScoped;

import org.apache.aries.cdi.test.interfaces.Pojo;

@ConversationScoped
public class G implements Pojo {

	@Override
	public String foo(String fooInput) {
		return "G" + fooInput;
	}

	@Override
	public int getCount() {
		return 0;
	}

}
