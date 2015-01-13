package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetRelation;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.func.SetFunctions;

public class DefaultRelationViewOnSet implements ISetRelation<ISet> {

	protected final IValueFactory vf;
	protected final ISet rel1;
	
	public DefaultRelationViewOnSet(final IValueFactory vf, final ISet rel1) {
		this.vf = vf;
		this.rel1 = rel1;
	}
	
	@Override
	public ISet compose(ISetRelation<ISet> rel2) {
        return SetFunctions.compose(vf, rel1, rel2.asSet());
	}

	@Override
	public ISet closure() {
		return SetFunctions.closure(vf, rel1);
	}

	@Override
	public ISet closureStar() {
		return SetFunctions.closureStar(vf, rel1);
	}
	
	@Override
	public int arity() {
		return rel1.getElementType().getArity();
	}	
	
	@Override
	public ISet project(int... fieldIndexes) {
		return SetFunctions.project(vf, rel1, fieldIndexes);
	}

	@Override
	public ISet projectByFieldNames(String... fieldsNames) {
		return SetFunctions.projectByFieldNames(vf, rel1, fieldsNames);
	}

	@Override
	public ISet carrier() {
		return SetFunctions.carrier(vf, rel1);
	}

	@Override
	public ISet domain() {
		return SetFunctions.domain(vf, rel1);
	}

	@Override
	public ISet range() {
		return SetFunctions.range(vf, rel1);
	}

	@Override
	public ISet asSet() {
		return rel1;
	}
	
	@Override
	public String toString() {
		return rel1.toString();
	}	

}
