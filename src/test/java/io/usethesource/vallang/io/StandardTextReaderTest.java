package io.usethesource.vallang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

class StandardTextReaderTest {
    private IString readString(IValueFactory valueFactory, TypeStore typeStore, String s) throws IOException {
        Reader reader = new StringReader(s);
        StandardTextReader textReader = new StandardTextReader();
        return (IString) textReader.read(valueFactory, typeStore, TypeFactory.getInstance().stringType(), reader);
        
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    void escapeNormalCharacters(IValueFactory valueFactory, TypeStore typeStore) throws IOException {
        IString s = readString(valueFactory, typeStore, "\"\\$\"");
        assertEquals("$", s.getValue());
    }
}
