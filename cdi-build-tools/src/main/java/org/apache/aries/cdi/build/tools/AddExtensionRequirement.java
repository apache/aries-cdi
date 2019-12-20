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

import static java.lang.String.format;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

import java.io.IOException;
import java.util.Arrays;

import org.osgi.annotation.bundle.Requirement;
import org.osgi.annotation.bundle.Requirements;
import org.osgi.service.cdi.CDIConstants;

import net.bytebuddy.build.BuildLogger;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;

/**
 * Example:
 * <pre>
 * &lt;transformation&gt;
 *   &lt;plugin&gt;org.apache.aries.cdi.build.tools.AddExtensionRequirement.AddExtensionRequirement&lt;/plugin&gt;
 *   &lt;arguments&gt;
 *     &lt;argument&gt;
 *       &lt;index&gt;1&lt;/index&gt;
 *       &lt;value&gt;eclipse.microprofile.config&lt;/value&gt;
 *     &lt;/argument&gt;
 *     &lt;argument&gt;
 *       &lt;index&gt;2&lt;/index&gt;
 *       &lt;value&gt;${mp.config.version}&lt;/value&gt;
 *     &lt;/argument&gt;
 *     &lt;argument&gt;
 *       &lt;index&gt;3&lt;/index&gt;
 *       &lt;value&gt;org.eclipse.microprofile.config.inject.ConfigProperty&lt;/value&gt;
 *     &lt;/argument&gt;
 *   &lt;/arguments&gt;
 * &lt;/transformation&gt;
 * </pre>
 */
public class AddExtensionRequirement implements Plugin {

	private final BuildLogger buildLogger;
	private final String name;
	private final Match match;
	private final AnnotationDescription annotationDescription;

	private final TypeDescription requirementsDescription = TypeDescription.ForLoadedType.of(Requirements.class);
	private final MethodDescription.InDefinedShape requirementsValue = requirementsDescription.getDeclaredMethods().getOnly();

	private enum Match {
		CLASS, PACKAGE, PREFIX
	}

	public AddExtensionRequirement(
		BuildLogger buildLogger, String extension, String version, String glob) {

		this.buildLogger = buildLogger;

		this.annotationDescription = AnnotationDescription.Builder.ofType(Requirement.class)
			.define("namespace", CDIConstants.CDI_EXTENSION_PROPERTY)
			.define("name", extension)
			.define("version", version)
			.build();

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
	public boolean matches(TypeDescription typeDescription) {
		if (typeDescription.isPackageType()) {
			return false;
		}

		if (isAnnotatedWith(Requirements.class).matches(typeDescription)) {
			AnnotationDescription[] annotationDescriptions = typeDescription.getDeclaredAnnotations().ofType(Requirements.class).getValue(requirementsValue).resolve(AnnotationDescription[].class);

			if (Arrays.asList(annotationDescriptions).contains(annotationDescription)) {
				return false;
			}
		}

		String className = typeDescription.getName();
		String packageName = typeDescription.getPackage().getName();

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
		buildLogger.info(format("Adding requirement %s on type %s", annotationDescription, typeDescription.getActualName()));

		AnnotationDescription[] annotationDescriptions = new AnnotationDescription[1];

		// This isn't quite working like I'd expect because the builder cannot see previous transformations.
		// We need some kind of merging operation to occur.
		if (isAnnotatedWith(requirementsDescription).matches(typeDescription)) {
			annotationDescriptions = typeDescription.getDeclaredAnnotations().ofType(Requirements.class).getValue(requirementsValue).resolve(AnnotationDescription[].class);

			buildLogger.info(format("Requirements were found on %s, %s", typeDescription.getActualName(), annotationDescriptions));

			annotationDescriptions = Arrays.copyOf(annotationDescriptions, annotationDescriptions.length + 1);

			builder = builder.visit(new RequirementsAnnotationRemover());
		}

		annotationDescriptions[annotationDescriptions.length - 1] = annotationDescription;

		return builder.annotateType(
			AnnotationDescription.Builder.ofType(Requirements.class)
				.defineAnnotationArray("value", annotationDescription.getAnnotationType(), annotationDescriptions)
				.build());
	}

	@Override
	public void close() throws IOException {
	}

}
