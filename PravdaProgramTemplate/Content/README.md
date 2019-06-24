# Pravda Program Template

## Installing the template

```bash
dotnet new -i Expload.PravdaProgramTemplate
dotnet new pravdaprogram
```

## Preparing wallets

### If you have no a wallet.json yet

First, you have to generate a wallet and put it into `wallet.json` file.

```bash
pravda gen address -o wallet.json
```

Then use [Playground-Faucet](https://faucet.playground.expload.com/ui) to accrue some XPlatinum on your wallet.

### If you already have a wallet which has enough XPlatinum for deployment operation

Just put your wallet into `wallet.json` file in current folder.

*Note. If you don't have enough XPlatinum, just get some funds with using [Playground-Faucet](https://faucet.playground.expload.com/ui).*


### If you have no a program-wallet.json yet

Generate a wallet and put it into `program-wallet.json` file.

```bash
pravda gen address -o program-wallet.json
```

Open `program-wallet.json` file and remember address field - it's your future program's address.

### If you already have a program-wallet.json

Just put your program's wallet into `program-wallet.json` into the current folder.

## Building and running

```bash
dotnet publish -c Deploy 
```

To update program run

```bash
dotnet publish -c Update
```

## FAQ

> I get `NotEnoughMoney` error. What should I do?

Make sure you've used Playground-faucet to get XPlatinum for account in `wallet.json`.

> What should I do next?

Now you can edit the program in *.cs and deploy scripts in .csproj to suit your needs. Make sure to check out [Pravda docs](https://expload.com/developers/documentation/pravda/)!
