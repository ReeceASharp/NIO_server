package cs455.scaling.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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

        //leading zeros are dropped. Pad to 20 bytes, but the hash is in
        //hex, so 40 characters

        String standardHash = standardize(hashInt.toString(16));

        return standardHash;

    }

    private static String standardize(String hash) {
        StringBuilder sb = new StringBuilder(hash);


        int zerosToAppend = 40 - hash.length();
        if (zerosToAppend > 0) {
            char[] zeros = new char[zerosToAppend];
            Arrays.fill(zeros, '0');
            sb.insert(0, zeros);
        }

        return sb.toString();
    }

}
