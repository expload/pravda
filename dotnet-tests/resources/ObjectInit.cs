using System;
using Expload.Pravda;

public class A
{
   private byte b;
   private short s;
   private double d;
   private int i;
   private string str;
   private Bytes bs;
}

[Program]
public class ObjectInit
{
    public void TestObjectInit()
    {
        A a = new A();
    }

   public static void Main() {}
}