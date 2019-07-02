# Your first program on Pravda

Let's go throughout the process of developing and deploying a Pravda 
program using a basic token program as an example. We will create 
a fungible token, which can be emitted and transferred to other wallets.

## Pre-requirements

- It is **highly** recommended to understand the basic principles of smart-contracts
- You have to [install Pravda CLI](https://developers.expload.com/documentation/pravda/#installation)
- You will also need [.NET Core SDK 2.1](https://dotnet.microsoft.com/download/dotnet-core/2.1)

## Writing the code

Let's start by [importing Pravda program template](https://developers.expload.com/documentation/pravda/#importing-project-template). 
You will get the following code (except that namespace and program 
name will probably be different):  
  
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
  
Basically, there is almost no difference between a usual C# code and our 
Pravda program. The only thing we are adding here - `Program` attribute. 
It indicates which class in our code is the actual "smart-contract".  
  
From now and so on we will use only term `Pravda program`, not `smart-contract`.
  
<details>
<summary>What is the difference between them?</summary>

> A `Pravda program` can be updated (you can change its code without changing 
> program's address) and sealed (you can "seal" program's code, making it 
> impossible to update), which contradicts the usual definition of 
> what a `smart-contract` is. So, technically, it is inaccurate to call
> Pravda programs smart-contracts, as they have different functionality.

</details>

Let's remove the `HelloWorld` function and a [mapping](https://en.wikipedia.org/wiki/Associative_array) storing users' 
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
  
Now, we are to add a way to emit our token:  
  
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
  
Function `Require` throws an exception if the logical statement in the 
first argument is false.  
`Info.Sender()` returns the address of function 
caller, `Info.ProgramAddress()` - address of the program owner wallet.
  
Then, let's add a way to transfer tokens and check balances:  
  
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

## Deploying the code
Refer to [compilation](https://developers.expload.com/documentation/pravda/#compiling-the-program) 
and [deployment](https://developers.expload.com/documentation/pravda/#deploying-to-testnet) 
sections of the ["Getting started"](https://developers.expload.com/documentation/pravda/) guide.