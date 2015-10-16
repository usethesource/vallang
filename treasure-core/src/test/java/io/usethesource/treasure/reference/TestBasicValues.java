package io.usethesource.treasure.reference;

import io.usethesource.treasure.BaseTestBasicValues;
import io.usethesource.treasure.impl.reference.ValueFactory;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(ValueFactory.getInstance());
	}
}
