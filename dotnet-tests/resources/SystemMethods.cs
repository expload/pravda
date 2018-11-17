using System;
using Expload.Pravda;

[Program]
public class SystemMethods
{
    public void TestSystemMethods()
    {
        long balance = Info.Balance(Bytes.VOID_ADDRESS);
        Bytes programAddress = Info.ProgramAddress();

        Actions.Transfer(Bytes.VOID_ADDRESS, 100L);
        Actions.TransferFromProgram(Bytes.VOID_ADDRESS, 200L);

        Bytes nullBytes = null;
    }

    public static void Main() {}
}