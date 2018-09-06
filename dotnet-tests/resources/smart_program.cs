using System;
using Com.Expload;

[Program]
class MyProgram {
    Mapping<Bytes, int> balances = new Mapping<Bytes, int>();

    public int balanceOf(Bytes tokenOwner) {
        return balances.getDefault(tokenOwner, 0);
    }

    public void transfer(Bytes to, int tokens) {
        if (tokens > 0) {
            if (balances.getDefault(Info.Sender(), 0) >= tokens) {
                balances.put(Info.Sender(), balances.getDefault(Info.Sender(), 0) - tokens);
                balances.put(to, balances.getDefault(to, 0) + tokens);
            }
        }
    }

    public static void Main() {}
}