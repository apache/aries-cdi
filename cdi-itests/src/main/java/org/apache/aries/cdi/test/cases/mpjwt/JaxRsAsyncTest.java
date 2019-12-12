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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_MEDIA_TYPE;

import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.ext.MessageBodyReader;

import org.eclipse.microprofile.jwt.tck.util.TokenUtils;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.test.junit4.service.ServiceUseRule;

// NOTE: reuses tck resources and token generation
public class JaxRsAsyncTest extends MpJwtAuthTests {
	@Rule
	@SuppressWarnings("rawtypes")
	public ServiceUseRule<MessageBodyReader> mbr = new ServiceUseRule.Builder<>(MessageBodyReader.class) //
		.filter("(%s=%s)", JAX_RS_MEDIA_TYPE, APPLICATION_JSON)
		.build();
	@Rule
	public ServiceUseRule<ClientBuilder> cbr = new ServiceUseRule.Builder<>(ClientBuilder.class) //
		.build();

	@Test
	public void runAsync() throws Exception {
		final ClientBuilder cb = cbr.getService();
		cb.register(mbr.getService());
		cb.connectTimeout(1000, TimeUnit.SECONDS);
		cb.readTimeout(1000, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String token = TokenUtils.generateTokenString("/Token2.json");
			final JsonObject object = client.target(getEndpoint())
					.path("test/async")
					.request(APPLICATION_JSON_TYPE)
					.header("Authorization", "bearer " + token)
					.get(JsonObject.class);
			assertEquals(object.toString(), "{\"before\":\"sync=" + token + "\",\"after\":\"async=" + token + "\"}");
		} finally {
			client.close();
		}
	}
}
