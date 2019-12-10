package org.apache.aries.cdi.test.cases.jaxrs;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.aries.cdi.test.cases.JaxrsBaseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.test.junit4.service.ServiceUseRule;

public class ResourceTests extends JaxrsBaseTestCase {
	@Rule
	public ServiceUseRule<ClientBuilder> cbr = new ServiceUseRule.Builder<>(ClientBuilder.class) //
		.build();

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		cdiBundle = installBundle("tb24.jar");

		int count = 100;
		while (jsr.getRuntimeDTO().defaultApplication.resourceDTOs.length < 1 && (count > 0)) {
			count--;
			Thread.sleep(100);
		}

		assertThat(jsr.getRuntimeDTO().defaultApplication.resourceDTOs).extracting("name").contains(
			"A");
	}

	@After
	@Override
	public void tearDown() throws Exception {
		cdiBundle.uninstall();
		super.tearDown();
	}

	@Test
	public void test() throws Exception {
		final ClientBuilder cb = cbr.getService();
		cb.connectTimeout(1000, TimeUnit.SECONDS);
		cb.readTimeout(1000, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String serverToken = client.target(getEndpoint())
					.path("a")
					.request(TEXT_PLAIN_TYPE)
					.get(String.class);
			assertEquals("a", serverToken);
		} finally {
			client.close();
		}
	}

}
