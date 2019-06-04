package io.usethesource.vallang.impl;

import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;

public class DefaultRelationViewOnSet implements IRelation<ISet> {
	protected final ISet set;
	
	public DefaultRelationViewOnSet(ISet rel1) {
		this.set = rel1;
	}
	
	@Override
	public String toString() {
		return set.toString();
	}	

    @Override
    public ISet asContainer() {
        return set;
    }
}
