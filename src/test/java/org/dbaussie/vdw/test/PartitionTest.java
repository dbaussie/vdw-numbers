package org.dbaussie.vdw.test;

import static org.junit.Assert.*;

import org.dbaussie.vdw.model.Partition;
import org.junit.Test;

public class PartitionTest {

	@Test
	public void testValues() {
		Partition ptn = new Partition();
		String testValue = "11101011";
		ptn.setValue(testValue);
		assertEquals("Value set correctly",testValue,ptn.toString());
		assertEquals("Value length correct",testValue.length(),ptn.digitCount);
	}

	@Test
	public void testIncrement() {
		Partition ptn = new Partition();
		String testValue = "11101011";
		ptn.setValue(testValue);
		boolean proceed = ptn.increment();
		assertEquals("proceed is true",proceed,true);
		assertEquals("increment succeeded","11101100",ptn.toString());
	}

}
