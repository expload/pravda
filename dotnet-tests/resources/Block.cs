using System;
using Expload.Pravda;

[Program]
public class Block
{
    public long TestHeightMethod()
    {
        return Info.Height();
    }

    public Bytes TestLastBlockHash()
    {
        return Info.LastBlockHash();
    }

    public static void Main() {}
}