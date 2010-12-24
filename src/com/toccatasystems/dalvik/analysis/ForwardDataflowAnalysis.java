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

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethodBody;

public abstract class ForwardDataflowAnalysis<Param> extends AbstractDataflowAnalysis<Param> {

	protected void computeDataflow(DexMethodBody method, Param params) {
		computeDataflow(method.getEntryBlock(), params);
	}
	
	protected Iterator<DexInstruction> getInstIterator(DexBasicBlock block) {
		return block.iterator();
	}

	protected Iterator<DexBasicBlock> getNextIterator(DexBasicBlock block) {
		return block.allSuccIterator();
	}

}
