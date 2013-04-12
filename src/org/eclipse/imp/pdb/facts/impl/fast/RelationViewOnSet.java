package org.eclipse.imp.pdb.facts.impl.fast;

import org.eclipse.imp.pdb.facts.IRelationalAlgebra;
import org.eclipse.imp.pdb.facts.ISet;

public class RelationViewOnSet implements IRelationalAlgebra<ISet> {

	protected final ISet rel1;
	
	public RelationViewOnSet(ISet rel1) {
		this.rel1 = rel1;
	}
	
	@Override
	public ISet compose(ISet rel2) {
		return RelationalOperations.compose(rel1, rel2);
	}

	@Override
	public ISet closure() {
		return RelationalOperations.closure(rel1);
	}

	@Override
	public ISet closureStar() {
		return RelationalOperations.closureStar(rel1);
	}
	
	@Override
	public int arity() {
		return rel1.getElementType().getArity();
	}	
	
	@Override
	public ISet project(int... fieldIndexes) {
		return RelationalOperations.project(rel1, fieldIndexes);
	}

	@Override
	public ISet projectByFieldNames(String... fieldsNames) {
		return RelationalOperations.projectByFieldNames(rel1, fieldsNames);
	}

	@Override
	public ISet carrier() {
		return RelationalOperations.carrier(rel1);
	}

	@Override
	public ISet domain() {
		return RelationalOperations.domain(rel1);
	}

	@Override
	public ISet range() {
		return RelationalOperations.range(rel1);
	}

}
