using System;
using Com.Expload;

[Program]
class MyProgram {
    public void system() {
        Bytes owner = Info.Owner(Bytes.EMPTY);
        long balance = Info.Balance(Bytes.EMPTY);
    }
}

class MainClass {
    public static void Main() {}
}