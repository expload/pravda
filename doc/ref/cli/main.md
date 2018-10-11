<!--
THIS FILE IS GENERATED. DO NOT EDIT MANUALLY!
-->

```pravda [gen|run|compile|broadcast|node]```

## Description
Pravda 0.7.0 Command Line Interface

To get info about options for particular command you can use flag --help or -h after command. For example, to get help about "gen address" command, type "pravda gen address -h"

No options available
## Commands

|Command|Description|
|----|----|
|[`gen address`](pravda-gen-address.md)|Generate ed25519 key pair. It can be used as regular wallet or validator node identifier.|
|[`gen unity`](pravda-gen-unity.md)|Generate auxiliary code to call program's methods from Unity|
|[`run`](pravda-run.md)|Run byte-code on Pravda VM|
|[`compile asm`](pravda-compile-asm.md)|Assemble Pravda VM bytecode from text representation. Input file is a Pravda assembly language text file. Output is binary Pravda program. By default read from stdin and print to stdout.|
|[`compile disasm`](pravda-compile-disasm.md)|Disassemble Pravda VM bytecode to text presentation. Input file is a Pravda executable binary. Output is a text file with Pravda assembly code. By default read from stdin and print to stdout.|
|[`compile dotnet`](pravda-compile-dotnet.md)|Compile .exe produced by .NET compiler to Pravda VM bytecode. Input file is a .NET PE (portable executable). Output is binary Pravdaprogram. By default read from stdin and print to stdout|
|[`broadcast run`](pravda-broadcast-run.md)|Send a transaction with Pravda Program address to the blockchain to run it|
|[`broadcast transfer`](pravda-broadcast-transfer.md)|Transfer native coins to a given wallet.|
|[`broadcast deploy`](pravda-broadcast-deploy.md)|Deploy Pravda program to the blockchain.|
|[`broadcast seal`](pravda-broadcast-seal.md)|Seal existing Pravda program in the blockchain.|
|[`broadcast update`](pravda-broadcast-update.md)|Update existing Pravda program in the blockchain.|
|[`node init`](pravda-node-init.md)|Create data directory and configuration for a new node.|
|[`node run`](pravda-node-run.md)|Run initialized node.|