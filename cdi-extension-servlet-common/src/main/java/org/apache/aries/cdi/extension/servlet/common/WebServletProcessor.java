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

package org.apache.aries.cdi.extension.servlet.common;

import static java.util.Optional.ofNullable;
import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.Servlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import org.apache.aries.cdi.extension.spi.adapt.Adapted;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletMultipart;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletPattern;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.apache.aries.cdi.spi.configuration.Configuration;

public class WebServletProcessor {

	/**
	 * Call this method from an observer defined as:
	 * <pre>
	 * &lt;X&gt; void webServlet(@Observes @WithAnnotations(WebServlet.class) ProcessAnnotatedType&lt;X&gt; pat) {
	 *   new WebServletProcessor().process(configuration, pat);
	 * }
	 * </pre>
	 * @param <X>
	 * @param pat
	 */
	public <X> void process(
		Configuration configuration, ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();

		if (!Adapted.withServiceTypes(configurator, Servlet.class)) {
			return;
		}

		WebServlet webServlet = annotatedType.getAnnotation(WebServlet.class);

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
				select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletName.class) && !webServlet.name().isEmpty()) {
			configurator.add(HttpWhiteboardServletName.Literal.of(webServlet.name()));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardServletPattern.class)) {
			if (webServlet.value().length > 0) {
				configurator.add(HttpWhiteboardServletPattern.Literal.of(webServlet.value()));
			}
			else if (webServlet.urlPatterns().length > 0) {
				configurator.add(HttpWhiteboardServletPattern.Literal.of(webServlet.urlPatterns()));
			}
		}

		if (!annotatedType.isAnnotationPresent(ServiceRanking.class)) {
			configurator.add(ServiceRanking.Literal.of(webServlet.loadOnStartup()));
		}

		// TODO Howto: INIT PARAMS ???

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletAsyncSupported.class)) {
			configurator.add(HttpWhiteboardServletAsyncSupported.Literal.of(webServlet.asyncSupported()));
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webServlet.description().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webServlet.description()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardServletMultipart.class)) {
			MultipartConfig multipartConfig = annotatedType.getAnnotation(MultipartConfig.class);

			if (multipartConfig != null) {
				configurator.add(HttpWhiteboardServletMultipart.Literal.of(true, multipartConfig.fileSizeThreshold(), multipartConfig.location(), multipartConfig.maxFileSize(), multipartConfig.maxRequestSize()));
			}
		}

		// TODO HowTo: ServletSecurity ???
	}

}
