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

import static org.osgi.service.http.whiteboard.HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.servlet.Servlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;

import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletMultipart;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardServletPattern;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.extra.propertytypes.ServiceRanking;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.service.cdi.annotations.Service;

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

		WebServlet webServlet = annotatedType.getAnnotation(WebServlet.class);

		final Set<Annotation> annotationsToAdd = new HashSet<>();

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotationsToAdd.add(Service.Literal.of(new Class[] {Servlet.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotationsToAdd.add(HttpWhiteboardContextSelect.Literal.of((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)));
		}

		if (!webServlet.name().isEmpty()) {
			annotationsToAdd.add(HttpWhiteboardServletName.Literal.of(webServlet.name()));
		}

		if (webServlet.value().length > 0) {
			annotationsToAdd.add(HttpWhiteboardServletPattern.Literal.of(webServlet.value()));
		}
		else if (webServlet.urlPatterns().length > 0) {
			annotationsToAdd.add(HttpWhiteboardServletPattern.Literal.of(webServlet.urlPatterns()));
		}

		annotationsToAdd.add(ServiceRanking.Literal.of(webServlet.loadOnStartup()));

		// TODO Howto: INIT PARAMS ???

		annotationsToAdd.add(HttpWhiteboardServletAsyncSupported.Literal.of(webServlet.asyncSupported()));

		if (!webServlet.description().isEmpty()) {
			annotationsToAdd.add(ServiceDescription.Literal.of(webServlet.description()));
		}

		MultipartConfig multipartConfig = annotatedType.getAnnotation(MultipartConfig.class);

		if (multipartConfig != null) {
			annotationsToAdd.add(HttpWhiteboardServletMultipart.Literal.of(true, multipartConfig.fileSizeThreshold(), multipartConfig.location(), multipartConfig.maxFileSize(), multipartConfig.maxRequestSize()));
		}

		// TODO HowTo: ServletSecurity ???

		if (!annotationsToAdd.isEmpty()) {
			annotationsToAdd.forEach(pat.configureAnnotatedType()::add);
		}
	}

}
