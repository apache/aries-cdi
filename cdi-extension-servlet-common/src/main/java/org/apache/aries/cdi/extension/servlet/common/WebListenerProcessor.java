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

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.servlet.annotation.WebListener;

import org.apache.aries.cdi.extension.spi.adapt.Adapted;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardContextSelect;
import org.apache.aries.cdi.extra.propertytypes.HttpWhiteboardListener;
import org.apache.aries.cdi.extra.propertytypes.ServiceDescription;
import org.apache.aries.cdi.spi.configuration.Configuration;

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
		Configuration configuration, ProcessAnnotatedType<X> pat) {

		final AnnotatedType<X> annotatedType = pat.getAnnotatedType();

		AnnotatedTypeConfigurator<X> configurator = pat.configureAnnotatedType();

		Set<Class<?>> serviceTypes = new HashSet<>();

		Class<X> javaClass = annotatedType.getJavaClass();

		if (javax.servlet.ServletContextListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.ServletContextListener.class);
		}
		if (javax.servlet.ServletContextAttributeListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.ServletContextAttributeListener.class);
		}
		if (javax.servlet.ServletRequestListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.ServletRequestListener.class);
		}
		if (javax.servlet.ServletRequestAttributeListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.ServletRequestAttributeListener.class);
		}
		if (javax.servlet.http.HttpSessionListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.http.HttpSessionListener.class);
		}
		if (javax.servlet.http.HttpSessionAttributeListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.http.HttpSessionAttributeListener.class);
		}
		if (javax.servlet.http.HttpSessionIdListener.class.isAssignableFrom(javaClass)) {
			serviceTypes.add(javax.servlet.http.HttpSessionIdListener.class);
		}

		if (!Adapted.withServiceTypes(configurator, serviceTypes)) {
			return;
		}

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
