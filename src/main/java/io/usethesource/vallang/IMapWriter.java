/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.vallang;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;


public interface IMapWriter extends IWriter<IMap> {
    /**
     * Put a value with a certain key into the map
     * @param key
     * @param value
     * @throws FactTypeUseException
     */
    void put(IValue key, IValue value);
    
    /**
     * Merge an entire map into the writer. Existing keys
     * will be overwritten by the new map
     * @param map
     * @throws FactTypeUseException
     */
    void putAll(IMap map);
    
    /**
     * Merge an entire java.util.Map into the writer. Existing
     * keys will be overwritten by the new map.
     * @param map
     * @throws FactTypeUseException
     */
    void putAll(Map<IValue, IValue> map);
    
    /**
     * Lookup a given key into the state of the current map-to-be
     * @param key
     * @return null if no value exists with this key, otherwise the respective value.
     */
    IValue get(IValue key);
    
    /**
     * @return an entry iterator over the current map key/value pairs
     */
    public Iterator<Entry<IValue, IValue>> entryIterator();
    
    @Override
    default Type computeType() {
        Type keyLub = TypeFactory.getInstance().voidType();
        Type valueLub = TypeFactory.getInstance().voidType();
        
        Iterator<Entry<IValue, IValue>> it = entryIterator();
        while (it.hasNext()) {
            Entry<IValue, IValue> entry = it.next();
            keyLub = keyLub.lub(entry.getKey().getType());
            valueLub = valueLub.lub(entry.getValue().getType());
        }
        
        return IValue.TF.mapType(keyLub, valueLub);
    }
}
