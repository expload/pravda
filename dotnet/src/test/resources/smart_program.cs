using System;
using Com.Expload;

[Program]
class MyProgram {
    Mapping<Address, int> balances = null;
    Address sender = null;

    public int balanceOf(Address tokenOwner) {
        return balances.getDefault(tokenOwner, 0);
    }

    public void transfer(Address to, int tokens) {
        if (tokens > 0) {
            if (balances.getDefault(sender, 0) >= tokens) {
                balances.put(sender, balances.getDefault(sender, 0) - tokens);
                balances.put(to, balances.getDefault(to, 0) + tokens);
            }
        }
    }
}

class MainClass {
    public static void Main() {
    }
}