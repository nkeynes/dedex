package com.toccatasystems.dalvik.analysis;

import java.util.Iterator;

import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexMethodBody;

public abstract class DexAnalysis {

	public abstract void analyse( DexMethodBody body );
	
	public void analyse( DexFile file ) {
		for( Iterator<DexMethodBody> it = file.methodBodyIterator(); it.hasNext(); ) {
			analyse(it.next());
		}
	}
	
}
