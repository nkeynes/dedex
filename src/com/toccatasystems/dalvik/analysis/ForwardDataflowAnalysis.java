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
