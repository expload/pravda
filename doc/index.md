# Getting Started

## Installation

Ensure that JRE 1.8 or higher is installed in your system. Also, ensure that 8080 TCP port is free.

### Windows

Download the MSI installer from [releases page](https://github.com/expload/pravda/releases) and double click on it. As the installer is currently unsigned, this causes red alert during the installation. Do not worry: it's OK.

### Linux and macOS

We've prepared a universal installation script. Just run this command in your terminal.

```
curl -sL https://git.io/pravda-for-unix | bash
```

## Write Your First Pravda Program

### Importing the Project Template

To use the project template, you will need [.NET Core SDK 2.1](https://www.microsoft.com/net/download/dotnet-core/2.1).
Create a directory for your project and run inside it:

```bash
dotnet new -i Expload.PravdaProgramTemplate
dotnet new pravdaprogram --namespace MyTeam --version 1.0.0 --name HelloProgram
```
Now your directory contains:
 - `HelloProgram.cs` - main program file. By default it contains simple program with `HelloWorld` method
 - `HelloProgram.csproj` - project configuration. It includes scripts for compiling and deployment, as well as .NET dependencies & metadata
 - `README.md` - feel free to consult it whenever you feel confused

You can also check out the section [Hello World](https://developers.expload.com/documentation/pravda/hello-world/) that contains a more complex sample of a Pravda program.

### Compiling the Program

To compile C# code into pravda binary run:

```
dotnet publish -c Debug
```

Now you have `MyProject.pravda` in your project directory.
You can use [local node](#Getting-started-with-CLI) and [Pravda CLI commands](https://developers.expload.com/documentation/pravda/CLI/pravda-broadcast-deploy/) to deploy to a local node or proceed further to work with Expload Playground.

### Deploying to Expload Playground

#### Preparing Wallets

##### If you have no wallet.json yet

First, you have to generate a wallet and put it into the `wallet.json` file.

```bash
pravda gen address -o wallet.json
```

Then use [Playground-Faucet](https://faucet.playground.expload.com/ui) to credit a certain amount of XPlatinum to your wallet.

##### If you already have a wallet with the sufficient amount of XPlatinum for the deployment operation

Just put your wallet into the `wallet.json` file in the current folder.

*Note. If you don't have the sufficient amount of XPlatinum, just get some funds using [Playground-Faucet](https://faucet.playground.expload.com/ui).*


##### If you have no program-wallet.json yet

Generate a wallet and put it into the `program-wallet.json` file.

```bash
pravda gen address -o program-wallet.json
```

Open the `program-wallet.json` file and remember the address field — this is your future program's address.

##### If you already have a program-wallet.json

Just put your file `program-wallet.json` into the current folder.

#### Deploying

Finally, run:

```
dotnet publish -c Deploy
```

Now your program is on the Expload Playground net!

If you want to update the program, run

```bash
dotnet publish -c Update
```

For more information on working with the template,
see `README.md` in your project directory.

## Set Up Your Own Pravda Testnet

### Configure Initial Coin Distribution

Before you can run a local node, you should build the initial coin distribution config.

```bash
pravda gen address -o my-wallet.json
```

This command will generate a ED25519 key pair. This is a valid Pravda wallet. Now you can add an address to the coin distribution config.

```json
[
  {
    "address": "address from my-wallet.json",
    "amount": 1000000000
  }
]
```

Save this to `my-coin-distribution.json`. Now let’s initialize the node configuration.

### Initialize the Pravda Node

```bash
pravda node init --local --coin-distribution my-coin-distribution.json
```

Congrats! The configuration is written to the `pravda-data/node.conf` directory (also, you can choose the data directory using `--data-dir` key). Now let's run the node.

### Run the Pravda Node

```bash
pravda node run
```

Now you have your own Pravda network with one validator and funds on your account. Check out `http://localhost:8080/ui`.

### Transfer Coins

You may now want to donate a part of your wealth to another user. Let's generate a wallet for them.

```bash
pravda gen address -o another-wallet.json
```

A wallet for the user has been created. Now let's copy this address and transfer some coins.

```bash
pravda broadcast transfer \
  --wallet my-wallet.json \
  --to <address-of-poor-guy> \
  --amount 1000000
```

The funds have been credited. Great job!
