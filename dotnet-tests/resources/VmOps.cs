using System;
using Expload.Pravda;

[Program]
public class VmOps
{
    public void TestThrow()
    {
        Error.Throw("Oops!");
    }

    public static void Main() {}
}