/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj.cmd;

import java.nio.file.Path;
import java.nio.file.Paths;
import joptsimple.ValueConverter;

/**
* @author yawkat
*/
class PathValueConverter implements ValueConverter<Path> {
    @Override
    public Path convert(String value) {
        return value.equals("-") ? null : Paths.get(value);
    }

    @Override
    public Class<? extends Path> valueType() {
        return Path.class;
    }

    @Override
    public String valuePattern() {
        return null;
    }
}
