# PRAVDA

Pravda is a blockchain platform for game developers. It's purpose is to
provide a comprehensive platform and tooling to build blockchain based economy
for games.

Pravda is written in Scala. It is JVM based. We use SBT as a build tool.

It consists of the following modules, each formed as a subproject:

## Modules

[Scala ABCI Server](https://github.com/mytimecoin/scala-abci-server) is a
generic gateway (or proxy) for Tendermint written in Scala that accepts
blockchain messages and passes them through to a business layer.

[pravda-node](doc/pravda-node.md) is a consensus engine based on Tendermint.
It handles all the low level stuff, like p2p networking, consensus protocol,
accepting binary transactions with delivering them to business logic handler and
persistent storage. It also glues together all the other modules, acting
similar to a service bus.

[vm](doc/spec.tex) (Pravda Virtual Machine) is a heart of the Pravda platform.
PVM interprets unified Pravda bytecode and handles security issues. Each unit
to be executed is called Program. In opposite to Smart Contracts, which are more
business centric, Programs are more developer friendly entities.

[asm](doc/PASM.md) (Pravda Assembler) is the assembly language for Pravda
bytecode.  It is also a tool to compile a Program written in the assembly
language to bytecode and vice versa, i.e. disassembler.

[forth](doc/PForth.md) is a Forth-like stack based language and a tool to
compile Programs written in the language to Pravda bytecode.

[vmcli](doc/PCLI.md) is a command line tool to work with Pravda VM.
Currently it can be used to run and debug Pravda Programs.

[pravda](doc/pravda.md) is a unified command line tool to work with Pravda
platform.

## Building and Installation

We use SBT native packager to produce runnable distros for each tool packed in
compressed archives. To build archives just run `sbt universal:packageBin` in
the root. This should create necessary artifacts in the `target` directory
of each subproject.

For those who are not familiar with SBT and native packager plugin, just run the
specified command. Than find <module>.zip archive in the `target` directory
of the subproject you are installing and unpack it to your programs folder. Than
just add it's `bin` directory to your `$PATH`.

We suggest to add all `bin` directories of Pravda tools to $PATH for easy usage
in scripts and pipes.

We hope to start publishing binary releases soon.

## How to

* Work with Forth: [forth how to](doc/how-to-forth.md)
* Run single node net: [test net: single node](doc/how-to-single-node.md)
* Run two nodes net locally: [test net: two nodes](doc/how-to-two-nodes.md)

## Further information

Tendermint: https://tendermint.readthedocs.io
