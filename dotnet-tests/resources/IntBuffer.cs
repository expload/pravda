using System;
using Expload.Pravda;

public class IntBuffer
{
    private int size = 0;
    private int[] buffer;

    public IntBuffer()
    {
        buffer = new int[16];
    }

    public IntBuffer(int initSize)
    {
        buffer = new int[initSize];
    }

    public void Append(int elem)
    {
        if (size == buffer.Length)
        {
            int[] newBuffer = new int[buffer.Length * 2 + 1];
            for (int i = 0; i < buffer.Length; i++) {
                newBuffer[i] = buffer[i];
            }
            buffer = newBuffer;
        }

        buffer[size] = elem;
        size += 1;
    }

    public int this[int i] { get { return buffer[i]; } set { buffer[i] = value; } }
}

[Program]
public class IntBufferProgram
{
    public string TestBuffer()
    {
        IntBuffer buff = new IntBuffer(2);
        buff.Append(1);
        buff.Append(3);
        buff.Append(5);
        buff.Append(7);
        buff.Append(9);
        buff.Append(11);
        buff.Append(13);
        buff.Append(15);
        buff.Append(17);
        int a = buff[0];
        int b = buff[1];
        int c = buff[2];
        buff[1] = 10;
        int d = buff[1];
        return Convert.ToString(a) + Convert.ToString(b) + Convert.ToString(c) + Convert.ToString(d);
    }

   public static void Main() {}
}