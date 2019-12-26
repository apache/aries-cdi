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

import java.util.stream.Stream;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.aries.cdi.extension.spi.adapt.MergeServiceTypes;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardListener;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.spi.configuration.Configuration;
import org.osgi.service.cdi.annotations.Service;

public class WebListenerProcessor {

	/**
	 * Call this method from an observer defined as:
	 * <pre>
	 * &lt;X&gt; void webListener(@Observes @WithAnnotations(WebListener.class) ProcessAnnotatedType&lt;X&gt; pat) {
	 *   new WebListenerProcessor().process(configuration, pat);
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

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();
		final Class<X> javaClass = annotatedType.getJavaClass();
		final Class<?>[] serviceTypes = Stream.of(
				ServletContextListener.class,
				ServletContextAttributeListener.class,
				ServletRequestListener.class,
				ServletRequestAttributeListener.class,
				HttpSessionListener.class,
				HttpSessionAttributeListener.class,
				HttpSessionIdListener.class)
				.filter(c -> c.isAssignableFrom(javaClass))
				.toArray(Class[]::new);

		beanManager.fireEvent(new MergeServiceTypes<>(pat, serviceTypes));

		AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();

		WebListener webListener = annotatedType.getAnnotation(WebListener.class);

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardContextSelect.class)) {
			ofNullable((String)configuration.get(HTTP_WHITEBOARD_CONTEXT_SELECT)).ifPresent(
					select -> configurator.add(HttpWhiteboardContextSelect.Literal.of(select))
			);
		}

		if(!annotatedType.isAnnotationPresent(HttpWhiteboardListener.class)) {
			configurator.add(HttpWhiteboardListener.Literal.INSTANCE);
		}

		if (!annotatedType.isAnnotationPresent(ServiceDescription.class) && !webListener.value().isEmpty()) {
			configurator.add(ServiceDescription.Literal.of(webListener.value()));
		}
	}

}
