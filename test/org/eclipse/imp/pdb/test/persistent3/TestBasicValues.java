package org.eclipse.imp.pdb.test.persistent3;

import org.eclipse.imp.pdb.facts.impl.persistent.ValueFactory3;
import org.eclipse.imp.pdb.test.BaseTestBasicValues;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(ValueFactory3.getInstance());
	}
}
