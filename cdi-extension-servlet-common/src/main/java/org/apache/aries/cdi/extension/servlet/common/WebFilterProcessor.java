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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
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
			Configuration configuration, ProcessAnnotatedType<X> pat, BeanManager beanManager) {
		if (pat.getAnnotatedType().isAnnotationPresent(Service.class)) {
			return;
		}

		beanManager.fireEvent(new MergeServiceTypes<>(pat, Filter.class));
		final AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();
		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		WebFilter webFilter = annotatedType.getAnnotation(WebFilter.class);

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
					select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webFilter.description().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webFilter.description()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterName.class) && !webFilter.filterName().isEmpty()) {
			configurator.add(HttpWhiteboardFilterName.Literal.of(webFilter.filterName()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterServlet.class) && webFilter.servletNames().length > 0) {
			configurator.add(HttpWhiteboardFilterServlet.Literal.of(webFilter.servletNames()));
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterPattern.class)) {
			if (webFilter.value().length > 0) {
				configurator.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.value()));
			}
			else if (webFilter.urlPatterns().length > 0) {
				configurator.add(HttpWhiteboardFilterPattern.Literal.of(webFilter.urlPatterns()));
			}
		}

		if (!annotatedType.isAnnotationPresent(HttpWhiteboardFilterDispatcher.class) && webFilter.dispatcherTypes().length > 0) {
			configurator.add(HttpWhiteboardFilterDispatcher.Literal.of(webFilter.dispatcherTypes()));
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardFilterAsyncSupported.class)) {
			configurator.add(HttpWhiteboardFilterAsyncSupported.Literal.of(webFilter.asyncSupported()));
		}
	}

}
