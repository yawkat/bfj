/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @author yawkat
 */
@ThreadSafe
@EqualsAndHashCode(of = "instructions")
public class MemoryProgram {
    @Getter
    private final List<Instruction> instructions;
    private Boolean valid = null;

    public MemoryProgram(List<Instruction> instructions) {
        this.instructions = Collections.unmodifiableList(new ArrayList<>(instructions));
    }

    public boolean isValid() {
        if (valid == null) {
            // if this is called concurrently validate() could be called twice but that should be fine.
            valid = validate();
        }
        return valid;
    }

    private boolean validate() {
        int loopDepth = 0;
        for (Instruction instruction : instructions) {
            switch (instruction) {
            case LOOP_START:
                loopDepth++;
                break;
            case LOOP_END:
                loopDepth--;
                if (loopDepth < 0) {
                    return false;
                }
                break;
            }
        }
        // don't allow unclosed loops
        return loopDepth == 0;
    }

    @Override
    public String toString() {
        char[] chars = new char[instructions.size()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = instructions.get(i).getOperator();
        }
        return new String(chars);
    }

    public ProgramIterator iterator() {
        return new MemoryProgramIterator();
    }

    private class MemoryProgramIterator implements ProgramIterator {
        int pos = 0;

        @Nullable
        @Override
        public Instruction next() {
            if (pos >= instructions.size()) {
                return null;
            }
            return instructions.get(pos++);
        }

        @Override
        public int position() {
            return pos;
        }

        @Override
        public void seek(int pos) {
            this.pos = pos;
        }

        @Override
        public void mark() {
            // no action since we have all instructions in memory
        }
    }
}
