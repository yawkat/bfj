/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj.compile;

import at.yawk.bfj.Instruction;
import at.yawk.bfj.ProgramIterator;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
class ExpressionCompiler {
    private final ProgramIterator iterator;
    private final Optimizer optimizer;

    public Expression compile() {
        List<Expression> expressions = build();
        Expression expression = expressions.size() == 1 ? expressions.get(0) : new Expression.Root(expressions);
        return optimizer.optimize(expression);
    }

    private List<Expression> build() {
        List<Expression> expressions = new ArrayList<>();
        while (true) {
            Instruction instruction = iterator.next();
            if (instruction == null || instruction == Instruction.LOOP_END) { break; }
            switch (instruction) {
            case INCREMENT:
                expressions.add(new Expression.Increment((short) 1));
                break;
            case DECREMENT:
                expressions.add(new Expression.Increment((short) -1));
                break;
            case MOVE_LEFT:
                expressions.add(new Expression.Move((short) -1));
                break;
            case MOVE_RIGHT:
                expressions.add(new Expression.Move((short) 1));
                break;
            case LOOP_START:
                expressions.add(new Expression.Loop(build()));
                break;
            case OUTPUT:
                expressions.add(new Expression.Out());
                break;
            case INPUT:
                expressions.add(new Expression.In());
                break;
            }
        }
        return expressions;
    }
}
