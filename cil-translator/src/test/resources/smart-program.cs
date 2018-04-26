// our special attribute, it will be a simple library
public class Program : Attribute {}

[Program]
class MyProgram : ProgramStorage {

    static int doSmth()

    // all fields are stored in the persistent storage
    public int fa = 1;
    public int fb = 2;




    // receive code is stored in a separate account
    // when receive is called it translates to RUN op-code
    void receive(int a, int b, int c) {
        fa = a;
        fb = b * c;
    }
}