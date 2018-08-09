using System;
using Com.Expload;

[Program]
class MyProgram {
    public void Throw()
    {
        Error.Throw("Oops!");
    }
}

class MainClass {
    public static void Main() {}
}