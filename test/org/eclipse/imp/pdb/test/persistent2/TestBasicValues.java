package org.eclipse.imp.pdb.test.persistent2;

import org.eclipse.imp.pdb.facts.impl.persistent.ValueFactory2;
import org.eclipse.imp.pdb.test.BaseTestBasicValues;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(ValueFactory2.getInstance());
	}
}
