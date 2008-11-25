package org.eclipse.imp.pdb.facts;

public interface IBool extends IValue {
	boolean getValue();
	IBool and(IBool other);
	IBool or(IBool other);
	IBool xor(IBool other);
	IBool not();
	IBool implies(IBool other);
	IBool equivalent(IBool other);
}
