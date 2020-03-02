package cs455.scaling.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

    public Hasher() {
    }


    public static String SHA1FromBytes(byte[] data) {
        MessageDigest digest;
        byte[] hash = null;

        try {
            digest = MessageDigest.getInstance("SHA1");
            hash = digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algorithm doesn't exist.");
            e.printStackTrace();
        }
        BigInteger hashInt = new BigInteger(1, hash);

        //Potential Issue. Feb 21, 11:00 echo, non-constant hash length.
		//need to pad, or also send the length first, then the hash
        return hashInt.toString(16);

    }

}
