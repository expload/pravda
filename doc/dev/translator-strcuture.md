# Translator structure

This document describes basic structure of C# translator: 
how it parses .exe and .pdb files, how it translates them into Pravda opcodes, 
important classes and concepts. 

### Parsing .exe and .pdb

`pravda.dotnet.parser.FilesParser` is an entry point for all parser requests. 
Its methods takes raw bytes, 
parses them according to http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf 
and produces `ParsedPe` or `ParsedPdb` case classes. 

`pravda.dotnet.parser` contains initial parsers needed to parse .exe and .pdb files. 

`pravda.dotnet.parser.PE` includes parsers for basic structures of PE file.

`pravda.dotnet.parser.CIL` includes parsers for CIL opcodes and data classes for them.

`pravda.dotnet.parser.Signatures` includes parsers for so called PE Signatures. 

`pravda.dotnet.parser.TablesInfo` includes parsers of PE tables and 
produces intermediate representations that used in `pravda.dotnet.data.TablesData` 
to construct easier to use structures of these tables. 

`pravda.dotnet.data` contains parsers that takes results from `pravda.dotnet.parser`
and forms more complex data.

As previously said, `pravda.dotnet.data.TablesData` forms complete PE tables 
from `pravda.dotnet.parser.TablesInfo`

`pravda.dotnet.data.Method` forms method descriptions from parsed files.

`pravda.dotnet.data.Heaps` includes auxiliary methods to access data from PE heaps. 

`pravda.dotnet.data.CodedIndexes` includes auxiliary methods 
to compute real indexes from PE coded indexes.

For more details refer to http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf 
and page links from head comments in each mentioned class.

### Translation

Entry point of translation is `pravda.dotnet.translation.Translator.translateAsm`. 
It takes parsed `ParsedPe` and `ParsedPdb` classes from previous section.

Translation process starts in `translateVerbose` 
than calling `translateAllMethods` for each parsed file,
than calling `translateMethod` to translate each method.
After this all translations are merged and `translationToAsm` is called
in order to create wrapping code and produce actual Pravda opcodes.

For more details refer to code comments in the `pravda.dotnet.translation.Translator` object.

#### Translating CIL opcodes

`pravda.dotnet.translation.opcode` contains descriptions of so-called `OpcodeTranslators`
of just _Translators_. Each Translator translates some CIL opcodes to the Pravda opcodes. 
You can find definitions of `OpcodeTranslator` trait and other convenient ancestors of it in 
`pravda.dotnet.translation.opcode.OpcodeTranslator`,
code comments will help you to understand why we need several kinds of `OpcodeTranslator`.
`pravda.dotnet.translation.opcode.OpcodeTranslator` also has list of all Translator 
that are used to translate CIL opcodes, if you want to add a new one you should modify this list. 

`pravda.dotnet.translation.opcode` also contains definitions of all these Translators. 
To understand which opcodes each Translator can handle and how does it handle them
take a look of code comments in the each Translator class 
and pattern matches in the `asmOps*` methods. 