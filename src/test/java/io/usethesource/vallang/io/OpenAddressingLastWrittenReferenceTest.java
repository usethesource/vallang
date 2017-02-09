package io.usethesource.vallang.io;

import io.usethesource.vallang.io.binary.util.OpenAddressingLastWritten;
import io.usethesource.vallang.io.binary.util.TrackLastWritten;

public class OpenAddressingLastWrittenReferenceTest extends TrackWritesTestBase {
    @Override
    public TrackLastWritten<Object> getWritesWindow(int size) {
        return OpenAddressingLastWritten.referenceEquality(size);
    }
}
