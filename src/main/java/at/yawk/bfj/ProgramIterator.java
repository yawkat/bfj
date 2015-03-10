/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj;

import javax.annotation.Nullable;

/**
 * @author yawkat
 */
public interface ProgramIterator {
    /**
     * Get the instruction at the current position and, after that, move forward the position by one place.
     */
    @Nullable
    Instruction next();

    /**
     * Get the current position of this iterator as accepted by seek(int).
     *
     * @throws ParserException if mark() has not been called yet. Optional exception.
     */
    int position() throws ParserException;

    /**
     * Seek to a position returned by position().
     *
     * @throws ParserException if the given position precedes the first mark() call. Optional exception.
     */
    void seek(int pos) throws ParserException;

    /**
     * Mark the instruction that would be returned by the next <code>next()</code> call to be seekable, i.e.:
     *
     * <pre>
     * itr.mark();
     * int pos = itr.position();
     * Instruction instructionHere = itr.next();
     * ...
     * itr.seek(pos);
     * itr.next() == instructionHere
     * </pre>
     */
    void mark();
}
