package io.usethesource.vallang.io;

import org.checkerframework.checker.nullness.qual.NonNull;

import io.usethesource.vallang.io.binary.util.OpenAddressingLastWritten;
import io.usethesource.vallang.io.binary.util.TrackLastWritten;

public class OpenAddressingLastWrittenBrokenHashTest extends TrackWritesTestBase {
    @Override
    public TrackLastWritten<Object> getWritesWindow(int size) {
        return new OpenAddressingLastWritten<@NonNull Object>(size) {
            @Override
            protected boolean equals(@NonNull Object a, @NonNull Object b) {
                return a == b;
            }

            @Override
            protected int hash(@NonNull Object obj) {
                return System.identityHashCode(obj) % Math.max(2, size / 2);
            }
        };
    }
}
