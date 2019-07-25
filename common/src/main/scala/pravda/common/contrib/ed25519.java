package pravda.common.contrib;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Package ed25519 implements the Ed25519 signature algorithm. See
 * http://ed25519.cr.yp.to/.
 *
 * This code is a port of the public domain, "ref10" implementation of ed25519
 * from SUPERCOP.
 */
public final class ed25519 {

    /**
     * GenerateKey generates a public/private key pair
     *
     * @param publicKey empty byte array size 32
     * @param privateKey random byte array size 64
     */
    public static void generateKey(byte[] publicKey, byte[] privateKey) {
        Ed25519PrivateKeyParameters pk = new Ed25519PrivateKeyParameters(privateKey, 0);
        System.arraycopy(pk.generatePublicKey().getEncoded(), 0, publicKey, 0, 32);
        System.arraycopy(publicKey, 0, privateKey, 32, 32);
    }

    private static final SecureRandom random = new SecureRandom();

    /**
     * Sign signs the message with privateKey and returns a signature.
     *
     * @param privateKey copy of private key 64 bytes length. array will be mutated
     * @param message arbitrary length message
     */
    public static byte[] sign(byte[] privateKey, byte[] message) {
        byte[] pk = new byte[Ed25519.PUBLIC_KEY_SIZE];
        Ed25519.generatePublicKey(privateKey, 0, pk, 0);
        byte[] sig = new byte[Ed25519.SIGNATURE_SIZE];
        Ed25519.sign(privateKey, 0, message, 0, message.length, sig, 0);
        return sig;
    }

    /**
     * Verify returns true iff sig is a valid signature of message by publicKey.
     */
    public static boolean verify(byte[] publicKey, byte[] message, byte[] sig){
        return Ed25519.verify(sig, 0, publicKey, 0, message, 0, message.length);
    }

    private ed25519() {}  // Not instantiable

    // ------
}
