using System;
using Expload.Pravda;

[Program]
public class SmartProgram
{
    Mapping<Bytes, int> Balances = new Mapping<Bytes, int>();

    public int BalanceOf(Bytes tokenOwner)
    {
        return Balances.getDefault(tokenOwner, 0);
    }

    public void Transfer(Bytes to, int tokens)
    {
        if (tokens > 0) {
            if (Balances.getDefault(Info.Sender(), 0) >= tokens) {
                Balances.put(Info.Sender(), Balances.getDefault(Info.Sender(), 0) - tokens);
                Balances.put(to, Balances.getDefault(to, 0) + tokens);
            }
        }
    }

    public void Emit(Bytes owner, int tokens)
    {
        if (tokens > 0) {
            Balances.put(owner, Balances.getDefault(owner, 0) + tokens);
        }
    }

    public static void Main() {}
}