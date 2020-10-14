package org.dbaussie.vdw;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.dbaussie.vdw.logging.Log;
import org.dbaussie.vdw.model.Partition;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class ThreadedVdwGenerator extends VdwGenerator {

	// constants
	final static public String ALGORITHM = "normalized-prefix-mt";

	static public void main(String[] argv) {
		Log.enabled = true;
		ThreadedVdwGenerator generator = new ThreadedVdwGenerator(2,5,-1);
		generator.generate();
		//System.exit(0);
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
		ListeningExecutorService les = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
		LinkedList<Callable<ThreadData>> callList = new LinkedList<Callable<ThreadData>>();
		int index = 0;
		for (Partition ptn : _initialCertificates) {
			final int tn = index;
			final Partition p = ptn;
			Callable<ThreadData> c = () -> {
				return calculatePartiton(p,tn);
			};
			index++;
			callList.add(c);
		}
		try {
			@SuppressWarnings({"unchecked","rawtypes"})
			List<ListenableFuture<ThreadData>> resultList = (List)les.invokeAll(callList);
			for (ListenableFuture<ThreadData> lf : resultList) {
				Futures.addCallback(lf,new FutureCallback<ThreadData>() {
					public void onSuccess(ThreadData td) {
						System.out.println("Thread ["+td.threadNumber+"] result="+td.result+" certificates="+td.certCount+" ptn="+td.ptn);
					}
					public void onFailure(Throwable thrown) {
					}
				},les);
			}
			int result = -1;
			for (ListenableFuture<ThreadData> f : resultList) {
				if (f.get().result>result) {
					result = f.get().result;
				}
			}
			setResult(result+1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		les.shutdown();
	}

	protected ThreadData calculatePartiton(Partition ptn,int threadNumber) {
		ThreadData td = new ThreadData();
		td.ptn = ptn;
		td.threadNumber = threadNumber;
		int maxDigitCount = 0;
		int digitCount = initialDigitCount + 1;
		ptn.setDigitCount(digitCount);
		td.result = -1;
		while (td.result==-1) {
			if (abortDigitCount>0 && digitCount>=abortDigitCount) {
				return td;
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
					td.certCount++;
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
					td.ptn = ptn.clone();
				}
				digitCount++;
				ptn.setDigitCount(digitCount);
			} else {
				boolean done = false;
				while (!done) {
					digitCount--;
					if (digitCount==initialDigitCount) {
						td.result = maxDigitCount;
						return td;
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
		return td;
	}

	static private class ThreadData {
		public int threadNumber;
		public int result;
		public Partition ptn;
		public long certCount;
	}
}
