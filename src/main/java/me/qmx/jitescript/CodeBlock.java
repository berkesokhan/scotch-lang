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

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.params;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * @author qmx
 */
public class CodeBlock implements Opcodes {

    private boolean DEBUG = false;
    private InsnList instructionList = new InsnList();
    private List<TryCatchBlockNode> tryCatchBlockList = new ArrayList<TryCatchBlockNode>();
    private List<LocalVariableNode> localVariableList = new ArrayList<LocalVariableNode>();
    private List<VisibleAnnotation> annotations = new ArrayList<VisibleAnnotation>();
    private int arity = 0;
    private boolean returns = false;

    public CodeBlock() {
    }

    public CodeBlock(me.qmx.jitescript.CodeBlock block) {
        this.arity = block.arity();
        prepend(block);
    }

    public CodeBlock(int arity) {
        this.arity = arity;
    }

    public static me.qmx.jitescript.CodeBlock newCodeBlock() {
        return new me.qmx.jitescript.CodeBlock();
    }

    public static me.qmx.jitescript.CodeBlock newCodeBlock(int arity) {
        return new me.qmx.jitescript.CodeBlock(arity);
    }

    public static me.qmx.jitescript.CodeBlock newCodeBlock(me.qmx.jitescript.CodeBlock block) {
        return new me.qmx.jitescript.CodeBlock(block);
    }

    /**
     * Short-hand for specifying a set of aloads
     *
     * @param args list of aloads you want
     */
    public me.qmx.jitescript.CodeBlock aloadMany(int... args) {
        for (int arg : args) {
            aload(arg);
        }
        return this;
    }

    public me.qmx.jitescript.CodeBlock aload(int arg0) {
        this.instructionList.add(new VarInsnNode(ALOAD, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iload(int arg0) {
        this.instructionList.add(new VarInsnNode(ILOAD, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lload(int arg0) {
        this.instructionList.add(new VarInsnNode(LLOAD, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fload(int arg0) {
        this.instructionList.add(new VarInsnNode(FLOAD, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dload(int arg0) {
        this.instructionList.add(new VarInsnNode(DLOAD, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock astore(int arg0) {
        this.instructionList.add(new VarInsnNode(ASTORE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock istore(int arg0) {
        this.instructionList.add(new VarInsnNode(ISTORE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lstore(int arg0) {
        this.instructionList.add(new VarInsnNode(LSTORE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fstore(int arg0) {
        this.instructionList.add(new VarInsnNode(FSTORE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dstore(int arg0) {
        this.instructionList.add(new VarInsnNode(DSTORE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ldc(Object arg0) {
        this.instructionList.add(new LdcInsnNode(arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock bipush(int arg) {
        this.instructionList.add(new IntInsnNode(BIPUSH, arg));
        return this;
    }

    public me.qmx.jitescript.CodeBlock sipush(int arg) {
        this.instructionList.add(new IntInsnNode(SIPUSH, arg));
        return this;
    }

    public me.qmx.jitescript.CodeBlock pushInt(int value) {
        if (value <= Byte.MAX_VALUE && value >= Byte.MIN_VALUE) {
            switch (value) {
                case -1:
                    iconst_m1();
                    break;
                case 0:
                    iconst_0();
                    break;
                case 1:
                    iconst_1();
                    break;
                case 2:
                    iconst_2();
                    break;
                case 3:
                    iconst_3();
                    break;
                case 4:
                    iconst_4();
                    break;
                case 5:
                    iconst_5();
                    break;
                default:
                    bipush(value);
                    break;
            }
        } else if (value <= Short.MAX_VALUE && value >= Short.MIN_VALUE) {
            sipush(value);
        } else {
            ldc(value);
        }
        return this;
    }

    public me.qmx.jitescript.CodeBlock pushBoolean(boolean bool) {
        if (bool) {
            iconst_1();
        } else {
            iconst_0();
        }
        return this;
    }

    public me.qmx.jitescript.CodeBlock invokestatic(String arg1, String arg2, String arg3) {
        this.instructionList.add(new MethodInsnNode(INVOKESTATIC, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock invokespecial(String arg1, String arg2, String arg3) {
        this.instructionList.add(new MethodInsnNode(INVOKESPECIAL, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock invokevirtual(String arg1, String arg2, String arg3) {
        this.instructionList.add(new MethodInsnNode(INVOKEVIRTUAL, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock invokeinterface(String arg1, String arg2, String arg3) {
        this.instructionList.add(new MethodInsnNode(INVOKEINTERFACE, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock invokedynamic(String arg0, String arg1, Handle arg2, Object... arg3) {
        this.instructionList.add(new InvokeDynamicInsnNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock aprintln() {
        dup();
        getstatic(p(System.class), "out", ci(PrintStream.class));
        swap();
        invokevirtual(p(PrintStream.class), "println", sig(void.class, params(Object.class)));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iprintln() {
        dup();
        getstatic(p(System.class), "out", ci(PrintStream.class));
        swap();
        invokevirtual(p(PrintStream.class), "println", sig(void.class, params(int.class)));
        return this;
    }

    public me.qmx.jitescript.CodeBlock areturn() {
        this.returns = true;
        this.instructionList.add(new InsnNode(ARETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ireturn() {
        this.returns = true;
        this.instructionList.add(new InsnNode(IRETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock freturn() {
        this.returns = true;
        this.instructionList.add(new InsnNode(FRETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lreturn() {
        this.returns = true;
        this.instructionList.add(new InsnNode(LRETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dreturn() {
        this.returns = true;
        this.instructionList.add(new InsnNode(DRETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock newobj(String arg0) {
        this.instructionList.add(new TypeInsnNode(NEW, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup() {
        this.instructionList.add(new InsnNode(DUP));
        return this;
    }

    public me.qmx.jitescript.CodeBlock swap() {
        this.instructionList.add(new InsnNode(SWAP));
        return this;
    }

    public me.qmx.jitescript.CodeBlock swap2() {
        dup2_x2();
        pop2();
        return this;
    }

    public me.qmx.jitescript.CodeBlock getstatic(String arg1, String arg2, String arg3) {
        this.instructionList.add(new FieldInsnNode(GETSTATIC, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock putstatic(String arg1, String arg2, String arg3) {
        this.instructionList.add(new FieldInsnNode(PUTSTATIC, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock getfield(String arg1, String arg2, String arg3) {
        this.instructionList.add(new FieldInsnNode(GETFIELD, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock putfield(String arg1, String arg2, String arg3) {
        this.instructionList.add(new FieldInsnNode(PUTFIELD, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock voidreturn() {
        this.instructionList.add(new InsnNode(RETURN));
        return this;
    }

    public me.qmx.jitescript.CodeBlock anewarray(String arg0) {
        this.instructionList.add(new TypeInsnNode(ANEWARRAY, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock multianewarray(String arg0, int dims) {
        this.instructionList.add(new MultiANewArrayInsnNode(arg0, dims));
        return this;
    }

    public me.qmx.jitescript.CodeBlock newarray(int arg0) {
        this.instructionList.add(new IntInsnNode(NEWARRAY, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_m1() {
        this.instructionList.add(new InsnNode(ICONST_M1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_0() {
        this.instructionList.add(new InsnNode(ICONST_0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_1() {
        this.instructionList.add(new InsnNode(ICONST_1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_2() {
        this.instructionList.add(new InsnNode(ICONST_2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_3() {
        this.instructionList.add(new InsnNode(ICONST_3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_4() {
        this.instructionList.add(new InsnNode(ICONST_4));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iconst_5() {
        this.instructionList.add(new InsnNode(ICONST_5));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lconst_0() {
        this.instructionList.add(new InsnNode(LCONST_0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock aconst_null() {
        this.instructionList.add(new InsnNode(ACONST_NULL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock label(LabelNode labelNode) {
        this.instructionList.add(labelNode);
        return this;
    }

    public me.qmx.jitescript.CodeBlock nop() {
        this.instructionList.add(new InsnNode(NOP));
        return this;
    }

    public me.qmx.jitescript.CodeBlock pop() {
        this.instructionList.add(new InsnNode(POP));
        return this;
    }

    public me.qmx.jitescript.CodeBlock pop2() {
        this.instructionList.add(new InsnNode(POP2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock arrayload() {
        this.instructionList.add(new InsnNode(AALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock arraystore() {
        this.instructionList.add(new InsnNode(AASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iarrayload() {
        this.instructionList.add(new InsnNode(IALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock barrayload() {
        this.instructionList.add(new InsnNode(BALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock barraystore() {
        this.instructionList.add(new InsnNode(BASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock aaload() {
        this.instructionList.add(new InsnNode(AALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock aastore() {
        this.instructionList.add(new InsnNode(AASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iaload() {
        this.instructionList.add(new InsnNode(IALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iastore() {
        this.instructionList.add(new InsnNode(IASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock laload() {
        this.instructionList.add(new InsnNode(LALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lastore() {
        this.instructionList.add(new InsnNode(LASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock baload() {
        this.instructionList.add(new InsnNode(BALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock bastore() {
        this.instructionList.add(new InsnNode(BASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock saload() {
        this.instructionList.add(new InsnNode(SALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock sastore() {
        this.instructionList.add(new InsnNode(SASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock caload() {
        this.instructionList.add(new InsnNode(CALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock castore() {
        this.instructionList.add(new InsnNode(CASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock faload() {
        this.instructionList.add(new InsnNode(FALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fastore() {
        this.instructionList.add(new InsnNode(FASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock daload() {
        this.instructionList.add(new InsnNode(DALOAD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dastore() {
        this.instructionList.add(new InsnNode(DASTORE));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fcmpl() {
        this.instructionList.add(new InsnNode(FCMPL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fcmpg() {
        this.instructionList.add(new InsnNode(FCMPG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dcmpl() {
        this.instructionList.add(new InsnNode(DCMPL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dcmpg() {
        this.instructionList.add(new InsnNode(DCMPG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup_x2() {
        this.instructionList.add(new InsnNode(DUP_X2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup_x1() {
        this.instructionList.add(new InsnNode(DUP_X1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup2_x2() {
        this.instructionList.add(new InsnNode(DUP2_X2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup2_x1() {
        this.instructionList.add(new InsnNode(DUP2_X1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dup2() {
        this.instructionList.add(new InsnNode(DUP2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock trycatch(LabelNode arg0, LabelNode arg1, LabelNode arg2,
                              String arg3) {
        this.tryCatchBlockList.add(new TryCatchBlockNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock trycatch(String type, Runnable body, Runnable catchBody) {
        LabelNode before = new LabelNode();
        LabelNode after = new LabelNode();
        LabelNode catchStart = new LabelNode();
        LabelNode done = new LabelNode();

        trycatch(before, after, catchStart, type);
        label(before);
        body.run();
        label(after);
        go_to(done);
        if (catchBody != null) {
            label(catchStart);
            catchBody.run();
        }
        label(done);
        return this;
    }

    public me.qmx.jitescript.CodeBlock go_to(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(GOTO, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lookupswitch(LabelNode arg0, int[] arg1, LabelNode[] arg2) {
        this.instructionList.add(new LookupSwitchInsnNode(arg0, arg1, arg2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock athrow() {
        this.instructionList.add(new InsnNode(ATHROW));
        return this;
    }

    public me.qmx.jitescript.CodeBlock instance_of(String arg0) {
        this.instructionList.add(new TypeInsnNode(INSTANCEOF, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifeq(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFEQ, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iffalse(LabelNode arg0) {
        ifeq(arg0);
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifne(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFNE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iftrue(LabelNode arg0) {
        ifne(arg0);
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_acmpne(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ACMPNE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_acmpeq(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ACMPEQ, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmple(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPLE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmpgt(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPGT, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmplt(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPLT, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmpne(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPNE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmpeq(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPEQ, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock if_icmpge(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IF_ICMPGE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock checkcast(String arg0) {
        this.instructionList.add(new TypeInsnNode(CHECKCAST, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock line(int line) {
        visitLineNumber(line, new LabelNode());
        return this;
    }

    public me.qmx.jitescript.CodeBlock line(int line, LabelNode label) {
        visitLineNumber(line, label);
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifnonnull(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFNONNULL, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifnull(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFNULL, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iflt(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFLT, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifle(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFLE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifgt(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFGT, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ifge(LabelNode arg0) {
        this.instructionList.add(new JumpInsnNode(IFGE, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock arraylength() {
        this.instructionList.add(new InsnNode(ARRAYLENGTH));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ishr() {
        this.instructionList.add(new InsnNode(ISHR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ishl() {
        this.instructionList.add(new InsnNode(ISHL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iushr() {
        this.instructionList.add(new InsnNode(IUSHR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lshr() {
        this.instructionList.add(new InsnNode(LSHR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lshl() {
        this.instructionList.add(new InsnNode(LSHL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lushr() {
        this.instructionList.add(new InsnNode(LUSHR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lcmp() {
        this.instructionList.add(new InsnNode(LCMP));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iand() {
        this.instructionList.add(new InsnNode(IAND));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ior() {
        this.instructionList.add(new InsnNode(IOR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ixor() {
        this.instructionList.add(new InsnNode(IXOR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock land() {
        this.instructionList.add(new InsnNode(LAND));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lor() {
        this.instructionList.add(new InsnNode(LOR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lxor() {
        this.instructionList.add(new InsnNode(LXOR));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iadd() {
        this.instructionList.add(new InsnNode(IADD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ladd() {
        this.instructionList.add(new InsnNode(LADD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fadd() {
        this.instructionList.add(new InsnNode(FADD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dadd() {
        this.instructionList.add(new InsnNode(DADD));
        return this;
    }

    public me.qmx.jitescript.CodeBlock isub() {
        this.instructionList.add(new InsnNode(ISUB));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lsub() {
        this.instructionList.add(new InsnNode(LSUB));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fsub() {
        this.instructionList.add(new InsnNode(FSUB));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dsub() {
        this.instructionList.add(new InsnNode(DSUB));
        return this;
    }

    public me.qmx.jitescript.CodeBlock idiv() {
        this.instructionList.add(new InsnNode(IDIV));
        return this;
    }

    public me.qmx.jitescript.CodeBlock irem() {
        this.instructionList.add(new InsnNode(IREM));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ineg() {
        this.instructionList.add(new InsnNode(INEG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2d() {
        this.instructionList.add(new InsnNode(I2D));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2l() {
        this.instructionList.add(new InsnNode(I2L));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2f() {
        this.instructionList.add(new InsnNode(I2F));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2s() {
        this.instructionList.add(new InsnNode(I2S));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2c() {
        this.instructionList.add(new InsnNode(I2C));
        return this;
    }

    public me.qmx.jitescript.CodeBlock i2b() {
        this.instructionList.add(new InsnNode(I2B));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ldiv() {
        this.instructionList.add(new InsnNode(LDIV));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lrem() {
        this.instructionList.add(new InsnNode(LREM));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lneg() {
        this.instructionList.add(new InsnNode(LNEG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock l2d() {
        this.instructionList.add(new InsnNode(L2D));
        return this;
    }

    public me.qmx.jitescript.CodeBlock l2i() {
        this.instructionList.add(new InsnNode(L2I));
        return this;
    }

    public me.qmx.jitescript.CodeBlock l2f() {
        this.instructionList.add(new InsnNode(L2F));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fdiv() {
        this.instructionList.add(new InsnNode(FDIV));
        return this;
    }

    public me.qmx.jitescript.CodeBlock frem() {
        this.instructionList.add(new InsnNode(FREM));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fneg() {
        this.instructionList.add(new InsnNode(FNEG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock f2d() {
        this.instructionList.add(new InsnNode(F2D));
        return this;
    }

    public me.qmx.jitescript.CodeBlock f2i() {
        this.instructionList.add(new InsnNode(F2D));
        return this;
    }

    public me.qmx.jitescript.CodeBlock f2l() {
        this.instructionList.add(new InsnNode(F2L));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ddiv() {
        this.instructionList.add(new InsnNode(DDIV));
        return this;
    }

    public me.qmx.jitescript.CodeBlock drem() {
        this.instructionList.add(new InsnNode(DREM));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dneg() {
        this.instructionList.add(new InsnNode(DNEG));
        return this;
    }

    public me.qmx.jitescript.CodeBlock d2f() {
        this.instructionList.add(new InsnNode(D2F));
        return this;
    }

    public me.qmx.jitescript.CodeBlock d2i() {
        this.instructionList.add(new InsnNode(D2I));
        return this;
    }

    public me.qmx.jitescript.CodeBlock d2l() {
        this.instructionList.add(new InsnNode(D2L));
        return this;
    }

    public me.qmx.jitescript.CodeBlock imul() {
        this.instructionList.add(new InsnNode(IMUL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock lmul() {
        this.instructionList.add(new InsnNode(LMUL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock fmul() {
        this.instructionList.add(new InsnNode(FMUL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock dmul() {
        this.instructionList.add(new InsnNode(DMUL));
        return this;
    }

    public me.qmx.jitescript.CodeBlock iinc(int arg0, int arg1) {
        this.instructionList.add(new IincInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock monitorenter() {
        this.instructionList.add(new InsnNode(MONITORENTER));
        return this;
    }

    public me.qmx.jitescript.CodeBlock monitorexit() {
        this.instructionList.add(new InsnNode(MONITOREXIT));
        return this;
    }

    public me.qmx.jitescript.CodeBlock jsr(LabelNode branch) {
        this.instructionList.add(new JumpInsnNode(JSR, branch));
        return this;
    }

    public me.qmx.jitescript.CodeBlock ret(int arg0) {
        this.instructionList.add(new IntInsnNode(RET, arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitInsn(int arg0) {
        this.instructionList.add(new InsnNode(arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitIntInsn(int arg0, int arg1) {
        this.instructionList.add(new IntInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitInsnNode(int arg0, int arg1) {
        this.instructionList.add(new IntInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitTypeInsn(int arg0, String arg1) {
        this.instructionList.add(new TypeInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
        this.instructionList.add(new FieldInsnNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
        this.instructionList.add(new MethodInsnNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitInvokeDynamicInsn(String arg0, String arg1, Handle arg2, Object... arg3) {
        this.instructionList.add(new InvokeDynamicInsnNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitJumpInsn(int arg0, LabelNode arg1) {
        this.instructionList.add(new JumpInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitLabel(Label arg0) {
        this.instructionList.add(new LabelNode(arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitLdcInsn(Object arg0) {
        this.instructionList.add(new LdcInsnNode(arg0));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitIincInsn(int arg0, int arg1) {
        this.instructionList.add(new IincInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitTableSwitchInsn(int arg0, int arg1, LabelNode arg2,
                                          LabelNode[] arg3) {
        this.instructionList.add(new TableSwitchInsnNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitLookupSwitchInsn(LabelNode arg0, int[] arg1, LabelNode[] arg2) {
        this.instructionList.add(new LookupSwitchInsnNode(arg0, arg1, arg2));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitMultiANewArrayInsn(String arg0, int arg1) {
        this.instructionList.add(new MultiANewArrayInsnNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitTryCatchBlock(LabelNode arg0, LabelNode arg1, LabelNode arg2,
                                        String arg3) {
        this.tryCatchBlockList.add(new TryCatchBlockNode(arg0, arg1, arg2, arg3));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitLocalVariable(String arg0, String arg1, String arg2,
                                        LabelNode arg3, LabelNode arg4, int arg5) {
        this.localVariableList.add(new LocalVariableNode(arg0, arg1, arg2, arg3, arg4, arg5));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitLineNumber(int arg0, LabelNode arg1) {
        this.instructionList.add(new LineNumberNode(arg0, arg1));
        return this;
    }

    public me.qmx.jitescript.CodeBlock tableswitch(int min, int max, LabelNode defaultLabel, LabelNode[] cases) {
        this.instructionList.add(new TableSwitchInsnNode(min, max, defaultLabel, cases));
        return this;
    }

    public me.qmx.jitescript.CodeBlock visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
        this.instructionList.add(new FrameNode(arg0, arg1, arg2, arg3, arg4));
        return this;
    }

    public InsnList getInstructionList() {
        return instructionList;
    }

    public List<TryCatchBlockNode> getTryCatchBlockList() {
        return tryCatchBlockList;
    }

    public List<LocalVariableNode> getLocalVariableList() {
        return localVariableList;
    }

    public List<VisibleAnnotation> getAnnotations() {
        return annotations;
    }

    /**
     * adds a compressed frame to the stack
     *
     * @param stackArguments the argument types on the stack, represented as "class path names" e.g java/lang/RuntimeException
     */
    public me.qmx.jitescript.CodeBlock frame_same(Object... stackArguments) {
        int type;
        if (stackArguments.length == 0) {
            type = F_SAME;
        } else if (stackArguments.length == 1) {
            type = F_SAME1;
        } else {
            throw new IllegalArgumentException("same frame should have 0 or 1 arguments on stack");
        }
        this.instructionList.add(new FrameNode(type, 0, null, stackArguments.length, stackArguments));
        return this;
    }

    public me.qmx.jitescript.CodeBlock prepend(me.qmx.jitescript.CodeBlock codeBlock) {
        if (codeBlock.returns()) {
            this.returns = true;
        }
        this.annotations.addAll(codeBlock.getAnnotations());
        this.getInstructionList().insert(codeBlock.getInstructionList());
        return this;
    }

    public me.qmx.jitescript.CodeBlock append(me.qmx.jitescript.CodeBlock codeBlock) {
        if (codeBlock.returns()) {
            this.returns = true;
        }
        this.getInstructionList().add(codeBlock.getInstructionList());
        this.tryCatchBlockList.addAll( codeBlock.getTryCatchBlockList() );
        this.annotations.addAll(codeBlock.getAnnotations());
        return this;
    }

    public me.qmx.jitescript.CodeBlock lambda(JiteClass jiteClass, LambdaBlock lambda) {
        lambda.apply(jiteClass, this);
        return this;
    }

    public VisibleAnnotation annotation(Class<?> type) {
        VisibleAnnotation annotation = new VisibleAnnotation(ci(type));
        addAnnotation(annotation);
        return annotation;
    }

    public me.qmx.jitescript.CodeBlock addAnnotation(VisibleAnnotation annotation) {
        annotations.add(annotation);
        return this;
    }

    public int arity() {
        return this.arity;
    }

    public boolean returns() {
        return returns;
    }
}
