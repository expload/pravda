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

[Program]
class MyProgram {
    Mapping<Address, int> balances = null;
    Address sender = null;

    public int balanceOf(Address tokenOwner) {
        return balances.getDefault(tokenOwner, 0);
    }

    public void transfer(Address to, int tokens) {
        if (tokens > 0) {
            if (balances.getDefault(sender, 0) >= tokens) {
                balances.put(sender, balances.getDefault(sender, 0) - tokens);
                balances.put(to, balances.getDefault(to, 0) + tokens);
            }
        }
    }
}

class MainClass {
    public static void Main() {
    }
}