using System;
using Expload.Pravda;

[Program]
public class Stdlib
{
    public Bytes Ripemd160(String input)
    {
        return StdLib.Ripemd160(input);
    }

    public bool ValidateEd25519Signature(Bytes pubKey, String message, Bytes sign)
    {
        return StdLib.ValidateEd25519Signature(pubKey, message, sign);
    }

    public String BytesToHex(Bytes bytes)
    {
        return StdLib.BytesToHex(bytes);
    }

    public static void Main() {}
}