using System;
using Com.Expload;

public class A
{
   private int AVal;

   public A(int aVal)
   {
       this.AVal = aVal;
   }

   public int AnswerA()
   {
       return AVal + 42;
   }
}

public class B
{
    private int BVal;

    public B(int bVal)
    {
        this.BVal = bVal;
    }

    public int AnswerB()
    {
        return BVal + 43;
    }
}

[Program]
public class MyProgram
{
    public int Func()
    {
        var a = new A(100);
        var b = new B(200);
        int c = a.AnswerA() + b.AnswerB();
        return c;
    }

   public static void Main() {}
}