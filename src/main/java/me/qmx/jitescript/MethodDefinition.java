/**
 *  Copyright 2012 Douglas Campos <qmx@qmx.me>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.qmx.jitescript;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Optional;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author qmx
 */
@SuppressWarnings("unchecked")
public class MethodDefinition {

    private final String           methodName;
    private final int              modifiers;
    private final String           signature;
    private final CodeBlock        methodBody;
    private       Optional<String> enclosingMethod;

    public MethodDefinition(String methodName, int modifiers, String signature, CodeBlock methodBody) {
        this.methodName = methodName;
        this.modifiers = modifiers;
        this.signature = signature;
        this.methodBody = methodBody;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getModifiers() {
        return modifiers;
    }

    public CodeBlock getMethodBody() {
        return methodBody;
    }

    public String getSignature() {
        return signature;
    }

    public MethodNode getMethodNode() {
        MethodNode method = new MethodNode(getModifiers(), getMethodName(), getSignature(), null, null);
        method.visibleAnnotations = new ArrayList<>();
        method.instructions.add(getMethodBody().getInstructionList());
        method.tryCatchBlocks.addAll(getMethodBody().getTryCatchBlockList());
        method.localVariables.addAll(getMethodBody().getLocalVariableList());
        method.visibleAnnotations.addAll(methodBody.getAnnotations().stream().map(AnnotationData::getNode).collect(toList()));
        return method;
    }

    @Override
    public String toString() {
        return methodName + ':' + signature;
    }
}
