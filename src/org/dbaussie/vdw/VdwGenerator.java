package org.dbaussie.vdw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;

import org.dbaussie.vdw.logging.Log;
import org.dbaussie.vdw.model.Partition;
import org.dbaussie.vdw.util.ArrayUtil;

public class VdwGenerator {

	// attributes
	private long _startTime;
	private long _totalDuration; // total duration in ms
	private int _result;
	private ArrayList<Partition> _initialCertificates;
	private int _certIndex;

	// properties
	public int colorCount;
	public int sequenceLength;
	public int abortDigitCount = -1; // if this is not -1 the stop the search here
	public boolean useNormalization = true;
	public int initialDigitCount;

	static public void main(String[] argv) {
		Log.enabled = true;
		VdwGenerator generator = new VdwGenerator(4,3,-1);
		generator.generate();
		System.out.println("\nCalculated "+generator+" = "+generator.getResult()+" in "+generator.getFormattedDuration());
	}

	public VdwGenerator(int colorCount,int sequenceLength,int initialCount) {
		this.colorCount = colorCount;
		this.sequenceLength = sequenceLength;
		this.initialDigitCount = initialCount<=0 ? sequenceLength+1 : initialCount;
	}

	public VdwGenerator(int colorCount,int sequenceLength) {
		this(colorCount,sequenceLength,sequenceLength+1);
	}

	@Override
	public String toString() {
		return "W("+colorCount+","+sequenceLength+")";
	}


	public void generate() {
		System.out.println("Computing "+toString());
		_startTime = new Date().getTime();
		boolean foundAnyCert = initialize();
		if (!foundAnyCert) {
			_result = 0;
			return;
		}
		int maxDigitCount = 0;
		Partition ptn = _initialCertificates.get(0);
		_certIndex++;
		int digitCount = initialDigitCount + 1;
		ptn.setDigitCount(digitCount);

		while (_result==-1) {
			if (abortDigitCount>0 && digitCount>=abortDigitCount) {
				_result = 0;
				break;
			}
			int maxd = (digitCount-1) / (sequenceLength-1);
			int lastDigit = ptn.getLastDigit();
			foundAnyCert = false;
			for (int testDigit=lastDigit; testDigit<colorCount; testDigit++) {
				ptn.setLastDigit(testDigit);
				int lastAPPosition = checkForAPFreenessFinalDigit(ptn,maxd,digitCount);
				boolean foundCert = lastAPPosition == -1;
				//if (enableLogging) {
					//Log.log("        checking partition "+digitCount+","+ptn+",cert="+foundCert);
				//}
				if (foundCert) {
					foundAnyCert = true;
					//Log.log("            found new cert "+ptn);
					break;
				}
			}
			if (foundAnyCert) {
				if (digitCount>maxDigitCount) {
					maxDigitCount = digitCount;
					long now = new Date().getTime();
					long dur = now - _startTime;
					System.out.println("    "+toString()+" > "+digitCount+" in "+formatDuration(dur/1000));
				}
				digitCount++;
				ptn.setDigitCount(digitCount);
			} else {
				boolean done = false;
				while (!done) {
					digitCount--;
					if (digitCount==initialDigitCount) {
						if (_certIndex>=_initialCertificates.size()) {
							_result = maxDigitCount+1;
							break;
						}
						ptn = _initialCertificates.get(_certIndex);
						_certIndex++;
						//Log.log("    switching to new initial cert "+ptn);
						digitCount++;
						ptn.setDigitCount(digitCount);
						done = true;
						break;
					}
					ptn.setDigitCount(digitCount);
					int value = ptn.getLastDigit();
					if (value<colorCount) {
						//Log.log("    backtracked to "+ptn);
						ptn.setLastDigit(value+1);
						done = true;
					}
				}
			}
		}
		long endTime = new Date().getTime();
		_totalDuration = endTime - _startTime;
	}

	private boolean initialize() {
		_result = -1;
		ArrayList<Partition> certs = new ArrayList<Partition>();
		int digitCount = initialDigitCount;
		Partition ptn = new Partition(colorCount,digitCount);
		final int maxd = 1;
		boolean foundAnyCert = generateAllCertificates(ptn,digitCount,maxd,certs);
		Log.log("    Found "+certs.size()+" initial certificates: "+certs+" with digit count "+initialDigitCount);
		if (useNormalization) {
			_initialCertificates = new ArrayList<Partition>();
			for (Partition ptnCert : certs) {
				boolean includeCert = filterNormalization(ptnCert,digitCount);
				if (includeCert) {
					_initialCertificates.add(ptnCert);
				}
			}
			Log.log("    Normalized list to "+_initialCertificates.size()+" certificates "+_initialCertificates);
		} else {
			_initialCertificates = certs;
		}
		return foundAnyCert;
	}

	/**
	 * Decision function as to whether the partition value can be excluded from the search
	 * Returns true if the partition must be included false otherwise
	 * @param ptn
	 * @param digitCount
	 * @return
	 */
	private boolean filterNormalization(Partition ptn,int digitCount) {
		int[] newData = new int[digitCount];
		int[] partitionData = ptn.getUnderlyingArray();
		ArrayUtil.shiftLeft(partitionData,newData,digitCount);
		if (newData[0]==0) {
			if (colorCount==2) {
				ArrayUtil.complementBinaryArray(newData,digitCount);
			} else {
				return true;
			}
		}
		int cmp = ArrayUtil.compare(partitionData,newData,digitCount);
		//System.out.println("cert "+digitCount+" "+ptn+", "+ArrayUtil.toString(newData,digitCount)+", "+cmp);
		return cmp <= 0;
	}
	
	private boolean generateAllCertificates(Partition ptn,int digitCount,int maxd,ArrayList<Partition> certList) {
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

	private int checkForAPFreenessFinalDigit(Partition ptn,int maxd,int digitCount) {
		int lastAPPosition = -1;
		boolean done = false;
		for (int d=1; d<=maxd && !done; d++) {
			int value = ptn.getLastDigit();
			lastAPPosition = checkForAPRightToLeft(ptn,digitCount-1,d,value);
			done = lastAPPosition != -1;
		}
		return lastAPPosition;
	}

	private int checkForAPRightToLeft(Partition ptn,int position,int difference,int value) {
		int pos = position;
		for (int n=1; n<sequenceLength; n++) {
			pos -= difference;
			if (value != ptn.getDigit(pos)) {
				return -1;
			}
		}
		return position;
	}

	private int checkForAPFreenessAnyDigit(Partition ptn,int maxd,int digitCount) {
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

	private int checkForAPLeftToRight(Partition ptn,int position,int difference,int value) {
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

	public String getFormattedDuration() {
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
