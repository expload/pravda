using System;
using io.mytc.pravda;

namespace io.mytc.pravda {

    // our special attribute, it will be a simple library
    public class Program : Attribute {}

    // access to the storage
    public interface Mapping<K, V> {
       V get(K key);
       bool exists(K key);
       void put(K key, V value);

       V getDefault(K key, V def); /*{
          if (!this.exists(k)) {
              return def;
          } else {
              return this.get(k);
          }
       }*/
    }

    public class Address {}
    public class Data {}
    public class Word {}
}

[Program]
class MyProgram {
    Mapping<Address, int> balances = null;
    Address sender = null;

    public int balanceOf(Address tokenOwner) {
        return balances.get(tokenOwner);
    }

    public void transfer(Address to, int tokens) {
        if (balances.getDefault(sender, 0) >= tokens) {
            balances.put(sender, balances.getDefault(sender, 0) - tokens);
            balances.put(to, balances.getDefault(to, 0) + tokens);
        }
    }
}

class MainClass {
    public static void Main() {
    }
}