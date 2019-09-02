<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda compile asm --input <sequence> --output <file> --meta-from-ipfs --ipfs-node <string>```

## Description
Assemble Pravda VM bytecode from the text representation. The input file is a Pravda assembly language text file. The output is a binary Pravda program. By default read from stdin and print to stdout.
## Options

|Option|Description|
|----|----|
|`-i`, `--input`|An input file
|`-o`, `--output`|An output file
|`--meta-from-ipfs`|Load metadata from IPFS if necessary. To configure the IPFS node address use "--ipfs-node" parameter.
|`--ipfs-node`|Ipfs node (/ip4/127.0.0.1/tcp/5001 by default).
