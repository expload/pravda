using System;
using Expload.Pravda;

[Program]
class MyProgram {
    public void system() {
        long balance = Info.Balance(Bytes.EMPTY);
        Bytes voidAddress = Bytes.VOID_ADDRESS;
        Bytes programAddress = Info.ProgramAddress();
    }

    public static void Main() {}
}