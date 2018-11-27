using System;
using Expload.Pravda;

[Program]
public class SmartProgram
{
    Mapping<Bytes, int> Balances = new Mapping<Bytes, int>();

    public int BalanceOf(Bytes tokenOwner)
    {
        return Balances.GetOrDefault(tokenOwner, 0);
    }

    public void Transfer(Bytes to, int tokens)
    {
        if (tokens > 0) {
            if (Balances.GetOrDefault(Info.Sender(), 0) >= tokens) {
                Balances[Info.Sender()] = Balances.GetOrDefault(Info.Sender(), 0) - tokens;
                Balances[to] = Balances.GetOrDefault(to, 0) + tokens;
            }
        }
    }

    public void Emit(Bytes owner, int tokens)
    {
        if (tokens > 0) {
            Balances[owner] = Balances.GetOrDefault(owner, 0) + tokens;
        }
    }

    public static void Main() {}
}