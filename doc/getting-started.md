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

## Run node
  
Then we need to run local node. First of all lets build initial coin distribution config. 

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

```bash
pravda node init --local --coin-distribution my-coin-distribution.json
```

Congratulations! The configuration is written to `pravda-data/node.conf` directory (also you can chose data directory using `--data-dir` key). Now let's run the node.

```bash
pravda node run
```

Now you have our own Pravda network with one validator, and one billion coins on your account. Check out `http://localhost:8080/ui`. 

## Transfer coins

You are very rich! Now you want to donate a part of your wealth to some poor guy. Let's generate wallet for him.

```bash
pravda gen address -o another-wallet.json
```
  
Wallet for poor guy is created. Now let's copy his address and transfer some coins.

```bash
pravda broadcast transfer \
  --wallet my-wallet.json \
  --to <address-of-poor-guy> \
  --amount 1000000
```

Now the poor guy is not so poor. Great job!
