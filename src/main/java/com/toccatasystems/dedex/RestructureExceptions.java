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

package com.toccatasystems.dedex;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexTryCatch;

/**
 * Dx tends to move code around in a way that a) breaks the JVM verifier, and
 * b) breaks Jode by causing the blocks to lose structural containment. Presumably
 * the result is intended to be more optimized for Dalvik execution.
 *
 * Try to fix undo this here. 
 */
public class RestructureExceptions {

	public void transform( DexFile file ) {
		for( Iterator<DexMethodBody> it = file.methodBodyIterator(); it.hasNext(); ) {
			transform(it.next());
		}
	}
	
	/**
	 * Return true if the block can trivially be moved out of the exception block - 
	 * ie it has no successors, throws no exceptions, and does not start with a 
	 * move-result*
	 */
	private boolean canMove( DexBasicBlock bb ) {
		return bb.getNumSuccessors() == 0 && !bb.hasExceptionSuccessors() &&
		    !bb.first().isMoveResult();
	}
	
	/**
	 * Return true if the handler could be extended to cover the given basic 
	 * block, which requires that a) the basic block be the handlers 'end block',
	 * and b) the basic block have no exception successors. 
	 */
	public boolean canExtend( DexBasicBlock bb, DexTryCatch handler ) {
		return bb == handler.getEndBlock() && !bb.hasExceptionSuccessors();
	}
	
	public void transform( DexMethodBody body ) {
		/**
		 * Scan the try-catch list for exception regions that have multiple
		 * entry points and change to one or more single-entry regions (JVM
		 * does not approve)
		 */
		boolean needSort = false;
		for( ListIterator<DexTryCatch> it = body.handlerIterator(); it.hasNext(); ) {
			DexTryCatch handler = it.next();
			List<DexBasicBlock> blocks = handler.getBlocks();
			for( ListIterator<DexBasicBlock> bbit = blocks.listIterator(); bbit.hasNext(); ) {
				DexBasicBlock bb = bbit.next();
				if( bb.getPC() != handler.getStartPC() && bb.getPC() < handler.getEndPC() ) {
					for( Iterator<DexBasicBlock> predit = bb.predIterator(); predit.hasNext(); ) {
						DexBasicBlock pred = predit.next();
						if( !blocks.contains(pred) ) {
							/* We have a branch into the middle of the exception region */
							if( canMove( bb ) ) {
								/* Block is movable, so move it to the end of the method */
								bb.moveToEnd();
								bbit.remove();
								break;
							} else if( canExtend(pred, handler) ) {
								handler.setEndPC(pred.getEndPC());
								bbit.add(pred);
							} else {
								/* split the handler block */
								DexTryCatch rest = handler.split(bb.getPC());
								it.add(rest);
								needSort = true;
								break;
							}
						}
					}
				}	
			}
		}
		
		if( needSort ) {
			/* If we split any blocks, resort the handler list to ensure they're 
			 * ordered by start PC
			 */
			Collections.sort(body.getExceptionHandlers(), new Comparator<DexTryCatch>() {
				@Override
				public int compare(DexTryCatch o1, DexTryCatch o2) {
					return o1.getStartPC() - o2.getStartPC();
				}
			});
		}
		
		int finalCount = 0;
		for( ListIterator<DexTryCatch> it = body.handlerIterator(); it.hasNext(); ) {
			DexTryCatch handler = it.next();
			/* Make sure we have a label for the handler-end. This can be missing
			 * if we added the final block to the end of the handler region and 
			 * there was no existing label after it. */
			if( body.getBlockForPC(handler.getEndPC()) == null ) {
				body.addBlock(handler.getEndPC(), "final." + finalCount);
				++finalCount;
			}
		}
	}
}
