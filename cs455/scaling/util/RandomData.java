package cs455.scaling.util;

import java.util.Random;

/**
 * Generates a random byte array
 * @author Owner Theresa Wellington
 *
 */
public class RandomData {
	
	public static byte[] generateData(int arraySize){
		byte[] randomData = new byte[arraySize];
		Random generator = new Random();
		generator.nextBytes(randomData);
		return randomData;
	}

}
