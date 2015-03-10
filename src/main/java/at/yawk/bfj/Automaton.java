/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj;

import javax.annotation.concurrent.ThreadSafe;

/**
 * @author yawkat
 */
@ThreadSafe
public interface Automaton {
    void execute(IO io);
}
