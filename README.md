# Pravda

Pravda is a blockchain platform for game developers. Its purpose is to
provide a comprehensive platform and tooling to build blockchain-based economy
for games.

Pravda is written in Scala. It is JVM-based. We use SBT as a build tool.

## Project structure

* [node](doc/pravda-node.md) is a consensus engine based on Tendermint.
It handles all the low level stuff, like p2p networking, consensus protocol,
accepting binary transactions with delivering them to business logic handler and
persistent storage. It also glues together all the other modules, acting
similar to a service bus.

* [vm](doc/spec.tex) (Pravda Virtual Machine) is a heart of the Pravda platform.
PVM interprets unified Pravda bytecode and handles security issues. Each unit
to be executed is called Program. In opposite to Smart Contracts, which are more
business centric, Programs are more developer friendly entities.

* [asm](doc/ref/cli/pravda-compile-asm.md) Pravda Assembler is the assembly
language for Pravda Virtual Machine.

* [forth](doc/ref/cli/pravda-compile-forth.md) is a Forth-like stack based
language which can be compiled to Pravda bytecode.

* [cli](doc/ref/cli/main.md) is a unified command line tool to work with Pravda
platform. It can be used to compile programs in Forth, .Net PE, Pravda Assembly
languages to Pravda bytecode, to execute programs and to control Pravda nodes.

## Installation

### Windows

Download MSI installer from [releases page](https://github.com/mytimecoin/pravda/releases).
Double click on it. Currently installer is unsigned. It leads to red alert during installation.
Do not afraid: it's OK.

### Linux and Macos

We've prepared universal installation script. Just run this command in your terminal.

```
curl -s https://raw.githubusercontent.com/mytimecoin/pravda/master/i.sh | bash
```

## Building from sources

We use SBT native packager to produce runnable distros for each tool packed in
compressed archives.

```
$ sbt cli/universal:stage
$ ./cli/target/universal/stage/bin/pravda
```

To build archive just run `sbt cli/universal:packageZipTarball` in
the root of project. This will create necessary tgz-archive of
the Pravda CLI in the `cli/target`.

## How to

* [Work with Forth](doc/how-to-forth.md)
* [Run single node net](doc/how-to-single-node.md)
* [Run two nodes net locally](doc/how-to-two-nodes.md)

## Participation

We are glad to see any Pull Requests, especially if they solve issues labeled
`good first issue` or `help wanted`. Also we will accept PRs which fix typos,
mistakes, broken links, etc. Regardless of the nature of your PR, we have to
ask you, to digitally sign the Mytime CLA. Please, send us email with GPG signed
text of CLA to contributing@mytc.io.

If you want to send PR, make sure that this requirements are satisfied:

* You have already sent GPG-signed Mytime CLA to contributing@mytc.io
* Commits are signed with same GPG-key as CLA
* Content of Pull Request satisfy Code Of Conduct
* Any PR should resolve an issue
* PR name matches "Close/Fix #issue: Summary"
* PR doesn't contain merge commits
* Commit message matches "verb in present simple subject (#issue)"

## Further information

Tendermint: https://tendermint.readthedocs.io
