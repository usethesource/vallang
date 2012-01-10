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
package org.eclipse.imp.pdb.facts.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.io.binary.BinaryWriter;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * Writer for PDB Binary Files (PBF).
 * 
 * @author Arnold Lankamp
 */
public class BinaryValueWriter implements IValueBinaryWriter{
	
	/**
	 * Constructor.
	 */
	public BinaryValueWriter(){
		super();
	}
	
	/**
	 * Writes the given value to the given stream.
	 * 
	 * @param value
	 *            The value to write.
	 * @param outputStream
	 *            The output stream to write to.
	 * @throws IOException
	 *            Thrown when something goes wrong.
	 * @see IValueTextWriter#write(IValue, OutputStream)
	 */
	public void write(IValue value, OutputStream outputStream) throws IOException{
		BinaryWriter binaryWriter = new BinaryWriter(value, outputStream, new TypeStore());
		binaryWriter.serialize();
		outputStream.flush();
	}
	
	/**
	 * Writes the given value to the given stream.
	 * 
	 * @param value
	 *            The value to write.
	 * @param outputStream
	 *            The output stream to write to.
	 * @param typeStore
	 *            The type store to use.
	 * @throws IOException
	 *            Thrown when something goes wrong.
	 * @see IValueTextWriter#write(IValue, OutputStream)
	 */
	public void write(IValue value, OutputStream outputStream, TypeStore typeStore) throws IOException{
		BinaryWriter binaryWriter = new BinaryWriter(value, outputStream, typeStore);
		binaryWriter.serialize();
		outputStream.flush();
	}
	
	/**
	 * Writes the given value to a file.
	 * 
	 * @param value
	 *            The value to write.
	 * @param file
	 *            The file to write to.
	 * @param typeStore
	 *            The type store to use.
	 * @throws IOException
	 *            Thrown when something goes wrong.
	 */
	public static void writeValueToFile(IValue value, File file, TypeStore typeStore) throws IOException{
		OutputStream fos = null;
		try{
			fos = new BufferedOutputStream(new FileOutputStream(file));
			
			BinaryWriter binaryWriter = new BinaryWriter(value, fos, typeStore);
			binaryWriter.serialize();
			fos.flush();
		}finally{
			if(fos != null){
				fos.close();
			}
		}
	}
}
