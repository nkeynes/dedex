package com.toccatasystems.dalvik;

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
	
	protected void setParent( DexMethodBody parent ) {
		this.parent = parent;
	}
}
