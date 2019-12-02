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

package org.apache.aries.cdi.build.tools;

import java.io.IOException;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.service.cdi.CDIConstants;

import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

/**
 * Example:
 * <pre>
 * &lt;transformation>
 *   &lt;plugin>org.apache.aries.cdi.build.tools.AddExtensionRequirement.AddExtensionRequirement&lt;/plugin>
 *   &lt;arguments>
 *     &lt;argument>
 *       &lt;index>1</index>
 *       &lt;value>eclipse.microprofile.config&lt;/value>
 *     &lt;/argument>
 *     &lt;argument>
 *       &lt;index>2&lt;/index>
 *       &lt;value>${mp.config.version}&lt;/value>
 *     &lt;/argument>
 *     &lt;argument>
 *       &lt;index>3&lt;/index>
 *       &lt;value>org.eclipse.microprofile.config.inject.ConfigProperty&lt;/value>
 *     &lt;/argument>
 *   &lt;/arguments>
 * &lt;/transformation>
 * </pre>
 */
public class AddExtensionRequirement implements Plugin {

	private final BuildLogger buildLogger;
	private final String extension;
	private final String version;
	private final String name;
	private final Match match;

	private enum Match {
		CLASS, PACKAGE, PREFIX
	}

	public AddExtensionRequirement(
		BuildLogger buildLogger, String extension, String version, String glob) {

		this.buildLogger = buildLogger;
		this.extension = extension;
		this.version = version;

		String name = glob;

		if (glob.endsWith(".**")) {
			match = Match.PREFIX;
			name = glob.substring(0, glob.length() - 3);
		}
		else if (glob.endsWith(".*")) {
			match = Match.PACKAGE;
			name = glob.substring(0, glob.length() - 2);
		}
		else {
			match = Match.CLASS;
			name = glob;
		}

		this.name = name;
	}

	@Override
	public boolean matches(TypeDescription target) {
		String className = target.getName();
		String packageName = target.getPackage().getName();

		boolean matches = false;
		switch (match) {
			case CLASS: {
				matches = className.equals(name);
				break;
			}
			case PACKAGE: {
				matches = packageName.equals(name);
				break;
			}
			case PREFIX: {
				matches = packageName.startsWith(name);
			}
		}

		return matches;
	}

	@Override
	public Builder<?> apply(Builder<?> builder, TypeDescription typeDescription, ClassFileLocator cfl) {
		buildLogger.info("Processing class: " + typeDescription.getActualName());

		return builder.annotateType(
			AnnotationDescription.Builder.ofType(Requirement.class)
				.define("namespace", CDIConstants.CDI_EXTENSION_PROPERTY)
				.define("name", extension)
				.define("version", version)
				.build());
	}

	@Override
	public void close() throws IOException {
	}

}
