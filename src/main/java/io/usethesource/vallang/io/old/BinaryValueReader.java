/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package io.usethesource.vallang.io.old;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.IValueBinaryReader;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;

/**
 * A reader for PDB Binary Files (PBF).
 * 
 * @author Arnold Lankamp
 * @deprecated use the new {@link IValueReader}
 */
@Deprecated
public class BinaryValueReader implements IValueBinaryReader {
	
	/**
	 * Constructor.
	 */
	public BinaryValueReader(){
		super();
	}
	
	/**
	 * @see IValueBinaryReader#read(IValueFactory, InputStream)
	 */
	public IValue read(IValueFactory valueFactory, InputStream inputStream) throws IOException{
		return doRead(valueFactory, new TypeStore(), inputStream);
	}
	
	/**
	 * @see IValueBinaryReader#read(IValueFactory, Type, InputStream)
	 */
	public IValue read(IValueFactory valueFactory, Type type, InputStream inputStream) throws IOException{
		return doRead(valueFactory, new TypeStore(), inputStream);
	}
	
	/**
	 * @see IValueBinaryReader#read(IValueFactory, TypeStore, Type, InputStream)
	 */
	public IValue read(IValueFactory valueFactory, TypeStore typeStore, Type type, InputStream inputStream) throws IOException{
		return doRead(valueFactory, typeStore, inputStream);
	}
	
	private IValue doRead(IValueFactory valueFactory, TypeStore typeStore, InputStream inputStream) throws IOException{
		try (BinaryReader binaryReader = new BinaryReader(valueFactory, typeStore, inputStream)) {
		    return binaryReader.deserialize();
		}
	}
	
	/**
	 * Reads the value from the given file.
	 * 
	 * @param valueFactory
	 *            The value factory to use.
	 * @param typeStore
	 *            The typestore to use.
	 * @param file
	 *            The file to read from.
	 * @return The resulting value.
	 * @throws IOException
	 *            Thrown when something goes wrong.
	 */
	public static IValue readValueFromFile(IValueFactory valueFactory, TypeStore typeStore, File file) throws IOException{
		try (InputStream fis = new BufferedInputStream(new FileInputStream(file)); BinaryReader binaryReader = new BinaryReader(valueFactory, typeStore, fis)) {
			return binaryReader.deserialize();
		}
	}
}
