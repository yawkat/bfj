package at.yawk.bfj.compile;

import at.yawk.bfj.Automaton;
import at.yawk.bfj.Engine;
import at.yawk.bfj.MemoryProgram;
import at.yawk.bfj.ProgramIterator;
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
            return produce0(iterator);
        } catch (NotFoundException | IOException | InstantiationException | CannotCompileException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Automaton produce0(ProgramIterator iterator)
            throws NotFoundException, IOException, CannotCompileException, InstantiationException,
                   IllegalAccessException {
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
        byte[] bytes = ctClass.toBytecode();

        Class<?> c = Unsafes.getUnsafe().defineAnonymousClass(CompiledEngine.class, bytes, null);
        return (Automaton) c.newInstance();
    }
}
