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
package org.apache.aries.cdi.extension.mp.jwt.test;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.junit.Test;
import org.osgi.framework.ServiceObjects;

// NOTE: reuses tck resources and token generation
public class PreProvidedTokenTest extends MpJwtAuthTests {
	@Test
	public void runAsync() {
		ServiceObjects<ClientBuilder> cbSO = bcr.getBundleContext().getServiceObjects(cbr.getServiceReference());
		final ClientBuilder cb = cbSO.getService();
		cb.connectTimeout(100, TimeUnit.SECONDS);
		cb.readTimeout(100, TimeUnit.SECONDS);

		final Client client = cb.build();

		try {
			final String value = client.target(getJaxrsEndpoint())
					.path("inspector")
					.queryParam("claim", "name")
					.request(TEXT_PLAIN_TYPE)
					.get(String.class)
					.trim();
			assertEquals("run-as", value);
		} finally {
			client.close();
			cbSO.ungetService(cb);
		}
	}
}
