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
import java.io.OutputStream;

import org.eclipse.imp.pdb.facts.IValue;

/**
 * An instance of IValueWriter can serialize all types of IValues.
 * There should be a corresponding IValueReader to de-serialize them
 * back to IValues.
 *  
 * @author jurgenv
 *
 */
public interface IValueWriter {
	 void write(IValue value, OutputStream stream) throws IOException;
}
