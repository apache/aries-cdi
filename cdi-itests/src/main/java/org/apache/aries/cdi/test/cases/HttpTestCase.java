/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.test.cases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.apache.aries.cdi.test.cases.base.HttpBaseTestCase;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;
import org.osgi.service.http.runtime.dto.ServletDTO;

public class HttpTestCase extends HttpBaseTestCase {

	@Test
	public void testSessionScoped() throws Exception {
		bundleInstaller.installBundle("tb6.jar");
		bundleInstaller.installBundle("tb2.jar");

		String path = "/foo";

		ServletDTO servletDTO = waitFor(path);

		assertEquals("foo", servletDTO.name);

		HttpClientBuilder clientBuilder = hcbf.newBuilder();
		CloseableHttpClient httpclient = clientBuilder.build();

		CookieStore cookieStore = new BasicCookieStore();
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		URI uri = new URIBuilder(getHttpEndpoint()).
				setPath(path).
				setParameter("name", "test").
				build();

		HttpGet httpget = new HttpGet(uri);

		try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
			HttpEntity entity = response.getEntity();

			assertEquals("test", read(entity));
		}

		for (int i = 0; i < 10; i++) {
			uri = new URIBuilder(getHttpEndpoint()).
					setPath(path).
					build();

			httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
				HttpEntity entity = response.getEntity();

				assertEquals("test", read(entity));
			}
		}

		uri = new URIBuilder(getHttpEndpoint()).
				setPath(path).
				build();

		httpget = new HttpGet(uri);

		try (CloseableHttpResponse response = httpclient.execute(httpget)) {
			HttpEntity entity = response.getEntity();

			assertEquals("", read(entity));
		}
	}

	@Test
	public void testRequestScopedWithReference() throws Exception {
		bundleInstaller.installBundle("tb6.jar");
		bundleInstaller.installBundle("tb2.jar");

		String path = "/bar";

		ServletDTO servletDTO = waitFor(path);

		assertEquals("bar", servletDTO.name);

		HttpClientBuilder clientBuilder = hcbf.newBuilder();
		CloseableHttpClient httpclient = clientBuilder.build();

		CookieStore cookieStore = new BasicCookieStore();
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

		URI uri = new URIBuilder(getHttpEndpoint()).
				setPath(path).
				setParameter("name", "test").
				build();

		HttpGet httpget = new HttpGet(uri);

		try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
			HttpEntity entity = response.getEntity();

			assertEquals("POJO-IMPLtest", read(entity));
		}

		for (int i = 0; i < 10; i++) {
			uri = new URIBuilder(getHttpEndpoint()).
					setPath(path).
					build();

			httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
				HttpEntity entity = response.getEntity();

				assertEquals("", read(entity));
			}
		}

		uri = new URIBuilder(getHttpEndpoint()).
				setPath(path).
				build();

		httpget = new HttpGet(uri);

		try (CloseableHttpResponse response = httpclient.execute(httpget)) {
			HttpEntity entity = response.getEntity();

			assertEquals("", read(entity));
		}
	}

}
