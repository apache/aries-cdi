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

import static java.util.Objects.requireNonNull;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 * Can be observed in an extension to customize an annotated type without
 * {@link org.osgi.service.cdi.annotations.Service} marker.
 */
public class ProcessPotentialService {
    private final ProcessAnnotatedType<?> processAnnotatedType;

    public ProcessPotentialService(final ProcessAnnotatedType<?> processAnnotatedType) {
        this.processAnnotatedType = requireNonNull(processAnnotatedType, "processAnnotatedType can't be null");
    }

    public ProcessAnnotatedType<?> getProcessAnnotatedType() {
        return processAnnotatedType;
    }

    public AnnotatedType<?> getAnnotatedType() {
        return processAnnotatedType.getAnnotatedType();
    }

    public AnnotatedTypeConfigurator<?> configureAnnotatedType() {
        return processAnnotatedType.configureAnnotatedType();
    }

    @Override
    public String toString() {
        return "ProcessPotentialService{processAnnotatedType=" + processAnnotatedType + '}';
    }
}
