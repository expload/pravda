# Translator Structure

This document describes the basic structure of C# translator: how it parses .exe and .pdb files, how it translates them into Pravda opcodes, important classes and concepts.

### Parsing .exe and .pdb

`pravda.dotnet.parser.FilesParser` is an entry point for all parser requests. Its methods take raw bytes, parse them according to http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf
and produce `ParsedPe` or `ParsedPdb` case classes.

`pravda.dotnet.parser` contains the initial parsers that are necessary to parse .exe and .pdb files.

`pravda.dotnet.parser.PE` includes parsers for the basic structures of PE file.

`pravda.dotnet.parser.CIL` includes parsers for CIL opcodes and their data classes.

`pravda.dotnet.parser.Signatures` includes parsers for the so-called PE Signatures.

`pravda.dotnet.parser.TablesInfo` includes parsers of PE tables and produces intermediate representations that are used in `pravda.dotnet.data.TablesData` to construct easier-to-use structures of these tables.

`pravda.dotnet.data` contains parsers that take results from the `pravda.dotnet.parser` and form more complex data.

As previously stated, `pravda.dotnet.data.TablesData` forms complete PE tables from `pravda.dotnet.parser.TablesInfo`

`pravda.dotnet.data.Method` forms method descriptions from the parsed files.

`pravda.dotnet.data.Heaps` includes the auxiliary methods to access data from PE heaps.

`pravda.dotnet.data.CodedIndexes` includes the auxiliary methods to compute real indexes from the PE coded indexes.

For more details, please refer to http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf
and page links from the head comments in each mentioned class.

### Translation

The entry point of translation is `pravda.dotnet.translation.Translator.translateAsm`. It takes the parsed `ParsedPe` and `ParsedPdb` classes from the previous section.

The translation process starts in `translateVerbose`, followed by the calling of `translateAllMethods` for each parsed file, and followed by the calling of `translateMethod` to translate each method. This done, all the translations are merged and `translationToAsm` is called in order to create a wrapping code and produce actual Pravda opcodes.

For more details, please refer to the code comments in the `pravda.dotnet.translation.Translator` object.

#### Translating CIL Opcodes

`pravda.dotnet.translation.opcode` contains descriptions of the so-called
`OpcodeTranslators`of just _Translators_. Each Translator translates some CIL opcodes to Pravda opcodes. You can find definitions of the `OpcodeTranslator` trait and its other convenient ancestors in `pravda.dotnet.translation.opcode.OpcodeTranslator`, code comments will help you to understand why we need several types of `OpcodeTranslator`.
`pravda.dotnet.translation.opcode. OpcodeTranslator` also has a list of all Translators
that are used to translate CIL opcodes. If you want to add a new one, you should modify this list.

`pravda.dotnet.translation.opcode` also contains definitions of all the Translators. To understand which opcodes and how can be handled by each particular Translator, please take a look at the code comments in the each Translator class and pattern matches in the `asmOps*` methods.

