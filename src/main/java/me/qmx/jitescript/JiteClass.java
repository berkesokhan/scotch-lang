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

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static me.qmx.jitescript.CodeBlock.*;
import static me.qmx.jitescript.util.CodegenUtils.*;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * Represents a Java Class
 *
 * @author qmx
 */
@SuppressWarnings("unchecked")
public class JiteClass implements Opcodes {

    public static final String[] INTERFACES = new String[]{};
    private final List<MethodDefinition> methods = new ArrayList<>();
    private final List<FieldDefinition> fields = new ArrayList<>();
    private final List<String> interfaces = new ArrayList<>();
    private final List<AnnotationData> annotations = new ArrayList<>();
    private final List<ChildEntry> childClasses = new ArrayList<>();
    private final String className;
    private final String superClassName;
    private String sourceFile;
    private String sourceDebug;
    private int access = ACC_PUBLIC;
    private String parentClassName;
    private int nextId;

    /**
     * Creates a new class representation
     *
     * @param className the desired class name
     */
    public JiteClass(String className) {
        this(className, INTERFACES);
    }

    /**
     * Creates a new class representation
     *
     * @param className  the desired class name
     * @param interfaces the desired java interfaces this class will implement
     */
    public JiteClass(String className, String[] interfaces) {
        this(className, p(Object.class), interfaces);
    }

    /**
     * Creates a new class representation
     *
     * @param className      the desired class name
     * @param superClassName the desired parent class
     * @param interfaces     the desired java interfaces this class will implement
     */
    public JiteClass(String className, String superClassName, String[] interfaces) {
        this.className = className;
        this.superClassName = superClassName;
        addAll(this.interfaces, interfaces);
    }

    public int getAccess() {
        return access;
    }

    public String getClassName() {
        return className;
    }

    public String getParentClassName() {
        return parentClassName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public String reserveLambda() {
        return "lambda$" + nextId++;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public void setParentClassName(String parentClassName) {
        this.parentClassName = parentClassName;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setSourceDebug(String sourceDebug) {
        this.sourceDebug = sourceDebug;
    }

    public void addChildClass(JiteClass child) {
        String childName = child.getClassName();
        if (childName.contains("$")) {
            childName = childName.substring(childName.lastIndexOf('$') + 1);
        } else {
            childName = childName.substring(childName.lastIndexOf('/') + 1);
        }
        addChildClass(childName.substring(childName.lastIndexOf('$') + 1), child);
    }

    public void addChildClass(String innerName, JiteClass child) {
        child.setParentClassName(getClassName());
        childClasses.add(new ChildEntry(innerName, child));
    }

    public List<JiteClass> getChildClasses() {
        return childClasses.stream().map(ChildEntry::getJiteClass).collect(toList());
    }

    /**
     * Defines a new method on the target class
     *
     * @param methodName the method name
     * @param modifiers  the modifier bitmask, made by OR'ing constants from ASM's {@link org.objectweb.asm.Opcodes} interface
     * @param signature  the method signature, on standard JVM notation
     * @param methodBody the method body
     */
    public void defineMethod(String methodName, int modifiers, String signature, CodeBlock methodBody) {
        addMethod(new MethodDefinition(methodName, modifiers, signature, methodBody));
    }

    public void addMethod(MethodDefinition methodDefinition) {
        this.methods.add(methodDefinition);
    }

    /**
     * Defines a new field on the target class
     *
     * @param fieldName the field name
     * @param modifiers  the modifier bitmask, made by OR'ing constants from ASM's {@link org.objectweb.asm.Opcodes} interface
     * @param signature  the field signature, on standard JVM notation
     * @param value the default value (null for JVM default)
     * @return the new field definition for further modification
     */
    public FieldDefinition defineField(String fieldName, int modifiers, String signature, Object value) {
        FieldDefinition field = new FieldDefinition(fieldName, modifiers, signature, value);
        this.fields.add(field);
        return field;
    }

    public void addField(FieldDefinition field) {
        this.fields.add(field);
    }

    /**
     * Defines a default constructor on the target class
     */
    public void defineDefaultConstructor() {
        defineDefaultConstructor(ACC_PUBLIC);
    }

    public void defineDefaultConstructor(int access) {
        defineMethod("<init>", access, sig(void.class),
            newCodeBlock()
                .aload(0)
                .invokespecial(superClassName, "<init>", sig(void.class))
                .voidreturn()
        );
    }

    /**
     * Convert this class representation to JDK bytecode
     *
     * @return the bytecode representation
     */
    public byte[] toBytes() {
        return toBytes(JDKVersion.V1_8);
    }

    public void addAnnotation(AnnotationData annotation) {
        annotations.add(annotation);
    }

    public AnnotationData annotate(Class<?> type) {
        AnnotationData annotation = new AnnotationData(ci(type));
        addAnnotation(annotation);
        return annotation;
    }

    /**
     * Convert this class representation to JDK bytecode
     *
     * @param version the desired JDK version
     * @return the bytecode representation of this class
     */
    public byte[] toBytes(ClassWriter writer, JDKVersion version) {
        ClassNode node = new ClassNode();
        node.version = version.getVer();
        node.access = this.access;
        if ((node.access & ACC_INTERFACE) == 0) {
            node.access |= ACC_SUPER;
        }
        node.name = this.className;
        node.superName = this.superClassName;
        node.sourceFile = this.sourceFile;
        node.sourceDebug = this.sourceDebug;

        if (parentClassName != null) {
            node.visitOuterClass(parentClassName, null, null);
        }

        for (ChildEntry child : childClasses) {
            node.visitInnerClass(child.getClassName(), className, child.getInnerName(), child.getAccess());
        }

        if (!this.interfaces.isEmpty()) {
            node.interfaces.addAll(this.interfaces);
        }

        node.methods.addAll(methods.stream().map(MethodDefinition::getMethodNode).collect(toList()));
        node.fields.addAll(fields.stream().map(FieldDefinition::getFieldNode).collect(toList()));

        if (node.visibleAnnotations == null) {
            node.visibleAnnotations = new ArrayList<AnnotationNode>();
        }

        node.visibleAnnotations.addAll(annotations.stream().map(AnnotationData::getNode).collect(toList()));

        node.accept(writer);
        return writer.toByteArray();
    }

    public byte[] toBytes(JDKVersion version) {
        return toBytes(new ClassWriter(ClassWriter.COMPUTE_FRAMES), version);
    }

    private static final class ChildEntry {

        public final String innerName;
        public final JiteClass jiteClass;

        public ChildEntry(String innerName, JiteClass jiteClass) {
            this.innerName = innerName;
            this.jiteClass = jiteClass;
        }

        public int getAccess() {
            return jiteClass.getAccess();
        }

        public String getClassName() {
            return jiteClass.getClassName();
        }

        public String getInnerName() {
            return innerName;
        }

        public JiteClass getJiteClass() {
            return jiteClass;
        }
    }
}
