<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda compile disasm --input <sequence> --output <file> --meta-from-ipfs --ipfs-node <string>```

## Description
Disassemble Pravda VM bytecode to text presentation. Input file is a Pravda executable binary. Output is a text file with Pravda assembly code. By default read from stdin and print to stdout.
## Options

|Option|Description|
|----|----|
|`-i`, `--input`|Input file
|`-o`, `--output`|Output file
|`--meta-from-ipfs`|Load metadata from IPFS if necessary. To configure the IPFS node address use "--ipfs-node" parameter.
|`--ipfs-node`|Ipfs node (/ip4/127.0.0.1/tcp/5001 by default).
