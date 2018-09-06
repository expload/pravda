using System;
using Com.Expload;

[Program]
class MyProgram {
    public Bytes Ripemd160(String input)
    {
        return StdLib.Ripemd160(input);
    }

    public bool ValidateEd25519Signature(Bytes pubKey, String message, Bytes sign)
    {
        return StdLib.ValidateEd25519Signature(pubKey, message, sign);
    }

    public static void Main() {}
}