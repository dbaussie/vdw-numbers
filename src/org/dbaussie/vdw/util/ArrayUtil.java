package org.dbaussie.vdw.util;

/**
 * General purpose Array manipulation utilities
 * @author dbaussie
 *
 */
public class ArrayUtil {

	/**
	 * Copies count entries from the src to dest arrays shifting all digits to the left by 1
	 * The dest array must be large enough to accommodate the digits and may be the same array as the src.
	 * Assumes the most significant digit is stored at index 0
	 * If the src array has less than count+1 digits then 0's are inserted as necessary
	 * 
	 * @param src - the source array
	 * @param dest - the destination array
	 * @param count - the number of entries to shift   
	 */

	static public void shiftLeft(int[] src,int[] dest,int count) {
		for (int i=0; i<count-1; i++) {
			dest[i] = src[i+1];
		}
		dest[count-1] = 0;
	}

	/**
	 * Copies count entries from the src to dest arrays shifting all digits to the right by 1
	 * The dest array must be large enough to accommodate the digits and may be the same array as the src.
	 * Assumes the most significant digit is stored at index 0
	 * 0's are used for the new most significant digit.
	 * 
	 * @param src - the source array
	 * @param dest - the destination array
	 * @param count - the number of entries to shift   
	 */
	static public void shiftRight(int[] src,int[] dest,int count) {
		for (int i=count-1; i>0; i--) {
			dest[i] = src[i-1];
		}
		dest[0] = 0;
	}

	/**
	 * Toggles count values of a binary array
	 * 
	 * @param data - the array is assumed to hold either 0 or 1
	 * @param count - the number of values to change
	 */
	static public void complementBinaryArray(int[] data,int count) {
		for (int i=0; i<count; i++) {
			data[i] = 1 - data[i];
		}
	}

	/**
	 * Compares two integer arrays
	 * Returns   0 if the arrays are equal
	 * 			-1 if array1 < array2
	 * 			+1 if array1 > array2
	 * 
	 * @param array1 - the first array
	 * @param array2 - the second array
	 * @param count - the number of entries to compare
	 */
	static public int compare(int[] array1,int[] array2,int count) {
		for (int i=0; i<count; i++) {
			if (array1[i] < array2[i] ) {
				return -1;
			}
			if (array1[i] > array2[i] ) {
				return 1;
			}
		}
		return 0;
	}
	
	static public String toString(int[] data,int digitCount) {
		String result = "";
		for (int i=0; i<digitCount; i++) {
			result += data[i];
		}
		return result;
	}
}
