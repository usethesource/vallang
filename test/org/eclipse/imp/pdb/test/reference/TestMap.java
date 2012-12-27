/*******************************************************************************
* Copyright (c) 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge
*******************************************************************************/
package org.eclipse.imp.pdb.test.reference;

import org.eclipse.imp.pdb.facts.impl.reference.ValueFactory;
import org.eclipse.imp.pdb.test.BaseTestMap;

public class TestMap extends BaseTestMap {
	
	@Override
	protected void setUp() throws Exception{
		super.setUp(ValueFactory.getInstance());
	}
}
