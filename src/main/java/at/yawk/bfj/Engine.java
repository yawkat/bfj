package at.yawk.bfj;

/**
 * @author yawkat
 */
public interface Engine {
    Automaton produce(ProgramIterator iterator);
}
