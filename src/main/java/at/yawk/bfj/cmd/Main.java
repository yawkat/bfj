/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package at.yawk.bfj.cmd;

import at.yawk.bfj.*;
import at.yawk.bfj.compile.CompiledEngine;
import at.yawk.bfj.interpret.InterpretedEngine;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import joptsimple.*;

/**
 * @author yawkat
 */
public class Main {
    private static final Engine DEFAULT_ENGINE = new InterpretedEngine();
    private static final Map<String, Engine> ENGINES = new HashMap<>();

    static {
        ENGINES.put("interpreted", new InterpretedEngine());
        ENGINES.put("compiled", new CompiledEngine());
    }

    private Main() {}

    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();

        OptionSpec<Charset> charsetOption = parser.accepts("charset")
                .withRequiredArg()
                .withValuesConvertedBy(new CharsetValueConverter())
                .defaultsTo(StandardCharsets.UTF_8);
        OptionSpec<Engine> engineOption = parser.accepts("engine")
                .withRequiredArg()
                .withValuesConvertedBy(new ValueConverter<Engine>() {
                    @Override
                    public Engine convert(String value) {
                        return ENGINES.get(value.toLowerCase());
                    }

                    @Override
                    public Class<? extends Engine> valueType() {
                        return Engine.class;
                    }

                    @Override
                    public String valuePattern() {
                        return String.join(",", ENGINES.keySet());
                    }
                })
                .defaultsTo(DEFAULT_ENGINE);
        OptionSpec<Void> disableAutoFlushOption = parser.accepts("disable-auto-flush");
        OptionSpec<Void> verboseOption = parser.accepts("v");
        OptionSpec<Void> detectInfiniteLoopsOption = parser.accepts("detect-infinite-loops");
        OptionSpec<Void> optimizeOption = parser.accepts("optimize");
        OptionSpec<Integer> iterationCountOption = parser.accepts("i")
                .withRequiredArg()
                .ofType(int.class)
                .defaultsTo(1);
        OptionSpec<Path> sourceOption = parser.nonOptions().withValuesConvertedBy(new PathValueConverter());

        OptionSet parsed;
        try {
            parsed = parser.parse(args);
        } catch (OptionException e) {
            e.printStackTrace();
            parser.printHelpOn(System.err);
            return;
        }

        Engine engine = engineOption.value(parsed);
        Charset charset = charsetOption.value(parsed);
        List<Path> sources = sourceOption.values(parsed);
        boolean autoFlush = !parsed.has(disableAutoFlushOption);
        int iterationCount = iterationCountOption.value(parsed);
        boolean optimizeOnly = parsed.has(optimizeOption);

        if (parsed.has(verboseOption)) {
            ((InterpretedEngine) engine).setPrintProgress(true);
        }
        if (parsed.has(detectInfiniteLoopsOption)) {
            ((InterpretedEngine) engine).setDetectInfiniteLoops(true);
        }

        IO io = new SystemIO(autoFlush);

        for (Path path : sources) {
            Supplier<InputStream> in;
            if (path != null) {
                in = () -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                };
            } else {
                in = new Supplier<InputStream>() {
                    boolean consumed = false;

                    @Override
                    public synchronized InputStream get() {
                        if (consumed) {
                            throw new UnsupportedOperationException("Cannot open stdin input source more than once");
                        }
                        consumed = true;
                        return System.in;
                    }
                };
            }
            try (Reader reader = new InputStreamReader(in.get(), charset)) {
                BufferedProgramIterator iterator = new ReaderProgramIterator(reader);

                if (optimizeOnly) {
                    @SuppressWarnings("ConstantConditions")
                    MemoryProgram optimized = ((CompiledEngine) engine).optimize(iterator);
                    for (Instruction instruction : optimized.getInstructions()) {
                        System.out.print(instruction.getOperator());
                    }
                    System.out.println();
                } else {
                    Automaton automaton = engine.produce(iterator);
                    for (int i = 0; i < iterationCount; i++) {
                        automaton.execute(io);
                    }
                }
            }
        }
    }
}
