package com.toccatasystems.dedex;

import java.util.Collection;
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
		 * entry points. If we find one, see if the entry points can be sliced
		 * from the exception block (i.e. they do not actually raise a exception
		 * in the Dalvik VM).
		 */
		for( Iterator<DexTryCatch> it = body.handlerIterator(); it.hasNext(); ) {
			DexTryCatch handler = it.next();
			List<DexBasicBlock> blocks = handler.getBlocks();
			for( ListIterator<DexBasicBlock> bbit = blocks.listIterator(); bbit.hasNext(); ) {
				DexBasicBlock bb = bbit.next();
				if( bb.getPC() != handler.getStartPC() ) {
					for( Iterator<DexBasicBlock> predit = bb.predIterator(); predit.hasNext(); ) {
						DexBasicBlock pred = predit.next();
						if( !blocks.contains(pred) ) {
							/* We have a branch into the middle of the exception region */
							if( canMove( bb ) ) {
								/* Block is moveable, so move it to the end of the method */
								bb.moveToEnd();
								bbit.remove();
								break;
							} else if( canExtend(pred, handler) ) {
								handler.setEndPC(pred.getEndPC());
								bbit.add(pred);
							} else {
								//System.out.println( "Error: Branch into " + bb.getName() + " from " + pred.getName() + " in " + 
								//		body.getParent().getDisplaySignature() );
								//body.disassemble(System.out);
								break;
							}
						}
					}
				}
				
			}
		
		}
	}
	
}
