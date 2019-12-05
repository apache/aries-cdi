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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.assertj.core.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

public class HttpTestCase extends AbstractTestCase {

	@Ignore
	@Test
	public void testSessionScoped() throws Exception {
		Bundle tb6Bundle = installBundle("tb6.jar");
		Bundle tb2Bundle = installBundle("tb2.jar");

		try {
			String path = "/foo";

			ServletDTO servletDTO = waitFor(path);

			assertEquals("foo", servletDTO.name);

			HttpClientBuilder clientBuilder = hcbf.newBuilder();
			CloseableHttpClient httpclient = clientBuilder.build();

			CookieStore cookieStore = new BasicCookieStore();
			HttpContext httpContext = new BasicHttpContext();
			httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			URI uri = new URIBuilder(getEndpoint()).
				setPath(path).
				setParameter("name", "test").
				build();

			HttpGet httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
				HttpEntity entity = response.getEntity();

				assertEquals("test", read(entity));
			}

			for (int i = 0; i < 10; i++) {
				uri = new URIBuilder(getEndpoint()).
					setPath(path).
					build();

				httpget = new HttpGet(uri);

				try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
					HttpEntity entity = response.getEntity();

					assertEquals("test", read(entity));
				}
			}

			uri = new URIBuilder(getEndpoint()).
				setPath(path).
				build();

			httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget)) {
				HttpEntity entity = response.getEntity();

				assertEquals("", read(entity));
			}
		}
		finally {
			tb6Bundle.uninstall();
			tb2Bundle.uninstall();
		}
	}

	@Ignore
	@Test
	public void testRequestScopedWithReference() throws Exception {
		Bundle tb6Bundle = installBundle("tb6.jar");
		Bundle tb2Bundle = installBundle("tb2.jar");

		try {
			String path = "/bar";

			ServletDTO servletDTO = waitFor(path);

			assertEquals("bar", servletDTO.name);

			HttpClientBuilder clientBuilder = hcbf.newBuilder();
			CloseableHttpClient httpclient = clientBuilder.build();

			CookieStore cookieStore = new BasicCookieStore();
			HttpContext httpContext = new BasicHttpContext();
			httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

			URI uri = new URIBuilder(getEndpoint()).
				setPath(path).
				setParameter("name", "test").
				build();

			HttpGet httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
				HttpEntity entity = response.getEntity();

				assertEquals("POJO-IMPLtest", read(entity));
			}

			for (int i = 0; i < 10; i++) {
				uri = new URIBuilder(getEndpoint()).
					setPath(path).
					build();

				httpget = new HttpGet(uri);

				try (CloseableHttpResponse response = httpclient.execute(httpget, httpContext)) {
					HttpEntity entity = response.getEntity();

					assertEquals("", read(entity));
				}
			}

			uri = new URIBuilder(getEndpoint()).
				setPath(path).
				build();

			httpget = new HttpGet(uri);

			try (CloseableHttpResponse response = httpclient.execute(httpget)) {
				HttpEntity entity = response.getEntity();

				assertEquals("", read(entity));
			}
		}
		finally {
			tb6Bundle.uninstall();
			tb2Bundle.uninstall();
		}
	}

	private String getEndpoint() {
		String[] endpoints = (String[])hsrReference.getProperty("osgi.http.endpoint");

		if (endpoints == null || endpoints.length == 0) {
			String port = (String)hsrReference.getProperty("org.osgi.service.http.port");
			return "http://localhost:" + port;
		}

		return endpoints[0];
	}

	@Before
	@Override
	public void setUp() throws Exception {
		hsrTracker = new ServiceTracker<>(bundleContext, HttpServiceRuntime.class, null);

		hsrTracker.open();

		hsr = hsrTracker.waitForService(timeout);

		hsrReference = hsrTracker.getServiceReference();

		hcbfTracker = new ServiceTracker<>(bundleContext, HttpClientBuilderFactory.class, null);

		hcbfTracker.open();

		hcbf = hcbfTracker.waitForService(timeout);

		assertNotNull(hsr);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		hsrTracker.close();
		hcbfTracker.close();
	}

	private static String read(HttpEntity entity) throws Exception {
		if (entity == null) {
			return null;
		}

		try (InputStream in = entity.getContent();
			java.util.Scanner s = new java.util.Scanner(in)) {

			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

	private ServletDTO waitFor(String path) throws InterruptedException {
		return waitFor(path, 20);
	}

	private ServletDTO waitFor(String path, int intervals) throws InterruptedException {
		for (int j = intervals; j > 0; j--) {
			for (ServletContextDTO scDTO : hsr.getRuntimeDTO().servletContextDTOs) {
				if (scDTO.name.equals(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME)) {
					for (ServletDTO sDTO : scDTO.servletDTOs) {
						if (Arrays.asList(sDTO.patterns).contains(path)) {
							return sDTO;
						}
					}
				}
			}

			Thread.sleep(50);
		}

		assertTrue(String.format("%s not found in time", path), false);

		return null;
	}

	private HttpClientBuilderFactory hcbf;
	private HttpServiceRuntime hsr;
	private ServiceReference<HttpServiceRuntime> hsrReference;
	private ServiceTracker<HttpClientBuilderFactory, HttpClientBuilderFactory> hcbfTracker;
	private ServiceTracker<HttpServiceRuntime, HttpServiceRuntime> hsrTracker;

}
