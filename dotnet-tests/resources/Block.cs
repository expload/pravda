using System;
using Expload.Pravda;

[Program]
public class Block
{
    public void TestBlockMethods()
    {
        long height = Info.Height();
        Bytes hash = Info.LastBlockHash();
    }

    public static void Main() {}
}