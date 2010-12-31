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
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.toccatasystems.util.ChainIterator;
import com.toccatasystems.util.ReverseListIterator;

public class DexBasicBlock {
	private DexMethodBody parent;
	private String name;
	private DexBasicBlock fallthrough;
	private DexBasicBlock fallthroughPredecessor;
	private List<DexInstruction> instructions;
	private List<DexBasicBlock> predecessors;
	private List<DexBasicBlock> successors;
	private List<DexBasicBlock> exceptionSuccessors; 
	
	public DexBasicBlock(DexMethodBody parent) {
		this.parent = parent;
		instructions = new ArrayList<DexInstruction>();
		predecessors = new LinkedList<DexBasicBlock>();
		successors = new LinkedList<DexBasicBlock>();
		exceptionSuccessors = new LinkedList<DexBasicBlock>();
	}
	
	public DexBasicBlock(DexMethodBody parent, String name) {
		this.parent = parent;
		this.name = name;
		instructions = new ArrayList<DexInstruction>();
		predecessors = new LinkedList<DexBasicBlock>();
		successors = new LinkedList<DexBasicBlock>();
		exceptionSuccessors = new LinkedList<DexBasicBlock>();
	}
	
	protected void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public DexMethodBody getParent() {
		return this.parent;
	}
	
	public void add( DexInstruction ins ) {
		instructions.add(ins);
		ins.setParent(this);
	}
	
	public void insertBefore( DexInstruction ins, DexInstruction before ) {
		for( int i=0; i<instructions.size(); i++ ) {
			if( instructions.get(i) == before ) {
				instructions.add(i, ins);
				return;
			}
		}
		instructions.add(ins);
	}
	
	public void insertAfter( DexInstruction ins, DexInstruction after ) {
		for( int i=0; i<instructions.size(); i++ ) {
			if( instructions.get(i) == after ) {
				instructions.add(i+1, ins);
			}
		}
		instructions.add(ins);
	}

	public void addSuccessor( DexBasicBlock next ) {
		successors.add(next);
		next.predecessors.add(this);
	}
	
	public void addFallthroughSuccessor( DexBasicBlock next ) {
		addSuccessor(next);
		this.fallthrough = next;
		next.fallthroughPredecessor = this;
	}
	
	protected void addExceptionSuccessor( DexBasicBlock handler ) {
		if( !this.exceptionSuccessors.contains(handler) ) {
			this.exceptionSuccessors.add(handler);
			handler.predecessors.add(this);
		}
	}

	public DexInstruction first() {
		return instructions.get(0);
	}
	
	public DexInstruction last() {
		return instructions.get(instructions.size()-1);
	}
	
	public DexInstruction getNext( DexInstruction after ) {
		Iterator<DexInstruction> it;
		for( it = instructions.iterator(); it.hasNext(); ) {
			if( it.next() == after )
				break;
		}
		if( it.hasNext() ) 
			return it.next();
		else
			return null;
	}
	
	public DexInstruction getPrevious( DexInstruction before ) {
		Iterator<DexInstruction> it;
		DexInstruction prev = null;
		for( it = instructions.iterator(); it.hasNext(); ) {
			DexInstruction inst = it.next();
			if( inst == before )
				return prev;
			prev = inst;
		}
		return null;
	}
	
	public DexInstruction getTerminator() {
		return instructions.get(instructions.size()-1);
	}
	
	/**
	 * @return the number of instructions in the block
	 */
	public int size() {
		return instructions.size();
	}
	
	public int getPC() {
		return instructions.get(0).getPC();
	}
	
	/**
	 * @return the PC of the first instruction after the end of the block
	 */
	public int getEndPC() {
		DexInstruction ti =  getTerminator();
		return ti.getPC() + ti.size();
	}
	
	/**
	 * @return an iterator over the instructions in the block.
	 */
	public Iterator<DexInstruction> iterator() {
		return instructions.iterator();
	}
	
	public Iterator<DexInstruction> reverseIterator() {
		return new ReverseListIterator<DexInstruction>(instructions);
	}
	
	/**
	 * @return an iterator over the instructions in the block, initially
	 * pointing at the specified instruction.
	 */
	public Iterator<DexInstruction> iterator(DexInstruction start) {
		for( int i=0; i<instructions.size(); i++ ) {
			if( instructions.get(i) == start ) {
				return instructions.listIterator(i);
			}
		}
		return instructions.listIterator(instructions.size());
	}
	
	public Iterator<DexInstruction> reverseIterator(DexInstruction start) {
		for( int i=0; i<instructions.size(); i++ ) {
			if( instructions.get(i) == start ) {
				return new ReverseListIterator<DexInstruction>(instructions, i);
			}
		}
		return new ReverseListIterator<DexInstruction>(instructions,0);
	}		
	
	/**
	 * @return true if the block has no instructions
	 */
	public boolean isEmpty() {
		return instructions.isEmpty();
	}
	
	/**
	 * @return the instruction at the given index.
	 */
	public DexInstruction get( int idx ) {
		return instructions.get(idx);
	}
	
	public Iterator<DexBasicBlock> predIterator() { 
		return predecessors.iterator();
	}
	
	public int getNumPredecessors() {
		return predecessors.size();
	}
	
	public DexBasicBlock getPredecessor( int idx ) {
		return predecessors.get(idx);
	}
	
	public Iterator<DexBasicBlock> succIterator() {
		return successors.iterator();
	}
	
	public int getNumSuccessors() {
		return successors.size();
	}
	
	public DexBasicBlock getSuccessor( int idx ) {
		return successors.get(idx);
	}
	
	public DexBasicBlock getFallthroughSuccessor() {
		return fallthrough;
	}
	
	public DexBasicBlock getFallthroughPredecessor() {
		return fallthroughPredecessor;
	}

	public boolean hasExceptionSuccessors() {
		return exceptionSuccessors.size() != 0;
	}
	
	public int getNumExceptionSuccessors() {
		return exceptionSuccessors.size();
	}
	
	public DexBasicBlock getExceptionSuccessor( int idx ) {
		return exceptionSuccessors.get(idx);
	}
	
	public Iterator<DexBasicBlock> exceptionIterator() {
		return exceptionSuccessors.iterator();
	}
	
	public void moveToEnd() {
		if( fallthroughPredecessor != null ) {
			int lastPC = fallthroughPredecessor.last().getPC();
			fallthroughPredecessor.add( new DexInstruction(parent, lastPC, DexOpcodes.GOTO16, new int[0], getPC()) );
		}
		parent.moveToEnd(this);
	}
	
	/**
	 * Iterate over both normal and exception successors together
	 */
	public Iterator<DexBasicBlock> allSuccIterator() {
		return new ChainIterator<DexBasicBlock>(successors,exceptionSuccessors);
	}
	
	public String toString() {
		return name;
	}
	
	public void disassemble( Formatter fmt, boolean verbose ) {
		fmt.format("    %s:", getName());
		if( getNumPredecessors() != 0 ) {
			fmt.format( "    ; preds: " );
			int count = 0;
			for( Iterator<DexBasicBlock> pit = predIterator(); pit.hasNext(); ) {
				fmt.format( (count == 0 ? "%s" : ", %s"), pit.next().getName() );
				count++;
			}
		}
		fmt.format("\n");
		for( Iterator<DexInstruction> ii = iterator(); ii.hasNext(); ) {
			DexInstruction inst = ii.next();
			if( verbose ) {
				int pc = inst.getPC();
				fmt.format("        %04X: ", pc);
				/* Print the raw data */
				for( int j=0; j<5; j++ ) {
					if( j < inst.size() ) {
						fmt.format("%04X ", inst.getUShort(j));
					} else {
						fmt.format( "     " );
					}
				}
			} else {
				fmt.format( "        " );
			}
			fmt.format( "%s\n%s", inst.disassemble(),
					inst.formatTable("            "));
		}
		if( hasExceptionSuccessors() ) {
			fmt.format( "            ; exceptions: " );
			int count = 0;
			for( Iterator<DexBasicBlock> eit = exceptionIterator(); eit.hasNext(); ) {
				DexBasicBlock ebb = eit.next();
				fmt.format( (count == 0 ? "%s" : ", %s"), ebb.getName() );
				count++;
			}
			fmt.format("\n");
		}
	}
	
	public void disassemble( Appendable app, boolean verbose ) {
		Formatter fmt = new Formatter(app);
		disassemble(fmt, verbose);
	}
	
	public String disassemble( boolean verbose ) {
		StringBuilder builder = new StringBuilder();
		Formatter fmt = new Formatter(builder);
		disassemble(fmt, verbose);
		return builder.toString();
	}
}
