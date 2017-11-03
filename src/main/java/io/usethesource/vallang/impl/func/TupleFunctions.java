package io.usethesource.vallang.impl.func;

import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;

public class TupleFunctions {

    public static boolean isEqual(ITuple t, IValue o) {
        if (t == o) {
            return true;
        } else if (o == null) {
            return false;
        } else if (t.getClass() == o.getClass()) {
            ITuple peer = (ITuple) o;

            // TODO: if types become canonical, this can be !=
            if (!t.getType().comparable(peer.getType())) {
                return false;
            }

            int arity = t.arity();
            if (arity != peer.arity()) {
                return false;
            }
            for (int i = 0; i < arity; i++) {
                if (!t.get(i).isEqual(peer.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean match(ITuple t, IValue o) {
        if (t == o) {
            return true;
        } else if (o == null) {
            return false;
        } else if (t.getClass() == o.getClass()) {
            ITuple peer = (ITuple) o;

            // TODO: if types become canonical, this can be ==
            if (!t.getType().comparable(peer.getType())) {
                return false;
            }

            int arity = t.arity();
            if (arity != peer.arity()) {
                return false;
            }
            for (int i = 0; i < arity; i++) {
                if (!t.get(i).match(peer.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

