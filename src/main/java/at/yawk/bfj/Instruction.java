package at.yawk.bfj;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public enum Instruction {
    INCREMENT('+'),
    DECREMENT('-'),

    MOVE_LEFT('<'),
    MOVE_RIGHT('>'),

    LOOP_START('['),
    LOOP_END(']'),

    OUTPUT('.'),
    INPUT(',');

    private static final Instruction[] INSTRUCTIONS = new Instruction[0xff];

    static {
        for (Instruction instruction : values()) {
            INSTRUCTIONS[instruction.operator] = instruction;
        }
    }

    @Getter
    private final char operator;

    public static Instruction forOperator(char operator) {
        if (operator > 0xff) {
            return null;
        } else {
            return INSTRUCTIONS[operator];
        }
    }
}
