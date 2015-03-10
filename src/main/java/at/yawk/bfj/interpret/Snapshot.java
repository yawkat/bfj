package at.yawk.bfj.interpret;

import java.util.Arrays;
import lombok.Value;

/**
 * @author yawkat
 */
@Value
class Snapshot {
    int programPos;
    byte[] tape;
    int tapePos;

    public static Snapshot capture(int programPos, Tape tape) {
        int first = -1;
        int last = -1;
        for (int i = 0; i < tape.buf.length; i++) {
            if (tape.buf[i] != 0) {
                if (first == -1) { first = i; }
                last = i;
            }
        }
        byte[] buf;
        int tapePos;
        if (first != -1) {
            buf = Arrays.copyOfRange(tape.buf, first, last + 1);
            tapePos = tape.pos - first;
        } else {
            buf = new byte[0];
            // since the program has no knowledge of the tape position we can set this to 0 if the tape is completely
            // empty.
            tapePos = 0;
        }
        return new Snapshot(programPos, buf, tapePos);
    }
}
