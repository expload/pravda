using System;
using Com.Expload;

public class HelloWorld
{
    public Mapping<String, String> strings;

    public void distributeSalary() {
        string salary = "za" + "pupu";
        string lu = "lu";
        string pa = "pa";
        string user = lu + pa;

        strings.put(user, salary);
        if (strings.exists("lupa")) {
          strings.put("pupa", "");
        }

        char c0 = salary[0];
        char c1 = user[3];
        string up = user.Substring(1, 2);
        // string upa = user.Substring(1); not implemented yet
    }

    static public void Main ()
    {
    }
}