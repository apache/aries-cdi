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

package org.apache.aries.cdi.extension.mp.config;

import java.io.IOException;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.cdi.CDIConstants;

import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

public class BB implements Plugin {

	private final BuildLogger buildLogger;
	private final String mpVersion;

	public BB(BuildLogger buildLogger, String mpVersion) {
		this.buildLogger = buildLogger;
		this.mpVersion = mpVersion;
	}

	@Override
	public boolean matches(TypeDescription target) {
		return target.getName().equals("org.eclipse.microprofile.config.inject.ConfigProperty");
	}

	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator cfl) {
		buildLogger.info("Processing class: " + typeDescription.getActualName());

		return builder.annotateType(
			AnnotationDescription.Builder.ofType(Requirement.class)
				.define("namespace", CDIConstants.CDI_EXTENSION_PROPERTY)
				.define("name", "eclipse.microprofile.config")
				.define("version", mpVersion)
				.build());
	}

	@Override
	public void close() throws IOException {
	}

}
