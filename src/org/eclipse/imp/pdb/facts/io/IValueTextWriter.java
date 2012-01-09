/*******************************************************************************
* Copyright (c) CWI 2008 
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (jurgenv@cwi.nl) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.Writer;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * An instance of IValueTextWriter can serialize all types of IValues as characters
 * There should be a corresponding IValueReader to de-serialize them again
 * back to IValues.
 */
public interface IValueTextWriter {
	/**
	 * Serialize a value using the given writer
	 * @param value  the value to serialize
	 * @param writer the writer to output character to
	 * @throws IOException in case the writer does
	 */
	 void write(IValue value, Writer writer) throws IOException;
	 
	 /**
	  * Serialize a value using the given writer
	  * @param value  the value to serialize
	  * @param writer the writer to output character to
	  * @throws IOException in case the writer does
	  */
	 void write(IValue value, Writer writer, TypeStore typeStore) throws IOException;
}
