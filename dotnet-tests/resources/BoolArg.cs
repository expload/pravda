using System;
using Expload.Pravda;

[Program]
public class BoolArg
{
    public int TestBoolArg(bool condition, int trueResult, int falseResult)
    {
        if (condition) {
            return trueResult;
        } else {
            return falseResult;
        }
    }

    public static void Main() {}
}