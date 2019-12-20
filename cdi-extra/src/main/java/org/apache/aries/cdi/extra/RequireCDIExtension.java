/*
 * Copyright (c) OSGi Alliance (2018). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.cdi.extra;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.apache.aries.cdi.extra.RequireCDIExtension.EFFECTIVE_MACRO;
import static org.apache.aries.cdi.extra.RequireCDIExtension.RESOLUTION_MACRO;
import static org.apache.aries.cdi.extra.RequireCDIExtension.VALUE_MACRO;
import static org.apache.aries.cdi.extra.RequireCDIExtension.VERSION_MACRO;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

import aQute.bnd.annotation.Resolution;

@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Repeatable(RequireCDIExtensions.class)
@Requirement(namespace = CDI_EXTENSION_PROPERTY, name = VALUE_MACRO, attribute = {
	VERSION_MACRO, EFFECTIVE_MACRO, RESOLUTION_MACRO
})
public @interface RequireCDIExtension {

	public static final String	VALUE_MACRO				= "${#value}";

	public static final String	EFFECTIVE_MACRO		= "${if;${size;${#effective}};effective:=${#effective}}";

	public static final String	RESOLUTION_MACRO	= "${if;${is;${#resolution};default};;resolution:=${#resolution}}";

	public static final String	VERSION_MACRO	= "${if;${size;${#version}};version=${#version}}";

	/**
	 * The name of the required extension.
	 */
	String value();

	/**
	 * The version of the required extension.
	 * <p>
	 * If not specified, the {@code version} directive is omitted from the
	 * requirement clause.
	 */
	String version() default "";

	/**
	 * The effective time of the {@code osgi.extender} requirement.
	 * <p>
	 * Specifies the time the {@code osgi.extender} requirements are available.
	 * The OSGi framework resolver only considers requirements without an
	 * effective directive or {@code effective:=resolve}. Requirements with
	 * other values for the effective directive can be considered by an external
	 * agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
	 */
	String effective() default "";

	/**
	 * The resolution policy of the {@code osgi.extender} requirement.
	 * <p>
	 * A mandatory requirement forbids the bundle to resolve when this
	 * requirement is not satisfied; an optional requirement allows a bundle to
	 * resolve even if this requirement is not satisfied.
	 * <p>
	 * If not specified, the {@code resolution} directive is omitted from the
	 * requirement clause.
	 */
	Resolution resolution() default Resolution.DEFAULT;

}
