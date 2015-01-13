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
import java.io.Reader;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * An instance of IValueTextReader can parse a serialized character-based representation of IValues.
 * There should be a corresponding IValueTextWriter to serialize them again. Note
 * that IValueTextReaders should also <emph>validate</emph> the serialized input against a {@link Type}.
 */

public interface IValueTextReader {
	/**
	 * Parse an IValue, validate it and build it if it can be validated.
	 * 
	 * @param factory used when building the value
	 * @param store  declarations of types to use
	 * @param type   used to validate the value
	 * @param stream  source of bytes to parse
	 * @return an IValue that represents the string input
	 */
	IValue read(IValueFactory factory, TypeStore store, Type type, Reader reader) throws FactTypeUseException, IOException;

	/**
	 * Parse an IValue, validate it and build it if it can be validated.
	 * 
	 * @param factory used when building the value
	 * @param type used to validate the value
	 * @param reader source of character to parse
	 * @return an IValue that represents the string input
	 */
	IValue read(IValueFactory factory, Type type, Reader reader) throws FactTypeUseException, IOException;
	
	/**
	 * Parse an IValue without validation.
	 * 
	 * @param factory used when building the value
	 * @param reader source of characters to parse
	 * @return an IValue that represents the string input
	 */
	IValue read(IValueFactory factory,  Reader reader) throws FactTypeUseException, IOException;
}
