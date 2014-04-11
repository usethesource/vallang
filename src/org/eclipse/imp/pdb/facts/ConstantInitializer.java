package org.eclipse.imp.pdb.facts;

public class ConstantInitializer implements IValueInitializer {
	private final IValue constant;
	
	public ConstantInitializer(IValue constant) {
		this.constant = constant;
	}
	
	@Override
	public IValue initialize() {
		return constant;
	}

}
