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

import static net.bytebuddy.jar.asm.Opcodes.ASM4;

import org.osgi.annotation.bundle.Requirements;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.Type;
import net.bytebuddy.pool.TypePool;

public class RequirementsAnnotationRemover implements AsmVisitorWrapper {
	private final String requirementsDescriptor = Type.getDescriptor(Requirements.class);

	@Override
	public int mergeWriter(int flags) {
		return 0;
	}

	@Override
	public int mergeReader(int flags) {
		return 0;
	}

	@Override
	public ClassVisitor wrap(
		TypeDescription instrumentedType, ClassVisitor classVisitor, Implementation.Context implementationContext,
		TypePool typePool, FieldList<FieldDescription.InDefinedShape> fields, MethodList<?> methods,
		int writerFlags, int readerFlags) {

		return new ClassVisitor(ASM4, classVisitor) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (descriptor.equals(requirementsDescriptor)) {
					return null;
				}

				return super.visitAnnotation(descriptor, visible);
			}
		};
	}

}