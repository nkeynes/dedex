package com.toccatasystems.dalvik;

import java.util.ArrayList;
import java.util.List;

/**
 * DexDebug contains the debug information for a method, extracted directly debug_info_item.
 * This is described as a state machine based on DWARF3.
 * 
 * @author nkeynes
 *
 */
public class DexDebug {
	
	public final static int END_SEQUENCE = 0x00;
	public final static int ADVANCE_PC   = 0x01;
	public final static int ADVANCE_LINE = 0x02;
	public final static int START_LOCAL  = 0x03;
	public final static int START_LOCAL_EXT = 0x04;
	public final static int END_LOCAL     = 0x05;
	public final static int RESTART_LOCAL = 0x06;
	public final static int SET_PROLOGUE_END = 0x07;
	public final static int SET_EPILOGUE_BEGIN = 0x08;
	public final static int SET_FILE = 0x09;

	public static class Op {
		public int op;
		public int intValue;
		public String name;
		public String type;
		public String signature;
		
		public Op(int op, int intValue, String name, String type, String sig) {
			this.op = op;
			this.intValue = intValue;
			this.name = name;
			this.type = type;
			this.signature = sig;
		}
	};
	
	private int startLine;
	private String paramNames[];
	
	private List<Op> debug;
	
	public DexDebug(int startLine, String paramNames[]) {
		this.startLine = startLine;
		this.paramNames = paramNames;
		this.debug = new ArrayList<Op>();
	}

	public int getOpCount() {
		return debug.size();
	}
	
	public Op getOp(int index) {
		return debug.get(index);
	}

	public int getStartLine() {
		return startLine;
	}
	
	public String[] getParamNames() {
		return paramNames;
	}
	
	public String getParamName( int idx ) {
		if( paramNames == null )
			return null;
		return paramNames[idx];
	}
	
	protected void add( int op ) {
		debug.add( new Op(op, 0, null, null, null) );
	}
	
	protected void add( int op, int intValue ) {
		debug.add( new Op(op, intValue, null, null, null) );
	}
	
	protected void add( int op, int intValue, String name, String type, String sig) {
		debug.add( new Op(op, intValue, name, type, sig) );
	}
}
