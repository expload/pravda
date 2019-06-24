# Getting started

## Installation

Ensure that JRE 1.8 or higher is installed in your system. Also ensure that 8080 TCP port is free.

### Windows

Download MSI installer from [releases page](https://github.com/expload/pravda/releases). Double click on it. Currently installer is unsigned. It leads to red alert during installation. Do not afraid: it's OK.

### Linux and macOS

We've prepared universal installation script. Just run this command in your terminal.

```
curl -sL https://git.io/pravda-for-unix | bash
```

## Write your first Pravda Program

### Importing Project Template

To use project template you will need [.NET Core SDK 2.1](https://www.microsoft.com/net/download/dotnet-core/2.1).  
Create a directory for your project and run inside it:  

```bash
dotnet new -i Expload.PravdaProgramTemplate
dotnet new pravdaprogram --namespace MyTeam --version 1.0.0 --name HelloProgram 
```
Now your directory contains:
 - `HelloProgram.cs` - main program file. By default it contains simple program with `HelloWorld` method
 - `HelloProgram.csproj` - project configuration. It includes scripts for compiling and deployment, as well as .NET dependencies & metadata
 - `README.md` - feel free to consult it whenever you feel confused

You can also check the section [Hello, World](https://developers.expload.com/documentation/pravda/hello-world/) that contains a more complex sample of a Pravda program.

### Compiling the Program

To compile C# code into pravda binary run:

```
dotnet publish -c Debug
```

Now you have `MyProject.pravda` in your project directory.  
You can use [local node](#Getting-started-with-CLI) and [Pravda CLI commands](https://developers.expload.com/documentation/pravda/CLI/pravda-broadcast-deploy/) to deploy to a local node or proceed further to work with Expload Playground.

### Deploying to Expload Playground

#### Preparing wallets

##### If you have no a wallet.json yet

First, you have to generate a wallet and put it into `wallet.json` file.

```bash
pravda gen address -o wallet.json
```

Then use [Playground-Faucet](https://faucet.playground.expload.com/ui) to accrue some XPlatinum on your wallet.

##### If you already have a wallet which has enough XPlatinum for deployment operation

Just put your wallet into `wallet.json` file in current folder.

*Note. If you don't have enough XPlatinum, just get some funds with using [Playground-Faucet](https://faucet.playground.expload.com/ui).*


##### If you have no a program-wallet.json yet

Generate a wallet and put it into `program-wallet.json` file.

```bash
pravda gen address -o program-wallet.json
```

Open `program-wallet.json` file and remember address field - it's your future program's address.

##### If you already have a program-wallet.json

Just put your program's wallet into `program-wallet.json` into the current folder.

#### Deploying 

Finally, run:

```
dotnet publish -c Deploy
```

Now your program is on the Expload Playground net! 

If you want to update program run

```bash
dotnet publish -c Update
```
  
For more information on working with the template, 
see `README.md` in your project directory.

## Setup your own Pravda testnet

### Configure initial coin distribution

Before we can to run a local node, we should build the initial coin distribution config. 

```bash
pravda gen address -o my-wallet.json
```

This command will generate ED25519 key pair. It's a valid Pravda wallet. Now you can add an address to coin distribution config.

```json
[
  {
    "address": "address from my-wallet.json",
    "amount": 1000000000
  }
]
```

Save this to `my-coin-distribution.json`. Now lets initialize node configuration.

### Initialize Pravda node

```bash
pravda node init --local --coin-distribution my-coin-distribution.json
```

Congratulations! The configuration is written to `pravda-data/node.conf` directory (also you can chose data directory using `--data-dir` key). Now let's run the node.

### Run Pravda node

```bash
pravda node run
```

Now you have our own Pravda network with one validator, and one billion coins on your account. Check out `http://localhost:8080/ui`. 

### Transfer coins

You are very rich! Now you want to donate a part of your wealth to some poor guy. Let's generate wallet for him.

```bash
pravda gen address -o another-wallet.json
```
  
Wallet for poor guy has created. Now let's copy that address and transfer some coins.

```bash
pravda broadcast transfer \
  --wallet my-wallet.json \
  --to <address-of-poor-guy> \
  --amount 1000000
```

Now the poor guy is not so poor. Great job!
