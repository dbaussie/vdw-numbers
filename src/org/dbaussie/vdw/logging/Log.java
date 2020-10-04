package org.dbaussie.vdw.logging;

public class Log {

	// static data
	static public boolean enabled;

	static public void log(String line) {
		if (enabled && line != null) {
			System.out.println(line);
		}
	}

	static public void logAppend(String msg) {
		if (enabled && msg != null) {
			System.out.print(msg);
		}
	}
}
