package com.toccatasystems.dalvik.analysis;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexInstruction;

/**
 * Generic iterative worklist algorithm for dataflow analysis.
 * @author nkeynes
 *
 * @param <Param>
 */
public abstract class AbstractDataflowAnalysis<Param> {

	Map<DexBasicBlock, Map<DexBasicBlock, Param>> data;
	boolean forwardFlow;
	
	protected AbstractDataflowAnalysis( ) {
		this.data = new HashMap<DexBasicBlock, Map<DexBasicBlock, Param>>();
	}
	
	protected abstract Param enterBlock( DexBasicBlock block, Map<DexBasicBlock,Param> params );
	protected abstract Param visit( DexInstruction inst, Param param );
	
	protected void printParamMap( PrintStream out, Map<DexBasicBlock,Param> params ) {
		Iterator<Map.Entry<DexBasicBlock, Param>> it = params.entrySet().iterator();
		while( it.hasNext() ) {
			Map.Entry<DexBasicBlock, Param> ent = it.next();
			out.println("  " + ent.getKey() + ": " + ent.getValue());
		}
	}
	
	protected Map<DexBasicBlock, Param> leaveBlock( DexBasicBlock block, Param param ) {
		Map<DexBasicBlock, Param> result = new HashMap<DexBasicBlock,Param>();
		Iterator<DexBasicBlock> it = getNextIterator(block);
		while( it.hasNext() ) {
			result.put(it.next(), param);
		}
		return result;
	}
	
	protected void computeDataflow( DexBasicBlock start, Param startParams ) {
		List<DexBasicBlock> list = new LinkedList<DexBasicBlock>();
		list.add(start);
		computeDataflow(list, startParams);
	}
	
	protected abstract Iterator<DexInstruction> getInstIterator( DexBasicBlock block );
	protected abstract Iterator<DexBasicBlock> getNextIterator( DexBasicBlock block );
	
	protected void computeDataflow( Collection<DexBasicBlock> start, Param startParams ) {
		data.clear();
		for( Iterator<DexBasicBlock> it = start.iterator(); it.hasNext(); ) {
			Map<DexBasicBlock, Param> initMap = new HashMap<DexBasicBlock,Param>();
			initMap.put(null, startParams);
			data.put( it.next(), initMap );
		}
		List<DexBasicBlock> worklist = new LinkedList<DexBasicBlock>(start);
		while( !worklist.isEmpty() ) {
			DexBasicBlock bb = worklist.remove(0);

			Map<DexBasicBlock, Param> inEdges = data.get(bb);
			Param params = enterBlock(bb, inEdges);
			Iterator<DexInstruction> ii = getInstIterator(bb);
			while( ii.hasNext() ) {
				params = visit(ii.next(), params);
			}
			Map<DexBasicBlock, Param> outEdges = leaveBlock(bb, params);
			Iterator<Map.Entry<DexBasicBlock,Param>> outit = outEdges.entrySet().iterator();
			while( outit.hasNext() ) {
				Map.Entry<DexBasicBlock,Param> ent = outit.next();
				DexBasicBlock outbb = ent.getKey();
				Param outp = ent.getValue();
				Map<DexBasicBlock, Param> outmap = data.get(outbb);
				if( outmap == null ) {
					outmap = new HashMap<DexBasicBlock,Param>();
					data.put(outbb, outmap);
				}
				Param oldparam = outmap.get(bb);
				if( oldparam == null || !oldparam.equals(outp) ) {
					outmap.put(bb, outp);
					worklist.add(outbb);
				}
			}
		}
	}
}
