using System;
using Expload.Pravda;

[Program]
public class Arithmetics {
    private int X = 10;

    public int TestBasicOperations()
    {
        int a = X + 2;
        int b = X * 2;
        int c = X / 2;
        int d = X % 2;
        return ((a + b + 42) * c + d) / 1337;
    }

    public static void Main() {}
}