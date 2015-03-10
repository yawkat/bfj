package at.yawk.bfj.interpret;

import at.yawk.bfj.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
final class InterpretedAutomaton implements Automaton {
    private final MemoryProgram program;
    private final boolean logProgress;
    private final boolean detectInfiniteLoops;

    @Override
    public void execute(IO io) {
        int[] loopStartStack = new int[16];
        int loopDepth = 0;
        Tape tape = new Tape();
        Set<Snapshot> visitedStates = detectInfiniteLoops ? new HashSet<>() : null;

        ProgramIterator iterator = program.iterator();

        programLoop:
        while (true) {
            Instruction instruction = iterator.next();
            if (instruction == null) { break; }
            if (logProgress) {
                System.err.println(tape + " <- " + instruction);
            }
            // if loop depth is 0, we are in code that can never be revisited so we don't need to capture it.
            if (detectInfiniteLoops && loopDepth != 0) {
                // since we are in a loop, we can call .position() without undefined behaviour.
                if (!visitedStates.add(Snapshot.capture(iterator.position(), tape))) {
                    throw new ParserException("Infinite loop at " + tape + " <- " + instruction);
                }
            }
            switch (instruction) {
            case INCREMENT:
                tape.set((byte) (tape.get() + 1));
                break;
            case DECREMENT:
                tape.set((byte) (tape.get() - 1));
                break;
            case MOVE_LEFT:
                tape.pos--;
                break;
            case MOVE_RIGHT:
                tape.pos++;
                break;
            case LOOP_START:
                if (loopDepth >= loopStartStack.length) {
                    loopStartStack = Arrays.copyOf(loopStartStack, loopStartStack.length * 2);
                }
                if (tape.get() == 0) {
                    int nestedDepth = 1;
                    while (nestedDepth != 0) {
                        Instruction nested = iterator.next();
                        if (nested == null) {
                            break programLoop;
                        }
                        if (nested == Instruction.LOOP_END) {
                            nestedDepth--;
                        } else if (nested == Instruction.LOOP_START) {
                            nestedDepth++;
                        }
                    }
                } else {
                    iterator.mark();
                    loopStartStack[loopDepth] = iterator.position();
                    loopDepth++;
                }
                break;
            case LOOP_END:
                loopDepth--;
                if (loopDepth < 0) {
                    throw new ParserException("Missing loop start for loop end instruction");
                }
                if (tape.get() != 0) {
                    // this seeks to just after the loop start instruction
                    iterator.seek(loopStartStack[loopDepth++]);
                }
                break;
            case OUTPUT:
                io.write(tape.get());
                break;
            case INPUT:
                tape.set(io.read());
                break;
            }
        }
    }
}
