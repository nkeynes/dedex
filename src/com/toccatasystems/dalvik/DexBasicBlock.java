package com.toccatasystems.dalvik;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class DexBasicBlock {
	private String name;
	private List<DexInstruction> instructions;
	private List<DexBasicBlock> predecessors;
	private List<DexBasicBlock> successors;
	
	public DexBasicBlock() {
		instructions = new ArrayList<DexInstruction>();
		predecessors = new LinkedList<DexBasicBlock>();
		successors = new LinkedList<DexBasicBlock>();
	}
	
	public DexBasicBlock(String name) {
		this.name = name;
		instructions = new ArrayList<DexInstruction>();
		predecessors = new LinkedList<DexBasicBlock>();
		successors = new LinkedList<DexBasicBlock>();
	}
	
	protected void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	protected void add( DexInstruction ins ) {
		instructions.add(ins);
	}
	
	protected void addSuccessor( DexBasicBlock next ) {
		successors.add(next);
		next.predecessors.add(this);
	}

	public DexInstruction first() {
		return instructions.get(0);
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
	
}
