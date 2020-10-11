package org.dbaussie.vdw;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.dbaussie.vdw.logging.Log;
import org.dbaussie.vdw.model.Partition;

public class ThreadedVdwGenerator extends VdwGenerator {

	// constants
	final static public String ALGORITHM = "normalized-prefix-mt";

	static public void main(String[] argv) {
		Log.enabled = true;
		ThreadedVdwGenerator generator = new ThreadedVdwGenerator(4,3,-1);
		generator.generate();
	}

	public ThreadedVdwGenerator(int colorCount,int sequenceLength,int initialCount) {
		super(colorCount,sequenceLength,initialCount);
		this.algorithm = ALGORITHM;
	}

	public ThreadedVdwGenerator(int colorCount,int sequenceLength) {
		this(colorCount,sequenceLength,sequenceLength+1);
	}

	@Override
	public void calculate() {
		boolean foundAnyCert = initialize();
		if (!foundAnyCert) {
			setResult(0);
			return;
		}
		ExecutorService es = Executors.newCachedThreadPool();
		LinkedList<Callable<Integer>> callList = new LinkedList<Callable<Integer>>();
		int index = 0;
		for (Partition ptn : _initialCertificates) {
			final int tn = index;
			final Partition p = ptn;
			Callable<Integer> c = () -> {
				return calculatePartiton(p,tn);
			};
			index++;
			callList.add(c);
		}
		List<Future<Integer>> resultList = null;
		try {
			resultList = es.invokeAll(callList);
			int result = -1;
			for (Future<Integer> fi : resultList) {
				if (fi.get()>result) {
					result = fi.get();
				}
			}
			setResult(result+1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected int calculatePartiton(Partition ptn,int threadNumber) {
		int maxDigitCount = 0;
		int digitCount = initialDigitCount + 1;
		ptn.setDigitCount(digitCount);
		int result = -1;
		while (result==-1) {
			if (abortDigitCount>0 && digitCount>=abortDigitCount) {
				return 0;
			}
			int maxd = (digitCount-1) / (sequenceLength-1);
			int lastDigit = ptn.getLastDigit();
			boolean foundAnyCert = false;
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
					System.out.println("    "+toString()+" > "+digitCount+" in "+formatDuration(dur/1000)+" ["+threadNumber+"]");
				}
				digitCount++;
				ptn.setDigitCount(digitCount);
			} else {
				boolean done = false;
				while (!done) {
					digitCount--;
					if (digitCount==initialDigitCount) {
						return maxDigitCount;
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
		return -1;
	}
}
