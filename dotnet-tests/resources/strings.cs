using System;
using Expload.Pravda;

[Program]
public class ProgramStrings
{
    public Mapping<String, String> strings = new Mapping<String, String>();

    public void distributeSalary() {
        string salary = "za" + "user1";
        string us = "us";
        string er2 = "er2";
        string user = us + er2;

        strings.put(user, salary);
        if (strings.exists("user1")) {
          strings.put("user2", "");
        }

        char c0 = salary[0];
        char c1 = user[3];
        string up = user.Substring(1, 2);
    }

    static public void Main () {}
}