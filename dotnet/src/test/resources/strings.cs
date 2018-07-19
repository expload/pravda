using System;
using Com.Expload;

[Program]
public class ProgramStrings
{
    public Mapping<String, String> strings = new Mapping<String, String>();

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
    }

    static public void Main () {}
}