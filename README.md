# Pravda

Pravda is a general purpose blockchain with PoA consensus (PoW in future). It's a part of [Expload](https://expload.com) - a platform for a games with distributed economy.

Network node is build on top of [Tendermint](http://tendermint.com/), is written in Scala and runs on JVM. We have our own virtual machine for "smart contracts" (not exactly) and translators from [.NET](https://en.wikipedia.org/wiki/Common_Intermediate_Language) (and [EVM](https://ethereum.github.io/yellowpaper/paper.pdf) in future) bytecode.

## Documentation

* [Getting Started](doc/getting-started.md)
* [Command line interface](doc/ref/cli/main.md)
* [Pravda Virtual Machine](doc/ref/vm)
  * [Using assembler](doc/ref/vm/asm.md)
  * [Internal data format](doc/ref/vm/data.md)
  * [Opcodes](doc/ref/vm/opcodes.md)
  * [Standard Library](doc/ref/vm/stdlib.md)
  * [Meta information](doc/ref/vm/meta.md)
* [DApp API specification](doc/dapp-api.md)
* [Node API specification](doc/node-api.md)
* [Dotnet](doc/ref/dotnet)
  * [Dotnet translation](doc/ref/dotnet/translation.md)
    * [Dotnet classes translation](doc/ref/dotnet/classes-translation.md)
* [Code generation](doc/codegen.md)
* [Glossary](doc/glossary.md)
* [FAQ](doc/faq.md)

## Development

  * [Participation](doc/dev/participation.md)
  * [Building from sources](doc/dev/building-from-sources.md)
  * [How to generate documentation](doc/dev/gen-doc.md)   
  * [How to add new functionality to VM](doc/dev/add-new-functionality.md)
