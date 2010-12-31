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
public abstract class AbstractDataflowAnalysis<Param> extends DexAnalysis {

	Map<DexBasicBlock, Map<DexBasicBlock, Param>> data;
	boolean forwardFlow;
	PrintStream debugOut = null;
	
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
	
	public void setDebug( PrintStream debugOut ) {
		this.debugOut = debugOut;
	}
	
	protected abstract Iterator<DexInstruction> getInstIterator( DexBasicBlock block );
	protected abstract Iterator<DexBasicBlock> getNextIterator( DexBasicBlock block );
	
	protected void computeDataflow( Collection<DexBasicBlock> start, Param startParams ) {
		if( debugOut != null ) {
			debugOut.println( "Computing dataflow on " + start.iterator().next().getParent().getParent().getDisplaySignature() + " from " + start );
		}
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
			if( debugOut != null ) {
				debugOut.println("Entering " + bb.getName() + ":");
				printParamMap(debugOut, inEdges);
			}
			Param params = enterBlock(bb, inEdges);
			if( debugOut != null ) {
				debugOut.println("  => " + params );
			}
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
