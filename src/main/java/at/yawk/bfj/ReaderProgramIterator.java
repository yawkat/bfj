/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import javax.annotation.Nullable;

/**
 * @author yawkat
 */
public class ReaderProgramIterator extends BufferedProgramIterator implements Closeable {
    private final Reader reader;

    public ReaderProgramIterator(Reader reader) {
        this.reader = reader;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Nullable
    @Override
    protected Instruction retrieveNext() {
        try {
            while (true) {
                int i = reader.read();
                if (i == -1) {
                    close();
                    return null;
                }
                Instruction instruction = Instruction.forOperator((char) i);
                if (instruction != null) {
                    return instruction;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
