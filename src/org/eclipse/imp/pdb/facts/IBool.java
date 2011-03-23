/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Jurgen Vinju (jurgen@vinju.org) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts;

public interface IBool extends IValue {
	boolean getValue();
	String getStringRepresentation();
	IBool and(IBool other);
	IBool or(IBool other);
	IBool xor(IBool other);
	IBool not();
	IBool implies(IBool other);
	IBool equivalent(IBool other);
}
