<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda [gen|run|compile|broadcast|execute|node]```

## Description
Pravda Command Line Interface

To learn about the options for the particular command you can use the flag --help or -h after the command. For example, to get help regarding the use of the "gen address" command, type "pravda gen address -h"

No options available
## Commands

|Command|Description|
|----|----|
|[`gen address`](pravda-gen-address.md)|Generate the ed25519 key pair. It can be used as a regular wallet or a validator node identifier.|
|[`gen unity`](pravda-gen-unity.md)|Generate the auxiliary code to call the program's methods from Unity|
|[`run`](pravda-run.md)|Run byte-code on Pravda VM|
|[`compile asm`](pravda-compile-asm.md)|Assemble Pravda VM bytecode from the text representation. The input file is a Pravda assembly language text file. The output is a binary Pravda program. By default read from stdin and print to stdout.|
|[`compile disasm`](pravda-compile-disasm.md)|Disassemble Pravda VM bytecode to the text presentation. The input file is a Pravda executable binary. The output is a text file with Pravda assembly code. By default read from stdin and print to stdout.|
|[`compile dotnet`](pravda-compile-dotnet.md)|Compile .exe produced by .NET compiler to Pravda VM bytecode. The input file is a .NET PE (portable executable). The output is a binary Pravda program. By default read from stdin and print to stdout|
|[`compile evm`](pravda-compile-evm.md)|[THIS COMPILATION MODE IS EXPERIMENTAL]Compile .bin produced by the solc compiler to Pravda VM bytecode. The input files are .bin contract and .abi. The output is a binary Pravda program. By default read from stdin and print to stdout|
|[`broadcast run`](pravda-broadcast-run.md)|Send a transaction with Pravda Program address to the blockchain to run it|
|[`broadcast transfer`](pravda-broadcast-transfer.md)|Transfer native coins to a given wallet.|
|[`broadcast deploy`](pravda-broadcast-deploy.md)|Deploy the Pravda program on the blockchain.|
|[`broadcast call`](pravda-broadcast-call.md)|Call the method of the program with arguments|
|[`broadcast seal`](pravda-broadcast-seal.md)|Seal the existing Pravda program in the blockchain.|
|[`broadcast update`](pravda-broadcast-update.md)|Update existing the Pravda program in the blockchain.|
|[`execute`](pravda-execute.md)|Executes a program without side-effects. No watt-limit is required.|
|[`node init`](pravda-node-init.md)|Create a data directory and configuration for the new node.|
|[`node run`](pravda-node-run.md)|Run initialized node.|