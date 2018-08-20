using Com.Expload;

public class Parent
{
    public Parent(int val)
    {

    }

    public virtual int AnswerPlus1()
    {
        return Answer() + 1;
    }

    public virtual int Answer()
    {
        return 0;
    }
}

public class A : Parent
{
    private int AVal;

    public A(int aVal): base(aVal)
    {
        this.AVal = aVal;
    }

    public override int Answer()
    {
        return AVal + 42;
    }
}

public class B: Parent
{
    private int BVal;

    public B(int bVal): base(bVal)
    {
        this.BVal = bVal;
    }

    public override int Answer()
    {
        return BVal + 43;
    }
}

[Program]
public class MyProgram
{
    public int Func()
    {
        Parent a = new A(100);
        Parent b = new B(200);
        int c = a.Answer() + b.Answer();
        int d = a.AnswerPlus1();
        int e = b.AnswerPlus1();
        return d + e;
    }

   public static void Main() {}
}