/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aries.cdi.test.cases.mpjwt;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Cookie;

import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.test.junit4.service.ServiceUseRule;

// NOTE: reuses tck resources and token generation
public class CookieTest extends MpJwtAuthTests {
	@Rule
	public ServiceUseRule<ClientBuilder> cbr = new ServiceUseRule.Builder<>(ClientBuilder.class) //
		.build();

	@Test
	public void test() throws Exception {
		final ClientBuilder cb = cbr.getService();
		cb.connectTimeout(1000, TimeUnit.SECONDS);
		cb.readTimeout(1000, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String token = TokenUtils.generateTokenString("/Token2.json");
			final String serverToken = client.target(getEndpoint())
					.path("passthrough")
					.request(TEXT_PLAIN_TYPE)
					.cookie(new Cookie("Bearer", token))
					.get(String.class);
			assertEquals(serverToken, token);
		} finally {
			client.close();
		}
	}
}
