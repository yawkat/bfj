package at.yawk.bfj.compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Setter;

/**
 * @author yawkat
 */
@Setter
public class Optimizer {
    private boolean joinIncrements = true;
    private boolean joinMoves = true;
    private boolean removeEmptyIncrements = true;
    private boolean removeEmptyMoves = true;
    private boolean removeEmptyLoops = true;
    private int controlFlowTapeSize = 256;
    private int controlFlowTapeLimit = 256;

    Expression optimize(Expression expression) {
        expression = collapseDeep(expression);
        expression = collapseControlFlow(expression);
        return expression;
    }

    private Expression collapseDeep(Expression expression) {
        if (expression instanceof Expression.Root) {
            List<Expression> expressions = ((Expression.Root) expression).getExpressions();
            List<Expression> then = expressions.stream()
                    .map(this::collapseDeep)
                    .collect(Collectors.toList());
            collapseShallow(then);
            if (expression instanceof Expression.Loop) {
                return new Expression.Loop(then);
            } else {
                return new Expression.Root(then);
            }
        }
        return expression;
    }

    private void collapseShallow(List<Expression> expressions) {
        for (int i = 0; i < expressions.size(); i++) {
            Expression a = expressions.get(i);
            if (removeEmptyIncrements) {
                if (a instanceof Expression.Increment && ((Expression.Increment) a).getDelta() == 0) {
                    expressions.remove(i);
                    continue;
                }
            }
            if (removeEmptyMoves) {
                if (a instanceof Expression.Move && ((Expression.Move) a).getDelta() == 0) {
                    expressions.remove(i);
                    continue;
                }
            }
            if (removeEmptyLoops) {
                if (a instanceof Expression.Loop && ((Expression.Loop) a).getExpressions().isEmpty()) {
                    expressions.remove(i);
                    continue;
                }
            }
            if (i != expressions.size() - 1) {
                Expression b = expressions.get(i + 1);
                if (joinIncrements) {
                    if (a instanceof Expression.Increment && b instanceof Expression.Increment) {
                        int joinedDelta = ((Expression.Increment) a).getDelta() + ((Expression.Increment) b).getDelta();
                        if (joinedDelta == (short) joinedDelta) {
                            expressions.set(i, new Expression.Increment((short) joinedDelta));
                            expressions.remove(i + 1);
                            if (i > 0) { i--; }
                            continue;
                        }
                    }
                }
                if (joinMoves) {
                    if (a instanceof Expression.Move && b instanceof Expression.Move) {
                        int joinedDelta = ((Expression.Move) a).getDelta() + ((Expression.Move) b).getDelta();
                        if (joinedDelta == (short) joinedDelta) {
                            expressions.set(i, new Expression.Move((short) joinedDelta));
                            expressions.remove(i + 1);
                            if (i > 0) { i--; }
                            continue;
                        }
                    }
                }
            }
        }
    }

    private Expression collapseControlFlow(Expression expression) {
        FlowOptimizer flowOptimizer = new FlowOptimizer();
        List<FlowOptimizer.Tape> tapes = new ArrayList<>();
        tapes.add(flowOptimizer.new Tape());
        return flowOptimizer.walkFlow(expression, tapes);
    }

    private final class FlowOptimizer {
        private boolean hitLimit = false;

        private Expression walkFlow(Expression expression, List<Tape> tapes) {
            if (tapes.isEmpty()) {
                return null;
            }
            if (tapes.size() >= controlFlowTapeLimit) {
                hitLimit = true;
                return expression;
            }

            if (expression instanceof Expression.Increment) {
                for (Tape tape : tapes) {
                    tape.increment(((Expression.Increment) expression).getDelta());
                }
            } else if (expression instanceof Expression.Move) {
                for (Tape tape : tapes) {
                    tape.move(((Expression.Move) expression).getDelta());
                }
            } else if (expression instanceof Expression.Loop) {
                List<Tape> downTapes = new ArrayList<>();
                for (int i = 0; i < tapes.size(); i++) {
                    Tape tape = tapes.get(i);
                    if (tape.hasOther(0)) {
                        downTapes.add(tape);
                        if (tape.has(0)) {
                            Tape clone = new Tape();
                            clone.set(0, true);
                            tapes.set(i, clone);
                        } else {
                            tapes.remove(i--);
                        }
                    }
                }
                List<Expression> newLoop = new ArrayList<>();
                for (Expression child : ((Expression.Root) expression).getExpressions()) {
                    child = walkFlow(child, downTapes);
                    if (hitLimit) { return expression; }
                    if (child != null) {
                        newLoop.add(child);
                    }
                }
                tapes.addAll(
                        downTapes.stream()
                                .filter(downTape -> !tapes.contains(downTape))
                                .collect(Collectors.toList())
                );
                if (newLoop.isEmpty()) {
                    return null;
                }
                return new Expression.Loop(newLoop);
            } else if (expression instanceof Expression.Root) {
                List<Expression> newLoop = new ArrayList<>();
                for (Expression child : ((Expression.Root) expression).getExpressions()) {
                    child = walkFlow(child, tapes);
                    if (hitLimit) { return expression; }
                    if (child != null) {
                        newLoop.add(child);
                    }
                }
                return new Expression.Root(newLoop);
            } else if (expression instanceof Expression.In) {
                tapes.forEach(FlowOptimizer.Tape::flipOn);
            }
            return expression;
        }

        private final class Tape {
            private final StateSet[] possibleValues;
            private int pos;

            private int hashCode = -1;

            public Tape() {
                this.possibleValues =
                        Stream.generate(StateSet::new)
                                .limit(controlFlowTapeSize)
                                .toArray(StateSet[]::new);
                this.pos = controlFlowTapeSize / 2;
            }

            private Tape(Tape prev) {
                this.possibleValues = prev.possibleValues.clone();
                this.pos = prev.pos;
            }

            public void move(int delta) {
                pos += delta;
                hashCode = -1;
                hitLimit |= pos < 0 || pos >= possibleValues.length;
            }

            public void increment(int delta) {
                possibleValues[pos].rotate(-delta);
            }

            public void flipOn() {
                possibleValues[pos].flipOn();
            }

            public boolean has(int val) {
                return possibleValues[pos].get(val);
            }

            public boolean hasOther(int val) {
                return possibleValues[pos].getOther(val);
            }

            public void set(int i, boolean val) {
                possibleValues[pos].set(i, val);
            }

            // todo: better equals & hashcode when tapes are just offset of each other?

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Tape)) { return false; }
                Tape other = (Tape) obj;
                if (other.hashCode() != this.hashCode()) { return false; }
                return this.pos == other.pos &&
                       Arrays.equals(this.possibleValues, other.possibleValues);
            }

            @Override
            public int hashCode() {
                if (hashCode == -1) {
                    int hc = pos;
                    for (StateSet pv : possibleValues) {
                        hc *= 3;
                        hc ^= pv.hashCode();
                    }
                    hashCode = hc;
                }
                return hashCode;
            }
        }

    }

    private static final class StateSet {
        private final boolean[] values = new boolean[256];
        private int offset = 0;

        private int hashCode = -1;

        { values[0] = true; }

        public void rotate(int off) {
            int newOffset = (offset + off) % 256;
            if (newOffset < 0) {
                newOffset += 256;
            }
            offset = newOffset;
            hashCode = -1;
        }

        public void flipOn() {
            Arrays.fill(values, true);
            hashCode = -1;
        }

        public boolean get(int i) {
            return values[(offset + i) % 256];
        }

        public boolean getOther(int i) {
            i = (offset + i) % 256;
            for (int j = 0; j < 256; j++) {
                if (values[j] && j != i) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEmpty() {
            for (int i = 0; i < values.length; i++) {
                boolean value = values[i];
                if (value && i != offset) { return false; }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StateSet)) { return false; }
            StateSet other = (StateSet) obj;
            for (int i = 0; i < 256; i++) {
                if (values[(offset + i) % 256] != other.values[(other.offset + i) % 256]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            if (hashCode == -1) {
                int hc = offset;
                hc *= 11;
                for (int i = 0; i < values.length; i++) {
                    boolean value = values[(offset + i) % 256];
                    hc ^= (value ? 1 : 0) << (i % 31);
                }
                hashCode = hc;
            }
            return hashCode;
        }

        public void set(int i, boolean val) {
            values[(offset + i) % 256] = val;
        }
    }
}
