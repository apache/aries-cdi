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
package org.apache.aries.cdi.extension.spi.adapt;

import java.util.Arrays;

import javax.enterprise.inject.spi.ProcessAnnotatedType;

public class MergeServiceTypes<T> {
    private final Class<?>[] types;
    private final ProcessAnnotatedType<T> processAnnotatedType;

    public MergeServiceTypes(final ProcessAnnotatedType<T> processAnnotatedType,
                             final Class<?>... types) {
        this.types = types;
        this.processAnnotatedType = processAnnotatedType;
    }

    public Class<?>[] getTypes() {
        return types;
    }

    public ProcessAnnotatedType<T> getProcessAnnotatedType() {
        return processAnnotatedType;
    }

    @Override
    public String toString() {
        return "MergeServiceTypes{types=" + Arrays.toString(types) + '}';
    }
}
