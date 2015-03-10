package at.yawk.bfj.cmd;

import java.nio.charset.Charset;
import joptsimple.ValueConverter;

/**
 * @author yawkat
 */
class CharsetValueConverter implements ValueConverter<Charset> {
    @Override
    public Charset convert(String value) {
        return Charset.forName(value);
    }

    @Override
    public Class<? extends Charset> valueType() {
        return Charset.class;
    }

    @Override
    public String valuePattern() {
        return null;
    }
}
