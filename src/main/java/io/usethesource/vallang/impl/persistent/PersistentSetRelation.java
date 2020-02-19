package io.usethesource.vallang.impl.persistent;

import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;

/**
 * The current implementation of relations for persistent sets is mostly
 * done by {@link PersistentHashIndexedBinaryRelation} for binary reflexive
 * relations. The other relations are covered by the default impementations
 * of {@link IRelation}. There is room for improvement there in the performance
 * department.
 */
public class PersistentSetRelation implements IRelation<ISet> {
    private final ISet set;

    public PersistentSetRelation(ISet set) {
        this.set = set;
    }
    
    @Override
    public ISet asContainer() {
        return set;
    }
}
