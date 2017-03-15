/** 
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.io.binary.util;

import java.util.concurrent.TimeUnit;

/**
 * Since we are constructing and deconstructing a lot of windows, use this factory to build them.
 * For caching reasons, also return the windows to this factory, so they can be reused again.
 *
 */
public class WindowCacheFactory {
	static private class InstanceHolder {
		static final WindowCacheFactory sInstance = new WindowCacheFactory();
	}
	
	public static WindowCacheFactory getInstance() {
		return InstanceHolder.sInstance;
	}

    private final CacheFactory<TrackLastRead<Object>> lastReads = new CacheFactory<>(60, TimeUnit.SECONDS, WindowCacheFactory::clear);
    private final CacheFactory<TrackLastWritten<Object>> lastWrittenReference = new CacheFactory<>(60, TimeUnit.SECONDS, WindowCacheFactory::clear);
    private final CacheFactory<TrackLastWritten<Object>> lastWrittenObject = new CacheFactory<>(60, TimeUnit.SECONDS, WindowCacheFactory::clear);
    
    private final TrackLastRead<Object> disabledReadWindow = new TrackLastRead<Object>() {
        @Override
        public Object lookBack(int howLongBack) { throw new IllegalArgumentException(); }
        @Override
        public void read(Object obj) { }
    };
    private TrackLastWritten<Object> disabledWriteWindow = new TrackLastWritten<Object>() {
        @Override
        public void write(Object obj) { }
        @Override
        public int howLongAgo(Object obj) { return -1; }
    };
    
    @SuppressWarnings("unchecked")
    public <T> TrackLastRead<T> getTrackLastRead(int size) {
        if (size == 0) {
            return (TrackLastRead<T>) disabledReadWindow;
        }
        return (TrackLastRead<T>) lastReads.get(size, LinearCircularLookupWindow::new);
    }

    @SuppressWarnings("unchecked")
    public <T> TrackLastWritten<T> getTrackLastWrittenReferenceEquality(int size) {
        if (size == 0) {
            return (TrackLastWritten<T>) disabledWriteWindow;
        }
        return (TrackLastWritten<T>) lastWrittenReference.get(size, OpenAddressingLastWritten::referenceEquality);
    }
    @SuppressWarnings("unchecked")
    public <T> TrackLastWritten<T> getTrackLastWrittenObjectEquality(int size) {
        if (size == 0) {
            return (TrackLastWritten<T>) disabledWriteWindow;
        }
        return (TrackLastWritten<T>) lastWrittenObject.get(size, OpenAddressingLastWritten::objectEquality);
    }
    
    @SuppressWarnings("unchecked")
    public <T> void returnTrackLastRead(TrackLastRead<T> returned) {
        if (returned != disabledReadWindow) {
            doReturn(lastReads, (TrackLastRead<Object>) returned);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void returnTrackLastWrittenReferenceEquality(TrackLastWritten<T> returned) {
        if (returned != disabledWriteWindow) {
            doReturn(lastWrittenReference, (TrackLastWritten<Object>) returned);
        }
    }
    @SuppressWarnings("unchecked")
    public <T> void returnTrackLastWrittenObjectEquality(TrackLastWritten<T> returned) {
        if (returned != disabledWriteWindow) {
            doReturn(lastWrittenObject, (TrackLastWritten<Object>) returned);
        }
    }
    
    private <T> void doReturn(CacheFactory<T> target, T returned) {
        if (returned instanceof ClearableWindow) {
            target.put(((ClearableWindow)returned).size(), returned);
        }
    }

    private static Boolean clear(Object c) {
        if (c instanceof ClearableWindow) {
            ClearableWindow clearable = (ClearableWindow)c;
            if (clearable.size() >= 1000) {
                clearable.clear();
                return true;
            }
        }
        return false;
    }

}
