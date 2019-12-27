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

/**
 * If fired (using the {@link javax.enterprise.inject.spi.BeanManager}) during a {@link ProcessAnnotatedType} event,
 * it will configure the annotated type to add {@link org.apache.aries.cdi.extension.spi.annotation.AdaptedService}
 * with the referenced types - potentially merged with already existing ones.
 */
public class MergeServiceTypes {
    private final Class<?>[] types;
    private final ProcessAnnotatedType<?> processAnnotatedType;

    private MergeServiceTypes(final ProcessAnnotatedType<?> processAnnotatedType,
                             final Class<?>... types) {
        this.types = types;
        this.processAnnotatedType = processAnnotatedType;
    }

    public Class<?>[] getTypes() {
        return types;
    }

    public ProcessAnnotatedType<?> getProcessAnnotatedType() {
        return processAnnotatedType;
    }

    @Override
    public String toString() {
        return "MergeServiceTypes{types=" + Arrays.toString(types) + '}';
    }

    public static Builder forEvent(final ProcessAnnotatedType<?> pat) {
        return new Builder(pat);
    }

    public static Builder forEvent(final ProcessPotentialService pat) {
        return new Builder(pat.getProcessAnnotatedType());
    }

    public static final class Builder {
        private final ProcessAnnotatedType<?> processAnnotatedType;
        private Class<?>[] types;

        private Builder(final ProcessAnnotatedType<?> processAnnotatedType) {
            if (processAnnotatedType == null) {
                throw new IllegalArgumentException("processAnnotatedType can't be null");
            }
            this.processAnnotatedType = processAnnotatedType;
        }

        public Builder withTypes(final Class<?>... types) {
            this.types = types;
            return this;
        }

        public MergeServiceTypes build() {
            if (types == null) {
                throw new IllegalArgumentException("No types set");
            }
            return new MergeServiceTypes(processAnnotatedType, types);
        }
    }
}
