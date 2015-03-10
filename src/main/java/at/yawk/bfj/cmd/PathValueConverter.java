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
