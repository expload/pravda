using System;
using Expload.Pravda;

[Program]
public class System
{
    public void TestSystemMethods()
    {
        long balance = Info.Balance(Bytes.EMPTY);
        Bytes voidAddress = Bytes.VOID_ADDRESS;
        Bytes programAddress = Info.ProgramAddress();
    }

    public static void Main() {}
}