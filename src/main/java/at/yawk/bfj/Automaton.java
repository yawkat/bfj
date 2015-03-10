package at.yawk.bfj;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author yawkat
 */
@ThreadSafe
public interface Automaton {
    void execute(IO io);
}
