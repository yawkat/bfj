package at.yawk.bfj.cmd;

import at.yawk.bfj.IO;
import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class SystemIO implements IO {
    private final boolean autoFlush;

    @Override
    public byte read() {
        try {
            int i = System.in.read();
            if (i == -1) {
                throw new EOFException();
            }
            return (byte) i;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(byte value) {
        System.out.write(value);
        if (autoFlush) {
            System.out.flush();
        }
    }
}
