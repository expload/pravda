using System;
using Expload.Pravda;

[Program]
public class Closure
{
    public int TestClosure()
    {
        int e = 1;
        Func<int, int> d = x => x + e;
        return d(3);
    }

    public static void Main() {}
}