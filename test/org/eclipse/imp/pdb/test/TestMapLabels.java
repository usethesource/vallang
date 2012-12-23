/*******************************************************************************
 * Copyright (c) 2012 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Anya Helene Bagge (University of Bergen) - implementation
 *    Arnold Lankamp - base implementation (from TestBinaryIO.java)
 *******************************************************************************/
package org.eclipse.imp.pdb.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.fast.ValueFactory;
import org.eclipse.imp.pdb.facts.io.StandardTextReader;
import org.eclipse.imp.pdb.facts.io.StandardTextWriter;
import org.eclipse.imp.pdb.facts.io.binary.BinaryReader;
import org.eclipse.imp.pdb.facts.io.binary.BinaryWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * @author Anya Helene Bagge
 */
public class TestMapLabels extends TestCase {
	private TypeStore ts = new TypeStore();
	private TypeFactory tf = TypeFactory.getInstance();
	private IValueFactory vf = ValueFactory.getInstance();
	private Type a = tf.abstractDataType(ts, "A");
	private Type b = tf.abstractDataType(ts, "B");
	enum Kind { BINARY, TEXT};
	private TestValue[] testValues = {
			new TestValue("Bergen", "Amsterdam", "from", "to"),
			new TestValue("New York", "London", null, null),
			new TestValue("Banana", "Fruit", "key", "value"),
	};

	public void testNoLabels() {
		// make a non-labeled map type, and the labels should be null
		Type type = tf.mapType(a, b);

		assertNull(type.getKeyLabel());
		assertNull(type.getValueLabel());
	}

	public void testLabels() {
		// make a labeled map type, and the labels should match
		Type type = tf.mapType(a, "apple", b, "banana");

		assertEquals("apple", type.getKeyLabel());
		assertEquals("banana", type.getValueLabel());
	}

	public void testTwoLabels1() {
		// make two map types with same key/value types but different labels,
		// and the labels should be kept distinct
		Type type1 = tf.mapType(a, "apple", b, "banana");
		Type type2 = tf.mapType(a, "orange", b, "mango");
		Type type3 = tf.mapType(a, b);

		assertEquals("apple", type1.getKeyLabel());
		assertEquals("banana", type1.getValueLabel());
		assertEquals("orange", type2.getKeyLabel());
		assertEquals("mango", type2.getValueLabel());
		assertNull(type3.getKeyLabel());
		assertNull(type3.getValueLabel());
	}

	public void testTwoLabels2() {
		Type type1 = tf.mapType(a, "apple", b, "banana");
		Type type2 = tf.mapType(a, "orange", b, "mango");

		assertTrue("Two map types with different labels should be equivalent", type1.equivalent(type2));
		assertTrue("Two map types with different labels should be equivalent", type2.equivalent(type1));
		assertFalse("Two map types with different labels should not be equals", type1.equals(type2));
		assertFalse("Two map types with different labels should not be equals", type2.equals(type1));

		Type type3 = tf.mapType(a, b);
		assertTrue("Labeled and unlabeled maps should be equivalent", type1.equivalent(type3));
		assertTrue("Labeled and unlabeled maps should be equivalent", type3.equivalent(type1));
		assertTrue("Labeled and unlabeled maps should be equivalent", type2.equivalent(type3));
		assertTrue("Labeled and unlabeled maps should be equivalent", type3.equivalent(type2));
		assertFalse("Labeled and unlabeled maps should not be equals", type1.equals(type3));
		assertFalse("Labeled and unlabeled maps should not be equals", type3.equals(type1));
		assertFalse("Labeled and unlabeled maps should not be equals", type2.equals(type3));
		assertFalse("Labeled and unlabeled maps should not be equals", type3.equals(type2));
	}

	public void testLabelsIO(){
		try{
			for(int i = 0; i < testValues.length; i++){
				for(Kind k : Kind.values()) {
					TestValue testValue = testValues[i];

					System.out.println(testValue + " : " + testValue.value.getType()); // Temp

					IValue result = doIO(testValue.value, k);
					System.out.println(result + " : " + result.getType()); // Temp
					System.out.println(); // Temp

					if(!testValue.value.isEqual(result)){
						String message = "Not equal: \n\t"+testValue+" : "+testValue.value.getType()+"\n\t"+result+" : "+result.getType();
						System.err.println(message);
						fail(message);
					}

					Type resultType = result.getType();
					assertEquals("Labels should be preserved by " + k.name() + " IO: ", testValue.keyLabel, resultType.getKeyLabel());
					assertEquals("Labels should be preserved by " + k.name() + " IO: ", testValue.valueLabel, resultType.getValueLabel());
				}
			}
		}catch(IOException ioex){
			ioex.printStackTrace();
			fail(ioex.getMessage());
		}
	}

	private IValue doIO(IValue val, Kind kind) throws IOException {
		switch(kind) {
		case BINARY: {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BinaryWriter binaryWriter = new BinaryWriter(val, baos, ts);
			binaryWriter.serialize();

			byte[] data = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			BinaryReader binaryReader = new BinaryReader(vf, ts, bais);
			printBytes(data); // Temp
			return binaryReader.deserialize();
		}
		/*// Doesn't work
		case XML: {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLWriter writer = new XMLWriter();
			writer.write(val, new OutputStreamWriter(baos), ts);

			byte[] data = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			XMLReader reader = new XMLReader();
			printBytes(data); // Temp
			return reader.read(vf, new InputStreamReader(bais));
		}
		*/
		case TEXT: {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StandardTextWriter writer = new StandardTextWriter();
			writer.write(val, new OutputStreamWriter(baos), ts);

			byte[] data = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			StandardTextReader reader = new StandardTextReader();
			printBytes(data); // Temp
			return reader.read(vf, new InputStreamReader(bais));
		}
		/* // Doesn't work
		case ATERM: {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ATermWriter writer = new ATermWriter();
			writer.write(val, new OutputStreamWriter(baos), ts);

			byte[] data = baos.toByteArray();
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ATermReader reader = new ATermReader();
			printBytes(data); // Temp
			return reader.read(vf, bais);
		}
		*/
		default:
			throw new RuntimeException("Missing case: " + kind.name());
		}
	}

	private final static String[] HEX = new String[]{"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};

	// May be handy when debugging.
	private static void printBytes(byte[] bytes){
		for(int i = 0; i < bytes.length; i++){
			byte b = bytes[i];
			int higher = (b & 0xf0) >> 4;
		int lower = b & 0xf;
		System.out.print("0x");
		System.out.print(HEX[higher]);
		System.out.print(HEX[lower]);
		System.out.print(" ");
		}
		System.out.println();
	}

	class TestValue {
		Type type;
		IValue value;
		String keyLabel;
		String valueLabel;

		TestValue(String key, String value, String keyLabel, String valueLabel) {
			this.keyLabel = keyLabel;
			this.valueLabel = valueLabel;
			if(keyLabel != null && valueLabel != null)
				type = tf.mapType(tf.stringType(), keyLabel, tf.stringType(), valueLabel);
			else
				type = tf.mapType(tf.stringType(), tf.stringType());
			this.value = ((IMap)type.make(vf)).put(vf.string(key), vf.string(value));
		}
		
		public String toString() {
			return value.toString();
		}
	}
}
