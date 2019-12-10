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
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterAsyncSupported;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterDispatcher;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterName;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterPattern;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardFilterServlet;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.service.cdi.annotations.Service;

public class WebFilterProcessor {

	/**
	 * Call this method from an observer defined as:
	 * <pre>
	 * &lt;X&gt; void webFilter(@Observes @WithAnnotations(WebFilter.class) ProcessAnnotatedType&lt;X&gt; pat) {
	 *   new WebFilterProcessor().process(configuration, pat);
	 * }
	 * </pre>
	 * @param <X>
	 * @param pat
	 */
	public <X> void process(
		Configuration configuration, ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebFilter webFilter = annotatedType.getAnnotation(WebFilter.class);

		final Set<Annotation> annotationsToAdd = new HashSet<>();

		if (!annotatedType.isAnnotationPresent(Service.class)) {
			annotationsToAdd.add(Service.Literal.of(new Class[] {Filter.class}));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			annotationsToAdd.add(HttpWhiteboardContextSelect.Literal.of((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)));
		}

		if (!webFilter.description().isEmpty()) {
			annotationsToAdd.add(ServiceDescription.Literal.of(webFilter.description()));
		}

		if (!webFilter.filterName().isEmpty()) {
			annotationsToAdd.add(HttpWhiteboardFilterName.Literal.of(webFilter.filterName()));
		}

		if (webFilter.servletNames().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterServlet.Literal.of(webFilter.servletNames()));
		}

		if (webFilter.value().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.value()));
		}
		else if (webFilter.urlPatterns().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.urlPatterns()));
		}

		if (webFilter.dispatcherTypes().length > 0) {
			annotationsToAdd.add(HttpWhiteboardFilterDispatcher.Literal.of(webFilter.dispatcherTypes()));
		}

		annotationsToAdd.add(HttpWhiteboardFilterAsyncSupported.Literal.of(webFilter.asyncSupported()));

		if (!annotationsToAdd.isEmpty()) {
			annotationsToAdd.forEach(pat.configureAnnotatedType()::add);
		}
	}

}
