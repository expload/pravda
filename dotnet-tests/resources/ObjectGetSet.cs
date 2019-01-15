using System;
using Expload.Pravda;

public class SomeClass
{
    public int field1;
    public int field2 { get; set; }
    private int field3;

    public void SetField3(int field3)
    {
        this.field3 = field3;
    }

    public int GetField3()
    {
        return field3;
    }
}

[Program]
public class ObjectGetSet
{
    public int TestObjectGetSet()
    {
        var cls = new SomeClass();
        cls.field1 = 3;
        cls.field2 = 20;
        cls.SetField3(100);

        return cls.field1 + cls.field2 + cls.GetField3();
    }

   public static void Main() {}
}