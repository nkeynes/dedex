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

package com.toccatasystems.dalvik;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a try-catch handler as part of a method body.
 * @author nkeynes
 *
 */
public class DexTryCatch {
	private DexMethodBody parent;
	private int startInst;
	private int instCount;
	private int handleInst;
	private DexType type;
	
	public DexTryCatch(int startInst, int instCount, int handleInst, String type ) {
		this.parent = null;
		this.startInst = startInst;
		this.instCount = instCount;
		this.handleInst = handleInst;
		this.type = (type == null ? null : new DexType(type));
	}
	
	public int getStartPC() {
		return startInst;
	}
	
	public int getEndPC() {
		return startInst + instCount;
	}
	
	public int getHandlerPC() {
		return handleInst;
	}

	public DexType getType() {
		return type;
	}
	
	public void setEndPC( int endPC ) {
		instCount = endPC - startInst;
	}
	
	public boolean isLiveAt( int pc ) { 
		return startInst <= pc && (startInst+instCount) > pc;
	}
	
	public String getInternalType() {
		if( type == null ) {
			return null;
		} else {
			return type.getInternalName();
		}
	}

	public DexBasicBlock getStartBlock() {
		return parent.getBlockForPC(startInst);
	}
	
	public DexBasicBlock getEndBlock() {
		return parent.getBlockForPC(startInst+instCount);
	}
	
	public DexBasicBlock getHandlerBlock() {
		return parent.getBlockForPC(handleInst);
	}
	
	public List<DexBasicBlock> getBlocks() {
		List<DexBasicBlock> result = new ArrayList<DexBasicBlock>();
		for( Iterator<DexBasicBlock> it = parent.iterator(); it.hasNext(); ) {
			DexBasicBlock bb = it.next();
			if( bb.getPC() >= startInst + instCount )
				break;
			if( bb.getPC() >= startInst )
				result.add(bb);
		}
		return result;
	}
	
	public Iterator<DexBasicBlock> blockIterator() {
		return getBlocks().iterator();
	}
	
	protected void setParent( DexMethodBody parent ) {
		this.parent = parent;
	}
}
