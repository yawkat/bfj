package at.yawk.bfj;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public abstract class BufferedProgramIterator implements ProgramIterator {
    private List<Instruction> backlog = new ArrayList<>();
    private int pos = 0;
    private boolean record = false;
    private boolean hitEnd = false;

    @Nullable
    protected abstract Instruction retrieveNext();

    @Nullable
    @Override
    public Instruction next() {
        if (!record || pos >= backlog.size()) {
            if (hitEnd) {
                return null;
            }
            Instruction instruction = retrieveNext();
            if (instruction == null) {
                hitEnd = true;
                return null;
            }
            if (record) {
                backlog.add(instruction);
                pos++;
            } else {
            }
            return instruction;
        } else {
            return backlog.get(pos++);
        }
    }

    @Override
    public int position() {
        return pos;
    }

    @Override
    public void seek(int pos) {
        // == backlog.size is acceptable, we'll just continue in that case
        if (pos < 0 || pos > backlog.size()) {
            throw new ParserException("Seek position out of range (did you call mark first?)");
        }
        this.pos = pos;
    }

    @Override
    public void mark() {
        record = true;
    }
}
