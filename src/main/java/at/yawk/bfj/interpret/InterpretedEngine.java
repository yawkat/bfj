package at.yawk.bfj.interpret;

import at.yawk.bfj.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

/**
 * @author yawkat
 */
public class InterpretedEngine implements Engine {
    @Setter private boolean printProgress = false;
    @Setter private boolean detectInfiniteLoops = false;

    @Override
    public Automaton produce(ProgramIterator iterator) {
        return new InterpretedAutomaton(dump(iterator), printProgress, detectInfiniteLoops);
    }

    private MemoryProgram dump(ProgramIterator iterator) {
        List<Instruction> instructions = new ArrayList<>();
        while (true) {
            Instruction instruction = iterator.next();
            if (instruction == null) { break; }
            instructions.add(instruction);
        }
        return new MemoryProgram(instructions);
    }
}
