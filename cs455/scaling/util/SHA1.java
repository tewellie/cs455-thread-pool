package cs455.scaling.util;

import java.math.BigInteger;
import java.security.*;

/**
 * creates a hash of the byte[] passed in
 * @author Owner Theresa Wellington
 *
 */
public class SHA1 {

	public static String SHA1FromBytes(byte[] data){
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		
		return hashInt.toString(16);
	}
}
