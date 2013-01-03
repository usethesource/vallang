package org.eclipse.imp.pdb.test.shared;

import org.eclipse.imp.pdb.facts.impl.shared.SharedValueFactory;
import org.eclipse.imp.pdb.test.BaseTestBasicValues;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(SharedValueFactory.getInstance());
	}
}
