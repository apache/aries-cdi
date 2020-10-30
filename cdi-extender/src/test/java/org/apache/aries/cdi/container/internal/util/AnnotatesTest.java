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
package org.apache.aries.cdi.container.internal.util;

import org.apache.aries.cdi.container.internal.annotated.AnnotatedTypeImpl;
import org.junit.Test;
import org.osgi.service.cdi.annotations.Service;

import javax.enterprise.context.ApplicationScoped;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AnnotatesTest {
    @Test
    public void isBeanDefining() {
        assertFalse(Annotates.hasBeanDefiningAnnotations(new AnnotatedTypeImpl<>(A.class)));
        assertTrue(Annotates.hasBeanDefiningAnnotations(new AnnotatedTypeImpl<>(Single.class)));
    }

    @ApplicationScoped
    public static class Single {
    }

    @Service
    public static class A {
    }
}
