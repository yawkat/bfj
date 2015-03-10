/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj.interpret;

/**
 * @author yawkat
 */
final class Tape {
    byte[] buf = new byte[1024];
    int pos = 512;

    byte get() {
        return isOutOfBounds() ? 0 : buf[pos];
    }

    void set(byte b) {
        growIfNecessary();
        buf[pos] = b;
    }

    private int growIfNecessary() {
        while (isOutOfBounds()) {
            byte[] newBuf = new byte[buf.length << 1];
            if (pos < 0) {
                // grow to start
                System.arraycopy(buf, 0, newBuf, buf.length, buf.length);
                pos += buf.length;
            } else {
                // grow to end
                System.arraycopy(buf, 0, newBuf, 0, buf.length);
            }
            buf = newBuf;
        }
        return pos;
    }

    private boolean isOutOfBounds() {
        return pos < 0 || pos >= buf.length;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        int first = -1;
        int last = -1;
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != 0) {
                if (first == -1) { first = i; }
                last = i;
            }
        }
        if (first > pos || first == -1) { first = pos; }
        if (last < pos || last == -1) { last = pos; }
        for (int i = first; i <= last; i++) {
            if (i != first) {
                builder.append(' ');
            }
            if (i == pos) {
                builder.append('(');
            }
            if (i > buf.length || i < 0) {
                builder.append(0);
            } else {
                builder.append(buf[i] & 0xff);
            }
            if (i == pos) {
                builder.append(')');
            }
        }
        return builder.append(']').toString();
    }
}
