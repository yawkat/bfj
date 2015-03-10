bfj
===

Fast brainfuck parser, interpreter and compiler written in java.

Command Line
------------

bfj includes a simple command-line interface for running and optimizing brainfuck programs.

`java -jar bfj.jar program.bf`

Options:

- `--charset <charset>` defines the charset of the input file(s). Defaults to `UTF-8`.
- `--engine <engine>` selects the engine to use for running the input. One of `interpreted` and `compiled`, defaults to interpreted mode. 
- `--disable-auto-flush` disables automatic flushing for program output.
- `--detect-infinite-loops` enables infinite loop detection for interpreted mode. Note that this is a very memory- and runtime-expensive option.
- `-v` tells the interpreter to give verbose output. Before each instruction, the current tape state and next instruction will be printed.
- `--optimize` outputs the optimized code instead of running it. Only supported on the `compiled` engine currently.
- `-i <n>` how many each program should be run, defaults to 1

If multiple program files are given, they will be executed in order.

Library
-------

The data model of bfj consists of four basic interfaces:

- `ProgramIterator` describes an iterator over a program, i.e. a stream of brainfuck instructions.
- `IO` is the input and output interface that brainfuck programs will call.
- `Automaton` is the class a compiled/interpreted brainfuck program will implement. It contains a single method, `.execute(IO)`
- `Engine` is the base interface for a system that builds an `Automaton` from a `ProgramIterator`, for example the interpreter or the compiler.

### Parser
Parsing is performed by the `ReaderProgramIterator`. It can be passed a `Reader` and it will parse the instructions in that reader to brainfuck instructions.

### Interpreter
The interpreter is a basic `Engine` implementation that simply walks through the instructions of the given program and interprets them on-the-fly. It also supports infinite loop detection and a progress logger. The implementation class is `InterpretedEngine`.

### Compiler
The compiler is an experimental `Engine` implementation that compiles the given program to java bytecode (using javassist) for extra speed over the interpreter. It also features more advanced optimization mechanisms.

### Examples

Interpreter:
```java
Reader reader = ...;
ProgramIterator programIterator = new ReaderProgramIterator(reader);
InterpretedEngine engine = new InterpretedEngine();
engine.setDetectInfiniteLoops(true);
Automaton automaton = engine.produce(programIterator);
automaton.execute(new SystemIO());
```

Compiler:
```java
Reader reader = ...;
ProgramIterator programIterator = new ReaderProgramIterator(reader);
Engine engine = new CompiledEngine();
Automaton automaton = engine.produce(programIterator);
automaton.execute(new SystemIO());
```
