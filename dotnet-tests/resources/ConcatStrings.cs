using System;
using Expload.Pravda;

[Program]
public class ConcatStrings
{
    public string TestConcatStrings()
    {
        string s = "s";
        string c2 = s + s + "2";
        string c3 = s + s + s + "3";
        string c4 = s + s + s + s + "4";
        string c5 = s + s + s + s + s + "5";
        string c6 = s + s + s + s + s + s + "6";
        string c7 = s + s + s + s + s + s + s + "7";
        string c8 = s + s + s + s + s + s + s + s + "8";
        string c9 = s + s + s + s + s + s + s + s + s + "9";
        string c10 = s + s + s + s + s + s + s + s + s + s + "10";

        return c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9 + c10;
    }

    static public void Main () {}
}