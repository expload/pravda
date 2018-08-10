<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda compile dotnet --input <file> --output <file> --visualize --pdb <file>```

## Description
Compile .exe produced by .NET compiler to Pravda VM bytecode. Input file is a .NET PE (portable executable). Output is binary Pravdaprogram. By default read from stdin and print to stdout
## Options

|Option|Description|
|----|----|
|`-i`, `--input`|Input file
|`-o`, `--output`|Output file
|`--visualize`|Visualize translation. Prints asm commands along with source CIL opcodes.
|`--pdb`|Pdb file with debug information obtained from .NET compilation.
