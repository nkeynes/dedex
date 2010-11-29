package com.toccatasystems.dalvik;

public class DexMethodBody {
	public static class ExceptionBlock {
		int startInst;
		int instCount;
		int handleInst;
		String type;
		
		public ExceptionBlock(int startInst, int instCount, int handleInst, String type ) {
			this.startInst = startInst;
			this.instCount = instCount;
			this.handleInst = handleInst;
			this.type = type;
		}
	}

	private DexMethod parent;
	private int numRegisters;
	private int inArgWords;
	private int outArgWords;
	private short []code;
	private DexDebug debug;
	private ExceptionBlock []handlers;
	
	public DexMethodBody( int numRegisters, int inArgWords, int outArgWords,
			short[]code, DexDebug debug, ExceptionBlock[]handlers ) {
		this.numRegisters = numRegisters;
		this.inArgWords = inArgWords;
		this.outArgWords = outArgWords;
		this.code = code;
		this.debug = debug;
		this.handlers = handlers;
	}
	
	protected void setParent( DexMethod parent ) { this.parent = parent; }
	
	public DexMethod getParent() { return parent; }
	
	public int getNumRegisters() { return numRegisters; }
	public int getInArgWords() { return inArgWords; }
	public int getOutArgWords() { return outArgWords; }
	public DexDebug getDebugInfo() { return debug; }
	
	public short []getCode() { return code; }
	
	public ExceptionBlock []getExceptionHandlers() { return handlers; }
	
}
