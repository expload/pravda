// our special attribute, it will be a simple library
public class Program : Attribute {}

[Program]
class MyProgram {

    // static fields are stored in heap as always
    public static int counter = 0;

    // static functions don't call any special op-codes
    public static int doSmth(int a, int b) {
        return a + b;
    }

    // all fields are stored in the persistent storage
    public int fa = 0;
    public int fb = 0;

    // receive code is stored in a separate account
    // when receive is called it translates to RUN op-code
    public void receive(int mode, int a, int b) {
        switch (mode) {
            case 1:
                fa = a + b;
                break;
            case 2:
                fb = a + b;
                break;
            case 3:
                fa = a;
                fb = b;
                break;
            case 4:
                fa = MyProgram.doSmth(a, b);
                fb = MyProgram.doSmth(a, b);
                break;
        }
    }

    // other functions are forbidden
    public void otherFunc(int arg1, int arg1) { // error
       int x = 2 + 2;
    }

    // all other stuff is also forbidden
    // to define
}