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
import java.io.InputStream;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * An instance of IValueReader can parse a serialized representation
 * of IValues. There should be a corresponding IValueWriter to serialize
 * them again. Note that IValueReaders should also <emph>validate</emph> the 
 * serialized input against a @ref Type.
 * @author jurgenv
 *
 */

public interface IValueReader {  
  /**
	 * Parse an IValue, validate it and build it if it can be validated.
	 * 
	 * @param factory used when building the value
	 * @param type    used to validate the value
	 * @param stream  source of bytes to parse
	 * @return an IValue that represents the string input
	 */
  IValue read(IValueFactory factory, Type type, InputStream stream) throws FactTypeError, IOException;
}
