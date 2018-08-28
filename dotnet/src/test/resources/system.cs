using System;
using Com.Expload;

[Program]
class MyProgram {
    public void system() {
        Bytes owner = Info.Owner(Bytes.EMPTY);
        long balance = Info.Balance(Bytes.EMPTY);
        Bytes voidAddress = Bytes.VOID_ADDRESS;
        Bytes programAddress = Info.ProgramAddress();
    }

    public static void Main() {}
}