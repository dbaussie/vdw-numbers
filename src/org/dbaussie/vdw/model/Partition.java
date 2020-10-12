package org.dbaussie.vdw.model;

/**
 * The Partition class is used to represent a single partition of the first N natural numbers into k subsets.
 * 
 * One approach to this (and the one used here) is to use a k-ary integer with N digits to encode the selection
 * of numbers into subsets. This representation has several advantages.
 * 1) You can easily cycle through all permutations by simply incrementing the k-ary integer
 * 2) There is a natural ordering associated with this representation that can be useful
 * 
 * Since we need to support both large (ie >64 bit) and non binary integer representations we use and integer array to hold
 * the digits of the k-ary integer.
 * Also we interchangeably interpret the digit as both a numerical value and/or a coloring.
 * Because of this we refer to k as colorCount and N as digitCount throughout the implementation. 
 * 
 * @author dbaussie@lycos.com
 *
 */
public class Partition {

	// constants
	final static public int BLOCK_SIZE = 1000; // we allocate physical memory for the digit array in multiples of the block size in order to avoid unnecessary heap access

	// properties
	final private int colorCount; // the number of colors to use - this never changes once the partition is created
	public int digitCount; // the number of digits to hold

	// attributes
	protected int[] _digitColors; // holds the color of the digit indexed by position. Note numbers are stored backwards ie index 0 represents the most significant digit

	static public void main(String[] argv) {
		System.out.println("Testing increment");
		Partition ptn = new Partition(2,8);
		boolean done = false;
		while (!done) {
			System.out.println(""+ptn);
			done = !ptn.increment();
		}
	}

	public Partition(int colorCount,int digitCount) {
		this.colorCount = colorCount;
		ensureCapacity(this.digitCount);
		this.digitCount = digitCount;
	}
	
	/**
	 * Make sure that we have allocated enough blocks to hold the desired digitCount.
	 * If we need to reallocate a new array then first copy over the old values.
	 */
	private void ensureCapacity(int newDigitCount) {
		int blockCount = (newDigitCount-1) / BLOCK_SIZE + 1;
		int newSize = BLOCK_SIZE * blockCount;
		int oldSize = _digitColors==null ? 0 : _digitColors.length;
		int[] newStorage = _digitColors;
		if (newSize > oldSize) {
			newStorage = new int[newSize];
			for (int i=0; i<oldSize; i++) {
				newStorage[i] = _digitColors[i];
			}
		}
		_digitColors = newStorage;
	}

	/**
	 * Updates the digitCount to the new value allocating more blocks if needed
	 */
	public void setDigitCount(int newDigitCount) {
		ensureCapacity(newDigitCount);
		// clear out any old values for the new digits
		for (int d=digitCount; d<newDigitCount; d++) {
			_digitColors[d] = 0;
		}
		this.digitCount = newDigitCount;
	}

	/** 
	 * Returns the numerical value of the digit at the given position.
	 * No bounds checking is done.
	 * 
	 * @param position - positions are 'backwards' with 0 representing the most significant digit 
	 * @return
	 */
	public int getDigit(int position) {
		return _digitColors[position];
	}

	/** 
	 * Returns the numerical value of the least significant digit.
	 */
	public int getLastDigit() {
		return _digitColors[digitCount-1];
	}

	/** 
	 * Sets the digit at the given position to the given value.
	 * No bounds checking is done.
	 * 
	 * @param position - positions are 'backwards' with 0 representing the most significant digit 
	 * @param value - the numerical value to use for the given digit 
	 */
	public void setDigitAt(int position,int value) {
		_digitColors[position] = value;
	}

	/** 
	 * Sets the last digit to the given value.
	 * No bounds checking is done.
	 * 
	 * @param value - the numerical value to use for the last digit 
	 */
	public void setLastDigit(int value) {
		_digitColors[digitCount-1] = value;
	}

	/** 
	 * Returns the character representation of the digit at the given position.
	 * Digits 0..9 are represented by chars '0'..'9' subsequent values are given by 'A'..'Z'
	 * No bounds checking is done.
	 * 
	 * @param position - positions are 'backwards' with 0 representing the most significant digit 
	 * @returns the specified char
	 */
	public char getDigitChar(int position) {
		int digit = _digitColors[position];
		return digit<10 ? (char)('0'+digit) : (char)('A'+digit-10);
	}
	
	/**
	 * Returns the underlying storage array. 
	 * Note: This array may contain extra unused digits as it will be a multiple of the block size
	 * @return
	 */
	public int[] getUnderlyingArray() {
		return _digitColors;
	}

	/** 
	 * Returns the digit string representing the current value stored in the Partition.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(digitCount);
		for (int d=0; d<digitCount; d++) {
			result.append(getDigitChar(d));
		}
		return result.toString();
	}

	/**
	 * Increments the value stored in the partition by 1.
	 * 
	 * @returns false if the increment fails (ie the value would wrap around back to 0) otherwise returns true
	 */
	public boolean increment() {
		for (int d=digitCount-1; d>=0; d--) {
			int newDigit = ++_digitColors[d];
			if (newDigit==colorCount) {
				_digitColors[d] = 0;
			} else {
				return true;
			}
		}
		return false;
	}

	/** 
	 * Replaces all digits after the given position with the highest color
	 * No bounds checking is done.
	 * 
	 * @param position - positions are 'backwards' with 0 representing the most significant digit 
	 */
	public void maskTrailingDigits(int position) {
		final int lastColor = colorCount - 1;
		for (int i=position+1; i<digitCount; i++) {
			_digitColors[i] = lastColor;
		}
	}

	/** 
	 * Clones the current partition
	 */
	public Partition clone() {
		Partition clone = new Partition(colorCount,digitCount);
		for (int d=0; d<digitCount; d++) {
			clone._digitColors[d] = _digitColors[d];
		}
		return clone;
	}
}
