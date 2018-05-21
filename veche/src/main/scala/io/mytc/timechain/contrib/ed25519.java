package io.mytc.timechain.contrib;

import org.whispersystems.curve25519.java.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.whispersystems.curve25519.java.ge_double_scalarmult.ge_double_scalarmult_vartime;
import static org.whispersystems.curve25519.java.ge_p3_tobytes.ge_p3_tobytes;
import static org.whispersystems.curve25519.java.ge_scalarmult_base.ge_scalarmult_base;
import static org.whispersystems.curve25519.java.ge_tobytes.ge_tobytes;
import static org.whispersystems.curve25519.java.sc_muladd.sc_muladd;
import static org.whispersystems.curve25519.java.sc_reduce.sc_reduce;

/**
 * Package ed25519 implements the Ed25519 signature algorithm. See
 * http://ed25519.cr.yp.to/.
 *
 * This code is a port of the public domain, "ref10" implementation of ed25519
 * from SUPERCOP.
 */
public final class ed25519 {

    /**
     * GenerateKey generates a public/private key pair using randomness from rand.
     *
     * @param publicKey empty byte array size 32
     * @param privateKey random byte array size 64
     */
    public static void generateKey(byte[] publicKey, byte[] privateKey) throws NoSuchAlgorithmException {
        MessageDigest h = MessageDigest.getInstance("SHA-512");
        byte[] digest = h.digest(Arrays.copyOfRange(privateKey, 0, 32));

        digest[0] &= 248;
        digest[31] &= 127;
        digest[31] |= 64;

        ge_p3 A = new ge_p3();
        ge_scalarmult_base(A, digest.clone());
        ge_p3_tobytes(publicKey, A);

        System.arraycopy(publicKey, 0, privateKey, 32, 32);
    }

    /**
     * Sign signs the message with privateKey and returns a signature.
     *
     * @param privateKey copy of private key 64 bytes length. array will be mutated
     * @param message arbitrary length message
     */
    public static byte[] sign(byte[] privateKey, byte[] message) throws NoSuchAlgorithmException {
        MessageDigest h = MessageDigest.getInstance("SHA-512");
        h.update(privateKey, 0, 32);

        byte[] hramDigest;
        byte[] expandedSecretKey = new byte[32];
        byte[] digest1;
        byte[] messageDigest;
        digest1 = h.digest();
        System.arraycopy(digest1, 0, expandedSecretKey, 0, 32);

        expandedSecretKey[0] &= 248;
        expandedSecretKey[31] &= 63;
        expandedSecretKey[31] |= 64;

        h.reset();
        h.update(digest1, 32, 32);
        h.update(message);
        messageDigest = h.digest();

        sc_reduce(messageDigest);
        ge_p3 R = new ge_p3();
        ge_scalarmult_base(R, messageDigest);

        byte[] encodedR = new byte[32];
        ge_p3_tobytes(encodedR, R);

        h.reset();
        h.update(encodedR);
        h.update(privateKey, 32, 32);
        h.update(message);
        hramDigest = h.digest();
        sc_reduce(hramDigest);

        byte[] s = new byte[32];
        sc_muladd(s, hramDigest, expandedSecretKey, messageDigest);

        byte[] signature = new byte[64];
        System.arraycopy(encodedR, 0, signature, 0, 32);
        System.arraycopy(s, 0, signature, 32, 32);
        return signature;
    }

    /**
     * Verify returns true iff sig is a valid signature of message by publicKey.
     */
    public static boolean verify(byte[] publicKey, byte[] message, byte[] sig) throws NoSuchAlgorithmException {
        if(sig.length < 64)
            return  false;

        if ((sig[63] & 224) != 0) {
            return false;
        }

        ge_p3 A = new ge_p3();
        if (ge_frombytes_negate_vartime(A, publicKey) < 0) {
            return false;
        }

        MessageDigest h = MessageDigest.getInstance("SHA-512");
        h.update(sig, 0, 32);
        h.update(publicKey);
        h.update(message);
        byte[] digest = h.digest();

        sc_reduce(digest);
        byte[] hReduced = new byte[32];
        System.arraycopy(digest, 0, hReduced, 0, 32);

        ge_p2 R = new ge_p2();
        byte[] b = new byte[32];
        System.arraycopy(sig, 32, b, 0, 32);
        ge_double_scalarmult_vartime(R, hReduced, A, b);

        byte[] checkR = new byte[32];
        ge_tobytes(checkR, R);

        // Constant time equality check
        boolean eq = true;
        for (int i = 0; i < 32; i++)
            eq = sig[i] == checkR[i] && eq;
        return eq;
    }

    private ed25519() {}  // Not instantiable

    // ------

    private static int[] d = {
        //CONVERT #include "d.h"
        -10913610,13857413,-15372611,6949391,114729,-8787816,-6275908,-3247719,-18696448,-12055116
    } ;

    private static int[] sqrtm1 = {
        //CONVERT #include "sqrtm1.h"
        -32595792,-7943725,9377950,3500415,12389472,-272473,-25146209,-2005654,326686,11406482
    } ;

    private static int ge_frombytes_negate_vartime(ge_p3 h,byte[] s)
    {
        int[] u = new int[10];
        int[] v = new int[10];
        int[] v3 = new int[10];
        int[] vxx = new int[10];
        int[] check = new int[10];

        fe_frombytes.fe_frombytes(h.Y,s);
        fe_1.fe_1(h.Z);
        fe_sq.fe_sq(u,h.Y);
        fe_mul.fe_mul(v,u,d);
        fe_sub.fe_sub(u,u,h.Z);       /* u = y^2-1 */
        fe_add.fe_add(v,v,h.Z);       /* v = dy^2+1 */

        fe_sq.fe_sq(v3,v);
        fe_mul.fe_mul(v3,v3,v);        /* v3 = v^3 */
        fe_sq.fe_sq(h.X,v3);
        fe_mul.fe_mul(h.X,h.X,v);
        fe_mul.fe_mul(h.X,h.X,u);    /* x = uv^7 */

        fe_pow22523.fe_pow22523(h.X,h.X); /* x = (uv^7)^((q-5)/8) */
        fe_mul.fe_mul(h.X,h.X,v3);
        fe_mul.fe_mul(h.X,h.X,u);    /* x = uv^3(uv^7)^((q-5)/8) */

        fe_sq.fe_sq(vxx,h.X);
        fe_mul.fe_mul(vxx,vxx,v);
        fe_sub.fe_sub(check,vxx,u);    /* vx^2-u */
        if (fe_isnonzero.fe_isnonzero(check) != 0) {
            fe_add.fe_add(check,vxx,u);  /* vx^2+u */
            if (fe_isnonzero.fe_isnonzero(check) != 0) return -1;
            fe_mul.fe_mul(h.X,h.X,sqrtm1);
        }

        if (fe_isnegative.fe_isnegative(h.X) == ((s[31] >>> 7) & 0x01)) {
            fe_neg.fe_neg(h.X,h.X);
        }

        fe_mul.fe_mul(h.T,h.X,h.Y);
        return 0;
    }
}
