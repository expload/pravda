# Game integration

## Typical use cases

### Crypto-assets and Marketplace

The most common use case of Expload integration is replacing ordinary 
game assets with crypto-assets. Users' assets ownership is protected by 
Pravda Blockchain, users are also given the opportunity to sell assets for 
XGold and XPlatinum on Expload Marketplace.  
  
Detailed marketplace integration guide can be 
found [here](https://github.com/expload/auction).
  
### XGold-based monetization

By replacing your premium-currency (gold/diamonds/... bought 
for real-life money) with XGold you gain a list of opportunities 
including:  
- No need for developing any payments in your game - players buy XGold 
directly from Expload and you can just use Expload's and XGold's easy API
- Allow players from other games on Expload Platform to comfortably 
migrate to your game from other ones - sell items in one game, 
buy in another one!
- Implement different kinds of economic concepts in your game: allow 
users to play for money, or let them rent each other virtual property - 
only your imagination is the limit!

## XGold and XPlatinum

### XGold

XGold is basically not a real cryptocurrency, but a crypto-token, emitted 
by Expload and sold to players for a fixed price. Players can not exchange 
XGold for real money, they can only spend it in-game. XGold spent in-game 
goes to game developers, and they can exchange it for real 
money with Expload.

### XPlatinum

XPlatinum is a fully-fledged cryptocurrency, which can only be bought or 
sold on cryptocurrency exchange services. Some usage of XPlatinum may 
require additional personal data, such as ID card / passport verification. 

## Unity integration example

We will use one of Expload's games, Expulsum, as an example. Expulsum is a 
card game, based on 
[Game of Pure Strategy, invented by Merrill Flood in the 1930s](https://en.wikipedia.org/wiki/Goofspiel).

### Contract Example

The contract we are to inspect processes gaming for money - players make bets, winners take 
it all, losers lose it all. 
  
```C#
namespace Expload
{

    using Pravda;
    using System;

    [Program]
    public class Game {
        
        // Mapping storing people's bets
        private Mapping<string, long> bets = new Mapping<string, long>();
        
        // XGold program address
        private Bytes _XGAddress = Bytes.VOID_ADDRESS;

        private void AssertIsProgramOwner()
        {
            if (Info.Sender() != Info.ProgramAddress()){
                Error.Throw("Only program owner can do this.");
            }
        }

        public void SetXGAddress(Bytes address)
        {
            AssertIsProgramOwner();
            _XGAddress = address;
        }

        public void MakeBet(string roomId, long bet)
        {
            ProgramHelper.Program<XGold>(_XGAddress).Spend(Info.ProgramAddress(), bet);
            bets[StdLib.BytesToHex(Info.Sender()) + roomId] = bet;
        }

        public bool CheckBet(Bytes address, string roomId, long bet)
        {
            return bets[StdLib.BytesToHex(address) + roomId] == bet;
        }

        public void GivePrize(Bytes winner, long amount)
        {
            AssertIsProgramOwner();
            ProgramHelper.Program<XGold>(_XGAddress).Refund(Info.ProgramAddress(), 
                winner, amount);
        }

        public void RefundBet(Bytes address, string roomId)
        {
            AssertIsProgramOwner();
            ProgramHelper.Program<XGold>(_XGAddress).Refund(Info.ProgramAddress(), 
                address, bets[StdLib.BytesToHex(address)  + roomId]);
            bets[StdLib.BytesToHex(address) + roomId] = 0;
        }
    }
}
```
  
Let's see how this works.  
First things first, game server's matchmaking service finds 3 players, who are to play 
together, and gives them a `roomId`. Then players are to call `MakeBet` function and pay 
the amount of XGold needed. The game server waits until all 3 players' `CheckBet` returns 
true, and then starts the game. If some player take too long to pay, the game is cancelled, 
bets that were already made are refunded with `RefundBet` function. If the game successfully 
proceeds till its end, the game server gives away the prizes using `GivePrize` function.

### Unity Code Sample

In this case, all Unity game client has to do is call `MakeBet` function, everything else 
is handled server-side. But before we are able to call any functions, we should handle 
Expload App authentication and get user's nickname to display in-game.  
Let's see how Expulsum developers solved those issues:
  
All of those functions use 
[dApp API](https://developers.expload.com/documentation/pravda/integration/dapp-api/), every 
section provides a link to appropriate dApp API documentation part.

#### Game server authorization   
[dApp API](https://developers.expload.com/documentation/pravda/integration/dapp-api/#check-the-user-was-authenticated)
  
```C#
public static async Task<string> Authenticate()
{
    try
    {
        var message = Encoding.UTF8.GetBytes("DEPS").ToHex();

        var signed = await dappUrl
            .AppendPathSegment("auth")
            .WithTimeout(10)
            .ConfigureRequest(c => c.JsonSerializer = camelCaseSerializer)
            .PostJsonAsync(new { App = "DEPS", Bytes = message })
            .ReceiveJson<PravdaResponce<SignResponce>>();

        return signed.Data.SignedData;
    }
    catch (Exception ex)
    {
        throw new NetworkingException(NetworkingExceptionReason.ExploadApp, ex);
    }
}
```
  
#### Get user's address   
[dApp API](https://developers.expload.com/documentation/pravda/integration/dapp-api/#get-current-user-address)
  
```C#    
public static async Task<string> GetAddress()
{
    try
    {
        var addressResp = await dappUrl
            .AppendPathSegment("address")
            .WithTimeout(10)
            .ConfigureRequest(c => c.JsonSerializer = camelCaseSerializer)
            .GetJsonAsync<PravdaResponce<string>>();

        return addressResp.Data;
    }
    catch (Exception ex)
    {
        throw new NetworkingException(NetworkingExceptionReason.ExploadApp, ex);
    }
}
```
  
#### Get user's nickname  
[dApp API](https://developers.expload.com/documentation/pravda/integration/dapp-api/#get-username-by-pravda-address)

```C#
public static async Task<string> GetNickname(string address)
{
    try
    {
        var nicknameResp = await dappUrl
            .AppendPathSegment("username")
            .WithTimeout(10)
            .SetQueryParam("address", address)
            .ConfigureRequest(c => c.JsonSerializer = camelCaseSerializer)
            .GetJsonAsync<PravdaResponce<string>>();

        return nicknameResp.Data;
    }
    catch (Exception ex)
    {
        throw new NetworkingException(NetworkingExceptionReason.ExploadApp, ex);
    }
}
```
  
#### Call a Pravda program method  
[dApp API](https://developers.expload.com/documentation/pravda/integration/dapp-api/#call-program-method)

```C#
public static async Task<PravdaResponce<PravdaTransactionResult>> CallMethod(string address, string method, params object[] args)
{
    var call = new PravdaMethodCall()
    {
        Address = address,
        Method = method,
        Args = new List<object>(args)
    };

    try
    {
        var result = await dappUrl
            .AppendPathSegment("program")
            .AppendPathSegment("method")
            .WithTimeout(60)
            .ConfigureRequest(c => c.JsonSerializer = camelCaseSerializer)
            .PostJsonAsync(call)
            .ReceiveJson<PravdaResponce<PravdaTransactionResult>>();

        return result;
    }
    catch (Exception ex)
    {
        throw new NetworkingException(NetworkingExceptionReason.ExploadApp, ex);
    }
}
```
  
> Full source code file can be found 
> [here](https://gist.github.com/UnimaginaryUnit/ac1f8a3d5df32b1dbef0287f487b759b).

### Tips and tricks

- Don't forget about permissions. You may want to make some functions in your Pravda 
programs gameserver-only or owner-only. This can be done using `Require()`
- Don't overload Pravda programs. Put as much logic as you can into the 
game server, as all the operations in blockchain cost XPlatinum 
- All XGold-related and Marketplace-related operations' costs are paid 
by Expload 
- Automatic Pravda program deploy/update pipeline might be a really 
good idea. Here is an example: 
[link](https://github.com/expload/auction/blob/master/deploy.sh) 