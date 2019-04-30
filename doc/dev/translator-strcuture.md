# Translator structure

This document describes basic structure of C# translator: 
how it parses .exe and .pdb files, how it translates them into Pravda opcodes, 
important classes and concepts. 

### Parsing .exe and .pdb

`pravda.dotnet.parser.FilesParser` is an entry point for all parser requests. 
Its methods takes raw bytes, 
parses them according to http://www.ecma-international.org/publications/files/ECMA-ST/ECMA-335.pdf 
and produces `ParsedPe` or `ParsedPdb` case classes. 

`pravda.dotnet.parser` contains intial parsers needed to parse .exe and .pdb files. 

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
and page links from comments head comments in each mentioned class.

### Translation

