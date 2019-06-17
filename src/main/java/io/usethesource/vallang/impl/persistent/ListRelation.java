package io.usethesource.vallang.impl.persistent;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.impl.util.collections.ShareableValuesHashSet;

public class ListRelation implements IRelation<IList> {
    private final IList list;

    public ListRelation(IList list) {
        this.list = list;
    }
    
    @Override
    public IList asContainer() {
        return list;
    }
    
    @Override
    public IList closure() {
        // will throw exception if not binary and reflexive
        list.getType().closure();

        IRelation<IList> tmp = this;

        int prevCount = 0;

        ShareableValuesHashSet addedTuples = new ShareableValuesHashSet();
        while (prevCount != tmp.asContainer().length()) {
            prevCount = tmp.asContainer().length();
            IList tcomp = tmp.compose(tmp);
            IWriter<IList> w = writer();
            for (IValue t1 : tcomp) {
                if (!tmp.asContainer().contains(t1) && !addedTuples.contains(t1)) {
                    addedTuples.add(t1);
                    w.append(t1);
                }
            }
            
            tmp = tmp.asContainer().concat(w.done()).asRelation();
            addedTuples.clear();
        }
        
        return tmp.asContainer();
    }

    @Override
    public IList closureStar() {
        list.getType().closure();
        // an exception will have been thrown if the type is not acceptable

        IWriter<IList> reflex = writer();

        for (IValue e : carrier()) {
            reflex.insertTuple(e, e);
        }

        return closure().concat(reflex.done());
    }

}
