using System;
using Expload.Pravda;

[Program]
public class Strings
{
    private Mapping<String, String> StringsMapping = new Mapping<String, String>();

    public void TestStrings()
    {
        string salary = "za" + "user1";
        string us = "us";
        string er2 = "er2";
        string user = us + er2;

        StringsMapping[user] = salary;
        if (StringsMapping.ContainsKey("user1")) {
          StringsMapping["user2"] = "";
        }

        char c0 = salary[0];
        char c1 = user[3];
        string up = user.Substring(1, 2);
    }

    static public void Main () {}
}