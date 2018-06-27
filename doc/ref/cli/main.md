```pravda [gen|run|compile|broadcast|node]```

## Description
Pravda Command Line Interface to Pravda SDK

No options available
## Commands

|Command|Docs|Description|
|----|----|----|
|`pravda-gen-address`|[docs](pravda-gen-address.md)|Generate ed25519 key pair. It can be used as regular wallet or validator node identifier.|
|`pravda-run`|[docs](pravda-run.md)|Run byte-code on Pravda VM|
|`pravda-compile-asm`|[docs](pravda-compile-asm.md)|Assemble Pravda VM bytecode from text representation. Input file is a Pravda assembly language text file. Output is binary Pravda program. By default read from stdin and print to stdout|
|`pravda-compile-disasm`|[docs](pravda-compile-disasm.md)|Disassemble Pravda VM bytecode to text presentation. Input file is a Pravda executable binary. Output is a text file with Pravda assembly code. By default read from stdin and print to stdout|
|`pravda-compile-forth`|[docs](pravda-compile-forth.md)|Compile Pravda pseudo-forth to Pravda VM bytecode. Input file is a Pravda executable binary. Output is a text file with Pravda assembly code. By default read from stdin and print to stdout.|
|`pravda-compile-dotnet`|[docs](pravda-compile-dotnet.md)|Compile .exe produced by .NET compiler to Pravda VM bytecode. Input file is a .Net PE (portable executable). Output is binary Pravdaprogram. By default read from stdin and print to stdout.|
|`pravda-broadcast-run`|[docs](pravda-broadcast-run.md)|Send a transaction with Pravda Program address to the blockchain to run it|
|`pravda-broadcast-transfer`|[docs](pravda-broadcast-transfer.md)|Transfer native coins to a given wallet.|
|`pravda-broadcast-deploy`|[docs](pravda-broadcast-deploy.md)|Deploy Pravda program to the blockchain.|
|`pravda-broadcast-update`|[docs](pravda-broadcast-update.md)|Update existing Pravda program in the blockchain.|
|`pravda-node-init`|[docs](pravda-node-init.md)|Create data directory and configuration for a new node.|
|`pravda-node-run`|[docs](pravda-node-run.md)|Run initialized node.|