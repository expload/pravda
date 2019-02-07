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

    public long TestLastBlockTime()
    {
        return Info.LastBlockTime();
    } 

    public static void Main() {}
}