using System;
using io.mytc.pravda;

namespace io.mytc.pravda {

    // our special attribute, it will be a simple library
    public class Program : Attribute {}

    // access to the storage
    public abstract class Mapping<K, V> {
       public abstract V get(K key);
       public abstract bool exists(K key);
       public abstract void put(K key, V value);

       public V getDefault(K key, V def) {
          if (!this.exists(key)) {
              return def;
          } else {
              return this.get(key);
          }
       }
    }

    public class Address {}
    public class Data {}
    public class Word {}
}

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
    }

    static public void Main ()
    {
    }
}