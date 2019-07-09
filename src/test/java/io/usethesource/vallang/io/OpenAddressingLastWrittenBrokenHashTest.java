package io.usethesource.vallang.io;

import java.util.Objects;

import io.usethesource.vallang.io.binary.util.OpenAddressingLastWritten;
import io.usethesource.vallang.io.binary.util.TrackLastWritten;

public class OpenAddressingLastWrittenBrokenHashTest extends TrackWritesTestBase {
    @Override
    public TrackLastWritten<Object> getWritesWindow(int size) {
        return new OpenAddressingLastWritten<Object>(size) {
            @Override
            protected boolean equals(Object a, Object b) {
                return a == b;
            }

            @Override
            protected int hash(Object obj) {
                Objects.requireNonNull(obj);
                return System.identityHashCode(obj) % Math.max(2, size / 2);
            }
        };
    }
}
