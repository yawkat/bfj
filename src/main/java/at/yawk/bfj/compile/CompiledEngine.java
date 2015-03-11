/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj.compile;

import at.yawk.bfj.*;
import at.yawk.reflect.UncheckedReflectiveOperationException;
import at.yawk.reflect.Unsafes;
import java.io.IOException;
import java.util.stream.Collectors;
import javassist.*;
import javassist.bytecode.Bytecode;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Opcode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yawkat
 */
public class CompiledEngine implements Engine {
    @Getter private final Optimizer optimizer = new Optimizer();
    @Setter private int initialBufferSize = 1024;

    public MemoryProgram optimize(ProgramIterator iterator) {
        Expression expression = new ExpressionCompiler(iterator, optimizer).compile();
        return new MemoryProgram(expression.toInstructions().collect(Collectors.toList()));
    }

    @Override
    public Automaton produce(ProgramIterator iterator) {
        try {
            byte[] bytes = compile(iterator);
            Class<?> c = Unsafes.getUnsafe().defineAnonymousClass(CompiledEngine.class, bytes, null);
            return (Automaton) c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UncheckedReflectiveOperationException(e);
        }
    }

    /**
     * Compile the given program to java bytecode. The returned array represents a .class file.
     *
     * <ul>
     * <li>The returned class implements {@link at.yawk.bfj.Automaton}.</li>
     * <li>The returned class does not necessarily have a non-empty, unique name.</li>
     * <li>The returned class may be final.</li>
     * </ul>
     */
    public byte[] compile(ProgramIterator iterator) {
        try {
            return compile0(iterator);
        } catch (NotFoundException | IOException | CannotCompileException e) {
            throw new ParserException("Failed to compile", e);
        }
    }

    private byte[] compile0(ProgramIterator iterator) throws NotFoundException, IOException, CannotCompileException {
        Expression rootExpression = new ExpressionCompiler(iterator, optimizer).compile();

        ClassPool pool = ClassPool.getDefault();
        CtClass automatonClass = pool.getCtClass(Automaton.class.getName());
        CtMethod itfM = automatonClass.getDeclaredMethods()[0];

        CtClass ctClass = pool.makeClass("");
        ctClass.setInterfaces(new CtClass[]{ automatonClass });
        CtMethod method = new CtMethod(itfM.getReturnType(), itfM.getName(), itfM.getParameterTypes(), ctClass);
        method.setModifiers(Modifier.FINAL | Modifier.PUBLIC);
        Bytecode bytecode = new Bytecode(method.getMethodInfo().getConstPool());
        bytecode.addNewarray(Opcode.T_BYTE, initialBufferSize);
        bytecode.addAstore(Expression.VAR_TAPE);
        bytecode.addIconst(initialBufferSize / 2);
        bytecode.addIstore(Expression.VAR_TAPE_INDEX);
        rootExpression.compile(bytecode);
        bytecode.addReturn(null);
        CodeAttribute ca = bytecode.toCodeAttribute();
        ca.setMaxLocals(5);
        ca.setMaxStack(7);
        method.getMethodInfo().setCodeAttribute(ca);
        ctClass.addMethod(method);

        ctClass.getClassFile().setVersionToJava5();
        return ctClass.toBytecode();
    }
}
