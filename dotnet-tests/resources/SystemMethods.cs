using System;
using Expload.Pravda;

[Program]
public class SystemMethods
{
    public void TestSystemMethods()
    {
        long balance = Info.Balance(Bytes.EMPTY);
        Bytes voidAddress = Bytes.VOID_ADDRESS;
        Bytes programAddress = Info.ProgramAddress();
    }

    public static void Main() {}
}