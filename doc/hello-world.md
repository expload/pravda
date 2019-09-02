# Your First Program on Pravda

Let's go throughout the process of developing and deploying a Pravda program using the basic token program as an example. We will create a fungible token, which can be released and transferred to other wallets.

## Pre-requirements

- It is **highly** recommended that you get familiarized with the basic principles of smart contracts
- You need to [install Pravda CLI](https://developers.expload.com/documentation/pravda/#installation)
- You will also need [.NET Core SDK 2.1](https://dotnet.microsoft.com/download/dotnet-core/2.1)

## Writing the Code

Let's start by [importing the Pravda program template](https://developers.expload.com/documentation/pravda/#importing-project-template).
You will get the following code (except that the namespace and program name will probably be different):

```C#
namespace NAMESPACE {

    using Expload.Pravda;
    using System;

    [Program]
    public class MyProgram {

        public string HelloWorld() {
            return "Hello, world!";
        }
    }
}
```

Basically, there is almost no difference between the regular C# code and our Pravda program. The only element that we are adding here is the `Program` attribute. It indicates which class in our code is the actual "smart contract".

From now on we will use only the term `Pravda program`, not `smart contract`.

<details>
<summary>What is the difference between them?</summary>

> A `Pravda program` can be updated (you can change its code without changing
> the program's address) and sealed (you can "seal" the program's code, making it
> immutable), which contradicts the widely accepted definition of
> `smart contract`. It is therefore technically inaccurate to refer to
> Pravda programs as smart contracts, since they have a different functionality.

</details>

Let's remove the `HelloWorld` function and the [mapping](https://en.wikipedia.org/wiki/Associative_array) storing the users'
balances:

```C#
namespace NAMESPACE {

    using Expload.Pravda;
    using System;

    [Program]
    public class MyProgram {

        private Mapping<Bytes, Int64> Balance =
            new Mapping<Bytes, Int64>();
    }
}
```

We will now add a method to release our token:

```C#
namespace NAMESPACE {

    using Expload.Pravda;
    using System;

    [Program]
    public class MyProgram {

        private Mapping<Bytes, Int64> Balance =
            new Mapping<Bytes, Int64>();

        public void Emit(Bytes recipient, Int64 amount) {
            Require(Info.Sender() == Info.ProgramAddress(),
                "Only program owner can do that");
            Require(amount > 0, "Amount must be positive");
            Int64 lastBalance = Balance.GetOrDefault(recipient, 0);
            Int64 newBalance = lastBalance + amount;
            Balance[recipient] = newBalance;
        }

    }
}
```

The `Require` function throws an exception when the logical statement in the first argument is false.
`Info.Sender()` returns the address of the function caller, `Info.ProgramAddress()` — the address of the program owner’s wallet.

Then, let's add a method to transfer tokens and check balances:

```C#
namespace NAMESPACE {

    using Expload.Pravda;
    using System;

    [Program]
    public class MyProgram {

        private Mapping<Bytes, Int64> Balance =
            new Mapping<Bytes, Int64>();

        public void Give(Bytes recipient, Int64 amount) {
            Require(Info.Sender() == Info.ProgramAddress(),
                "Only program owner can do that");
            Require(amount > 0, "Amount must be positive");
            Int64 lastBalance = Balance.GetOrDefault(recipient, 0);
            Int64 newBalance = lastBalance + amount;
            Balance[recipient] = newBalance;
        }

        public void Transfer(Bytes recipient, Int64 amount) {
            Require(amount > 0, "Amount must be positive");

            Int64 lastSenderBalance =
                Balance.GetOrDefault(Info.Sender(), 0);
            Require(lastSenderBalance >= amount,
                "Not enough tokens");
            Int64 newSenderBalance = lastSenderBalance - amount;
            Balance[Info.Sender()] = newSenderBalance;

            Int64 lastBalance = Balance.GetOrDefault(recipient, 0);
            Int64 newBalance = lastBalance + amount;
            Balance[recipient] = newBalance;
        }

        public Int64 GetBalance(Bytes address){
            return Balance.GetOrDefault(address, 0);
        }
    }
}
```

## Deploying the Code
Refer to [compilation](https://developers.expload.com/documentation/pravda/#compiling-the-program)
and [deployment](https://developers.expload.com/documentation/pravda/#deploying-to-testnet)
sections of the ["Getting started"](https://developers.expload.com/documentation/pravda/) guide.

