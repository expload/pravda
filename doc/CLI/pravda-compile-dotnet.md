<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda compile dotnet --input <sequence> --output <file> --meta-from-ipfs --ipfs-node <string> --main-class <string>```

## Description
Compile .exe produced by .NET compiler to Pravda VM bytecode. The input file is a .NET PE (portable executable). The output is a binary Pravda program. By default read from stdin and print to stdout
## Options

|Option|Description|
|----|----|
|`-i`, `--input`|An input file
|`-o`, `--output`|An output file
|`--meta-from-ipfs`|Load metadata from IPFS if necessary. To configure the IPFS node address use "--ipfs-node" parameter.
|`--ipfs-node`|Ipfs node (/ip4/127.0.0.1/tcp/5001 by default).
|`--main-class`|Full name of the class that should be compile to Pravda program
