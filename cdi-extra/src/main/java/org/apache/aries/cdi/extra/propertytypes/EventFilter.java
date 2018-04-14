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

package org.apache.aries.cdi.extra.propertytypes;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.service.cdi.annotations.ComponentPropertyType;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.annotations.RequireEventAdmin;

/**
 * Component Property Type for the {@link EventConstants#EVENT_FILTER} service
 * property of an {@link EventHandler} service.
 * <p>
 * This annotation can be used on an {@link EventHandler} component to declare
 * the value of the {@link EventConstants#EVENT_FILTER} service property.
 */
@ComponentPropertyType
@RequireEventAdmin
@Retention(RUNTIME)
@Target({FIELD, METHOD, TYPE})
public @interface EventFilter {
	/**
	 * Service property specifying the {@code Event} filter to an
	 * {@link EventHandler} service.
	 *
	 * @return The event filter.
	 * @see EventConstants#EVENT_FILTER
	 */
	String value();
}
