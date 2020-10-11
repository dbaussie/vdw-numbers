package org.dbaussie.vdw;

import java.util.ArrayList;
import java.util.Date;

import org.dbaussie.vdw.logging.Log;
import org.dbaussie.vdw.model.Partition;

abstract public class AbstractVdwGenerator {

	// attributes
	protected long _startTime;
	protected long _totalDuration; // total duration in ms
	protected int _result;

	// properties
	public int colorCount;
	public int sequenceLength;
	public int abortDigitCount = -1; // if this is not -1 the stop the search here
	public int initialDigitCount;
	public String algorithm;

	public AbstractVdwGenerator(int colorCount,int sequenceLength,int initialCount) {
		this.colorCount = colorCount;
		this.sequenceLength = sequenceLength;
		this.initialDigitCount = initialCount<=0 ? sequenceLength+1 : initialCount;
	}

	@Override
	public String toString() {
		return "W("+colorCount+","+sequenceLength+")";
	}

	abstract public void calculate();

	public void generate() {
		System.out.println("Computing "+toString()+" with algorithm "+algorithm);
		_startTime = new Date().getTime();
		calculate();
		long endTime = new Date().getTime();
		_totalDuration = endTime - _startTime;
		System.out.println("\nCalculated "+toString()+" = "+getResult()+" in "+getFormattedDuration());
	}

	protected boolean generateAllCertificates(Partition ptn,int digitCount,int maxd,ArrayList<Partition> certList) {
		Log.log("    Generating all certificates of length "+digitCount);
		ptn.setDigitCount(digitCount);
		ptn.setDigitAt(0,colorCount-1);
		boolean done = false;
		boolean foundAnyCert = false;
		while (!done) {
			int lastAPPosition = checkForAPFreenessAnyDigit(ptn,maxd,digitCount);
			boolean foundCert = lastAPPosition == -1;
			if (foundCert) {
				foundAnyCert = true;
				certList.add(ptn.clone());
			} else {
				ptn.maskTrailingDigits(lastAPPosition);
			}
			done = !ptn.increment();
		}
		return foundAnyCert;
	}

	protected int checkForAPFreenessFinalDigit(Partition ptn,int maxd,int digitCount) {
		int lastAPPosition = -1;
		boolean done = false;
		for (int d=1; d<=maxd && !done; d++) {
			int value = ptn.getLastDigit();
			lastAPPosition = checkForAPRightToLeft(ptn,digitCount-1,d,value);
			done = lastAPPosition != -1;
		}
		return lastAPPosition;
	}

	protected int checkForAPRightToLeft(Partition ptn,int position,int difference,int value) {
		int pos = position;
		for (int n=1; n<sequenceLength; n++) {
			pos -= difference;
			if (value != ptn.getDigit(pos)) {
				return -1;
			}
		}
		return position;
	}

	protected int checkForAPFreenessAnyDigit(Partition ptn,int maxd,int digitCount) {
		int lastAPPosition = -1;
		boolean done = false;
		int ub = digitCount;
		for (int d=1; d<=maxd && !done; d++) {
			ub = ub - (sequenceLength-1);
			for (int a=0; a<ub && !done; a++) { // ub = w-d*(N-1)
				int v = ptn.getDigit(a);
				lastAPPosition = checkForAPLeftToRight(ptn,a,d,v);
				done = lastAPPosition != -1;
			}
		}
		return lastAPPosition;
	}

	protected int checkForAPLeftToRight(Partition ptn,int position,int difference,int value) {
		int pos = position;
		for (int n=1; n<sequenceLength; n++) {
			pos += difference;
			if (value != ptn.getDigit(pos)) {
				return -1;
			}
		}
		return pos;
	}

	public int getResult() {
		return _result;
	}
	public void setResult(int value) {
		_result = value;
	}

	protected String getFormattedDuration() {
		return formatDuration(_totalDuration/1000);
	}

	static public String formatDuration(long durationInSeconds) {
		long seconds = durationInSeconds % 60;
		long totalMinutes = durationInSeconds / 60;
		long minutes = totalMinutes % 60;
		long hours = totalMinutes / 60;
		String result = "";
		if (hours>0) {
			result += hours+"h ";
		}
		if (hours>0 || minutes>0) {
			result += minutes+"m ";
		}
		result += seconds+"s";
		return result;
	}
}
