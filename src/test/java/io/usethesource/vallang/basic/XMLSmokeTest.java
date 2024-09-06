/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju

 *******************************************************************************/

package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.UnsupportedTypeException;
import io.usethesource.vallang.io.XMLReader;
import io.usethesource.vallang.io.XMLWriter;
import io.usethesource.vallang.type.TypeStore;

public class XMLSmokeTest extends BooleanStoreProvider {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    @Disabled("XML writer fails on empty strings and empty lists")
    public void testXMLWriter(IValueFactory vf, TypeStore ts, @ExpectedType("Boolean") IConstructor val) throws IOException {
        StringWriter buffer = new StringWriter();
        XMLWriter testWriter = new XMLWriter();
        XMLReader testReader = new XMLReader();

        try {
            testWriter.write(val, buffer);
            IValue result = testReader.read(vf, ts, val.getType(), new StringReader(buffer.toString()));
            assertEquals(val, result);
        }
        catch (UnsupportedTypeException e) {
            // this happens because the XML support does not offer serialization of all types
        }
    }

}
