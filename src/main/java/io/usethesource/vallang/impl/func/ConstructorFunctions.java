package io.usethesource.vallang.impl.func;

import java.util.Iterator;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;

public class ConstructorFunctions {

    public static boolean isEqual(IConstructor current, IValue value){
        if(value == current) return true;
        if(value == null) return false;

        if (value instanceof IConstructor){
            IConstructor otherTree = (IConstructor) value;

            if(!current.getConstructorType().comparable(otherTree.getConstructorType())) {
              return false;
            }

            final Iterator<IValue> it1 = current.iterator();
            final Iterator<IValue> it2 = otherTree.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                // call to IValue.isEqual(IValue)
                if (it1.next().isEqual(it2.next()) == false) {
                    return false;
                }
            }

            // if this has keyword parameters, then isEqual is overriden by the wrapper
            // but if the other has keyword parameters, then we should fail here:
            return otherTree.mayHaveKeywordParameters() ? !otherTree.asWithKeywordParameters().hasParameters() : true;
        }

        return false;
    }
    
    public static boolean match(IConstructor current, IValue value){
        if(value == current) return true;
        if(value == null) return false;

        if (value instanceof IConstructor){
            IConstructor otherTree = (IConstructor) value;

            // TODO: if types are canonical, this can be ==
            if(!current.getConstructorType().comparable(otherTree.getConstructorType())) {
              return false;
            }

            final Iterator<IValue> it1 = current.iterator();
            final Iterator<IValue> it2 = otherTree.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                // call to IValue.isEqual(IValue)
                if (it1.next().match(it2.next()) == false) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
