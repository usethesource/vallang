package io.usethesource.treasure.persistent;

import io.usethesource.treasure.BaseTestBasicValues;
import io.usethesource.treasure.impl.persistent.ValueFactory;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(ValueFactory.getInstance());
	}
}
