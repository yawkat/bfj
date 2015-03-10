package at.yawk.bfj.compile;

import at.yawk.bfj.IO;
import at.yawk.bfj.Instruction;
import java.util.List;
import java.util.stream.Stream;
import javassist.bytecode.Bytecode;
import javassist.bytecode.Opcode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * @author yawkat
 */
interface Expression {
    static final int VAR_TAPE = 2;
    static final int VAR_TAPE_INDEX = 3;
    static final int VAR_IO = 1;

    void compile(Bytecode bytecode);

    Stream<Instruction> toInstructions();

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    class Root implements Expression {
        private final List<Expression> expressions;

        @Override
        public void compile(Bytecode bytecode) {
            for (Expression expression : expressions) {
                assert bytecode.getStackDepth() == 0 : expression;
                //bytecode.addPrintln("Run " + expression);
                expression.compile(bytecode);
                assert bytecode.getStackDepth() == 0 : expression;
            }
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return expressions.stream().flatMap(Expression::toInstructions);
        }

        @Override
        public String toString() {
            if (expressions.isEmpty()) {
                return "{}";
            } else {
                StringBuilder builder = new StringBuilder();
                for (Expression expression : expressions) {
                    for (String line : expression.toString().split("\n")) {
                        builder.append(line).append('\n');
                    }
                }
                builder.setLength(builder.length() - 1);
                return builder.toString();
            }
        }
    }

    @Value
    class Increment implements Expression {
        private final short delta;

        @Override
        public void compile(Bytecode bytecode) {
            // stack: [

            // retrieve array and index
            bytecode.addAload(VAR_TAPE);
            bytecode.addIload(VAR_TAPE_INDEX);

            bytecode.addOpcode(Opcode.DUP2);

            // stack: [tape, ti, tape, ti

            // get value
            bytecode.addOpcode(Opcode.BALOAD);
            // get delta
            bytecode.addIconst(delta);
            // add delta + value and cast to byte
            bytecode.addOpcode(Opcode.IADD);
            bytecode.addOpcode(Opcode.I2B);

            // store into array again
            bytecode.addOpcode(Opcode.BASTORE);
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return Stream.generate(() -> delta > 0 ? Instruction.INCREMENT : Instruction.DECREMENT)
                    .limit(Math.abs(delta));
        }

        @Override
        public String toString() {
            return (delta < 0 ? "-" : "+") + Math.abs(delta);
        }

    }

    @Value
    class Move implements Expression {
        private final short delta;

        @Override
        public void compile(Bytecode bytecode) {
            assert bytecode.getStackDepth() == 0;

            bytecode.addIconst(delta);
            bytecode.addIload(VAR_TAPE_INDEX);
            bytecode.addOpcode(Opcode.IADD);
            // stack: [pos

            if (delta < 0) {
                // stack: [pos
                bytecode.addOpcode(Opcode.DUP);
                // stack: [pos, pos

                // if we're >= 0, skip the following resize
                bytecode.addOpcode(Opcode.IFGE);
                bytecode.addIndex(27); // jump to after GOTO below

                // stack: [pos
                bytecode.addAload(VAR_TAPE);
                // stack: [pos, old_arr
                bytecode.addOpcode(Opcode.DUP);
                bytecode.addOpcode(Opcode.ARRAYLENGTH);
                // stack: [pos, old_arr, old_len
                assert bytecode.getStackDepth() == 3;
                bytecode.addOpcode(Opcode.SWAP);
                bytecode.addOpcode(Opcode.DUP);
                bytecode.addOpcode(Opcode.ARRAYLENGTH);
                // stack: [pos, old_len, old_arr, old_len
                assert bytecode.getStackDepth() == 4;
                bytecode.addIconst(0);
                bytecode.addOpcode(Opcode.SWAP);
                // stack: [pos, old_len, old_arr, 0, old_len
                assert bytecode.getStackDepth() == 5;
                bytecode.addOpcode(Opcode.DUP);
                bytecode.addIconst(1);
                bytecode.addOpcode(Opcode.ISHL);
                // stack: [pos, old_len, old_arr, 0, old_len, new_len
                assert bytecode.getStackDepth() == 6;
                bytecode.addOpcode(Opcode.NEWARRAY);
                bytecode.add(Opcode.T_BYTE);
                // stack: [pos, old_len, old_arr, 0, old_len, new_arr
                assert bytecode.getStackDepth() == 6;
                bytecode.addOpcode(Opcode.DUP);
                bytecode.addAstore(VAR_TAPE); // store new array to tape variable
                // stack: [pos, old_len, old_arr, 0, old_len, new_arr
                assert bytecode.getStackDepth() == 6;
                bytecode.addOpcode(Opcode.SWAP);
                bytecode.addOpcode(Opcode.DUP);
                // stack: [pos, old_len, old_arr, 0, new_arr, old_len, old_len
                assert bytecode.getStackDepth() == 7;
                // copy old contents
                bytecode.addInvokestatic("java.lang.System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
                // stack: [pos, old_len
                bytecode.addOpcode(Opcode.IADD);
                // stack: [new_pos
                assert bytecode.getStackDepth() == 1;

                bytecode.addOpcode(Opcode.GOTO);
                bytecode.addIndex(-25); // jump back to DUP just before IFGE
            } else {
                // stack: [pos
                bytecode.addOpcode(Opcode.DUP);
                // stack: [pos, pos

                bytecode.addAload(VAR_TAPE);
                bytecode.addOpcode(Opcode.ARRAYLENGTH);
                // stack: [pos, pos, old_len
                assert bytecode.getStackDepth() == 3;

                bytecode.addOpcode(Opcode.IF_ICMPLT);
                bytecode.addIndex(15); // todo: jump to after resize

                // stack: [pos
                bytecode.addAload(VAR_TAPE);
                bytecode.addOpcode(Opcode.DUP);
                bytecode.addOpcode(Opcode.ARRAYLENGTH);
                // stack: [pos, old_arr, old_len
                assert bytecode.getStackDepth() == 3;
                bytecode.addIconst(1);
                bytecode.addOpcode(Opcode.ISHL);
                // stack: [pos, old_arr, new_len
                assert bytecode.getStackDepth() == 3;
                bytecode.addInvokestatic("java.util.Arrays", "copyOf", "([BI)[B");
                // stack: [pos, new_arr
                bytecode.addAstore(VAR_TAPE); // store new array to tape variable
                // stack: [pos
                assert bytecode.getStackDepth() == 1;
                bytecode.addOpcode(Opcode.GOTO);
                bytecode.addIndex(-15); // jump to DUP just before IF_ICMPLT
            }

            // stack: [new_pos
            //bytecode.addOpcode(Opcode.DUP);
            bytecode.addIstore(VAR_TAPE_INDEX);
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return Stream.generate(() -> delta > 0 ? Instruction.MOVE_RIGHT : Instruction.MOVE_LEFT)
                    .limit(Math.abs(delta));
        }

        @Override
        public String toString() {
            return (delta < 0 ? "<" : ">") + Math.abs(delta);
        }

    }

    @Value
    class Out implements Expression {
        @Override
        public void compile(Bytecode bytecode) {
            // load IO object
            bytecode.addAload(VAR_IO);
            // load item from tape
            bytecode.addAload(VAR_TAPE);
            bytecode.addIload(VAR_TAPE_INDEX);
            bytecode.addOpcode(Opcode.BALOAD);
            // invoke write(byte)
            bytecode.addInvokeinterface(IO.class.getName(), "write", "(B)V", 2);
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return Stream.of(Instruction.OUTPUT);
        }

        @Override
        public String toString() {
            return "->";
        }
    }

    @Value
    class In implements Expression {
        @Override
        public void compile(Bytecode bytecode) {
            // load tape for use in BASTORE
            bytecode.addAload(VAR_TAPE);
            bytecode.addIload(VAR_TAPE_INDEX);

            // load IO object
            bytecode.addAload(VAR_IO);
            // invoke read()
            bytecode.addInvokeinterface(IO.class.getName(), "read", "()B", 1);
            // store item to tape
            bytecode.addOpcode(Opcode.BASTORE);
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return Stream.of(Instruction.INPUT);
        }

        @Override
        public String toString() {
            return "<-";
        }
    }

    class Loop extends Root {
        public Loop(List<Expression> expressions) {
            super(expressions);
        }

        @Override
        public void compile(Bytecode bytecode) {
            Bytecode blockContent = new Bytecode(bytecode.getConstPool());
            super.compile(blockContent);

            // load item from tape
            bytecode.addAload(VAR_TAPE);
            bytecode.addIload(VAR_TAPE_INDEX);
            bytecode.addOpcode(Opcode.BALOAD);
            if (blockContent.getSize() < Short.MAX_VALUE - 10) {
                // if 0, jump to after loop
                bytecode.addOpcode(Opcode.IFEQ);
                bytecode.addIndex(blockContent.getSize() + 6);

                byte[] content = blockContent.get();
                for (byte b : content) {
                    bytecode.add(b);
                }

                bytecode.addOpcode(Opcode.GOTO);
                bytecode.addIndex(-blockContent.getSize() - 6);
            } else {
                // if 0, jump to after loop
                bytecode.addOpcode(Opcode.WIDE);
                bytecode.addOpcode(Opcode.IFEQ);
                bytecode.add32bit(blockContent.getSize() + 8);

                byte[] content = blockContent.get();
                for (byte b : content) {
                    bytecode.add(b);
                }

                bytecode.addOpcode(Opcode.WIDE);
                bytecode.addOpcode(Opcode.GOTO);
                bytecode.addIndex(-blockContent.getSize() - 8);
            }
        }

        @Override
        public Stream<Instruction> toInstructions() {
            return Stream.concat(
                    Stream.concat(
                            Stream.of(Instruction.LOOP_START),
                            super.toInstructions()
                    ),
                    Stream.of(Instruction.LOOP_END)
            );
        }

        @Override
        public String toString() {
            if (!getExpressions().isEmpty()) {
                StringBuilder builder = new StringBuilder("{\n");
                for (Expression expression : getExpressions()) {
                    for (String line : expression.toString().split("\n")) {
                        builder.append("  ").append(line).append('\n');
                    }
                }
                return builder.append("}").toString();
            }
            return super.toString();
        }
    }
}
