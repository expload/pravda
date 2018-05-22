`asm` is a command line tool to compile Pravda programs written in Pravda
assembly language to Pravda bytecode.
```
Usage:

asm <filename>

    Compile program and write bytecode to file (a.pravda by default).

    -d, --disasm Disassemble bytecode given in file <filename>.
                 Print assembly representation to standard out.

    -x, --hex    Print hex encoded bytecode to standard out.

    -b, --base64 Print base64 encoded bytecode to standard out.

    -o, --output <filename>
                 Write bytecode to <filename>.
```
