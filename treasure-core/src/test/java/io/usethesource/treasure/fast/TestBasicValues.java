package io.usethesource.treasure.fast;

import io.usethesource.treasure.BaseTestBasicValues;
import io.usethesource.treasure.impl.fast.ValueFactory;

public class TestBasicValues extends BaseTestBasicValues {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp(ValueFactory.getInstance());
	}
}
