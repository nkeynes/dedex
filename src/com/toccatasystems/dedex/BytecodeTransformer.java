package com.toccatasystems.dedex;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexTryCatch;

/**
 * This class does the low-level work of translating dalvik bytecode
 * back into jvm bytecode. 
 * 
 * @author nkeynes
 */
public class BytecodeTransformer {

	
	
	public void transform( DexMethodBody body, MethodVisitor out ) {
		/* Construct labels for each basic block */
		Map<DexBasicBlock, Label> labelMap = new TreeMap<DexBasicBlock,Label>();
		for( Iterator<DexBasicBlock> it = body.iterator(); it.hasNext(); ) {
			labelMap.put(it.next(), new Label());
		}
		
		out.visitCode();
		
		/* Visit the exception handlers - ASM requires that this is done before
		 * reaching the labels in question, so it's simplest to do it first
		 */
		for( Iterator<DexTryCatch> it = body.handlerIterator(); it.hasNext(); ) {
			DexTryCatch e = it.next();
			DexBasicBlock start = e.getStartBlock();
			DexBasicBlock end = e.getEndBlock();
			DexBasicBlock handler = e.getHandlerBlock();
			out.visitTryCatchBlock(labelMap.get(start), labelMap.get(end), labelMap.get(handler), e.getInternalType() );
		}
		
		for( Iterator<DexBasicBlock> bbit = body.iterator(); bbit.hasNext(); ) {
			DexBasicBlock bb = bbit.next();
			out.visitLabel(labelMap.get(bb));
			for(Iterator<DexInstruction> ii = bb.iterator(); ii.hasNext(); ) {
				DexInstruction inst = ii.next();
				switch( inst.getOpcode() ) {
				}
			}
		}
		
	}
	
}
