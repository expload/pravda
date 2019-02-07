using System;
using Expload.Pravda;

public class SomeObj
{
    public int SomeInt;
    public string SomeStr;

    public SomeObj(int i, string s) {
        SomeInt = i;
        SomeStr = s;
    }
}

[Program]
public class Arrays
{
    public string TestObjectArrays()
    {
        SomeObj[] objs1 = new SomeObj[]{ new SomeObj(1, "str"), new SomeObj(2, "str2"), new SomeObj(3, "str3") };
        SomeObj[] objs2 = new SomeObj[2];
        objs2[0] = new SomeObj(4, "str4");
        objs2[1] = new SomeObj(5, "str5");

        return objs1[0].SomeStr + objs1[1].SomeStr + objs1[2].SomeStr + objs2[0].SomeStr + objs2[1].SomeStr;
    }

    static public void Main () {}
}