package org.apache.aries.cdi.test.cases.jaxrs;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.aries.cdi.test.cases.base.JaxrsBaseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;

public class ResourceTests extends JaxrsBaseTestCase {

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		cdiBundle = bcr.installBundle("tb24.jar");

		int count = 100;
		RuntimeDTO runtimeDTO;
		while ((runtimeDTO = jsrr.getService().getRuntimeDTO()).defaultApplication.resourceDTOs.length < 1 && (count > 0)) {
			count--;
			Thread.sleep(100);
		}

		assertThat(runtimeDTO.defaultApplication.resourceDTOs).extracting("name").contains(
			"A");
	}

	@After
	@Override
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
	}

	@Test
	public void test() throws Exception {
		final ClientBuilder cb = cbr.getService();
		cb.connectTimeout(1000, TimeUnit.SECONDS);
		cb.readTimeout(1000, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String serverToken = client.target(getJaxrsEndpoint())
					.path("a")
					.request(TEXT_PLAIN_TYPE)
					.get(String.class);
			assertEquals("a", serverToken);
		} finally {
			client.close();
		}
	}

}
