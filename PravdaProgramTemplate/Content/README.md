# Pravda Program Template

## Installing the template

```bash
dotnet new -i Expload.PravdaProgramTemplate
dotnet new pravdaprogram
```

## Building and running

First, you have to generate a wallet and put it into wallet.json (or move it to the folder if you have one).

```bash
pravda gen address -o wallet.json
```

Then use [Faucet](http://faucet.dev.expload.com/ui) to get some XCoin. After you have funds on your wallet, deploy the program.

```bash
dotnet publish -c Deploy 
```

To update program run

```bash
dotnet publish -c Update
```

## FAQ

> I get `NotEnoughMoney` exception. What do I do?

Make sure you've used faucet to get XCoin for account in `wallet.json`.

> What do I do next?

Now you can edit the program in *.cs and deploy scripts
in .csproj to suit your needs. Make sure to check out [Pravda docs](https://expload.com/developers/documentation/pravda/)!
