package io.usethesource.vallang.impl;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IRelation;

public class DefaultRelationViewOnList implements IRelation<IList> {

	protected final IList rel1;
	
	public DefaultRelationViewOnList(IList rel1) {
		this.rel1 = rel1;
	}

	@Override
	public String toString() {
		return rel1.toString();
	}

    @Override
    public IList asContainer() {
        return rel1;
    }
}
