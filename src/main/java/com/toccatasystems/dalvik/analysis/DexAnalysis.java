/**
 * Copyright (c) 2010 Toccata Systems.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

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
