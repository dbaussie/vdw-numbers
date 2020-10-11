package org.dbaussie.vdw;

import java.util.ArrayList;
import java.util.Date;

import org.dbaussie.vdw.logging.Log;
import org.dbaussie.vdw.model.Partition;
import org.dbaussie.vdw.util.ArrayUtil;

public class VdwGenerator extends AbstractVdwGenerator {

	// constants
	final static public String ALGORITHM = "normalized-prefix";

	// attributes
	protected ArrayList<Partition> _initialCertificates;
	protected int _certIndex;

	// properties
	public boolean useNormalization = true;

	static public void main(String[] argv) {
		Log.enabled = true;
		VdwGenerator generator = new VdwGenerator(2,5,-1);
		generator.generate();
	}

	public VdwGenerator(int colorCount,int sequenceLength,int initialCount) {
		super(colorCount,sequenceLength,initialCount);
		this.algorithm = ALGORITHM;
	}

	public VdwGenerator(int colorCount,int sequenceLength) {
		this(colorCount,sequenceLength,sequenceLength+1);
	}

	@Override 
	public void calculate() {
		boolean foundAnyCert = initialize();
		if (!foundAnyCert) {
			setResult(0);
			return;
		}
		int maxDigitCount = 0;
		Partition ptn = _initialCertificates.get(0);
		_certIndex++;
		int digitCount = initialDigitCount + 1;
		ptn.setDigitCount(digitCount);

		while (_result==-1) {
			if (abortDigitCount>0 && digitCount>=abortDigitCount) {
				setResult(0);
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
	}

	protected boolean initialize() {
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
	protected boolean filterNormalization(Partition ptn,int digitCount) {
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
}
