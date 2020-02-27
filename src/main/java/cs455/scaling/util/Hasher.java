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
			System.out.println("Algorithm doesn't exist");
			e.printStackTrace();
		}
		 BigInteger hashInt = new BigInteger(1, hash);
		 return hashInt.toString(16);
		 
		}

}
