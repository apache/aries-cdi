package org.apache.aries.cdi.test.tb23;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.auth.LoginConfig;

@ApplicationPath("/")
@ApplicationScoped
@LoginConfig(authMethod = "MP-JWT", realmName = "TCK-MP-JWT")
public class TCKApplication extends Application {

	@PostConstruct
	public void init() {
		System.out.println("here");
	}
}
