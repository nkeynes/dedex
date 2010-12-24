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
import java.util.Map;
import java.util.TreeMap;

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

	public final static int FIRST_SPECIAL = 0x0A;
	public final static int LINE_BASE = -4;
	public final static int LINE_RANGE = 15;
	
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
	
	public static class Line {
		public String sourceFile;
		public int lineNo;
		
		public Line( String sourceFile, int lineNo ) {
			this.sourceFile = sourceFile;
			this.lineNo = lineNo;
		}
	}
	
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
	
	public Map<Integer, Line> getLineNumberTable() {
		Map<Integer,Line> lines = new TreeMap<Integer,Line>();
		int pc = 0;
		int lineNo = startLine;
		String filename = null;
		int adjop;
		
		for( Iterator<Op> it = debug.iterator(); it.hasNext(); ) {
			Op op = it.next();
			switch( op.op ) {
			case ADVANCE_PC: pc += op.intValue; break;
			case ADVANCE_LINE: lineNo += op.intValue; break;
			case SET_FILE: filename = op.name; break;
			case START_LOCAL: case START_LOCAL_EXT: case END_LOCAL:
			case RESTART_LOCAL: case SET_PROLOGUE_END: case SET_EPILOGUE_BEGIN:
				break; /* Ignore */
			default:
				adjop = op.op - FIRST_SPECIAL;
				lineNo += LINE_BASE + (adjop%LINE_RANGE);
				pc += (adjop/LINE_RANGE);
				lines.put(pc, new Line(filename, lineNo));
			}
		}
		return lines;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for( int i=0; i<debug.size(); i++ ) {
			Op op = debug.get(i);
			builder.append("    ");
			switch( op.op ) {
			case END_SEQUENCE: builder.append("end"); break;
			case ADVANCE_PC:   builder.append("advance-pc   " + op.intValue); break;
			case ADVANCE_LINE: builder.append("advance-line " + op.intValue); break;
			case START_LOCAL:  builder.append("start-local  v" + op.intValue + " = " + op.type + " " + op.name); break;
			case START_LOCAL_EXT:builder.append("start-localex v" + op.intValue + " = " + op.type + " " + op.signature + " " + op.name); break;
			case END_LOCAL:    builder.append("end-local    v" + op.intValue); break;
			case RESTART_LOCAL:builder.append("reset-local  v" + op.intValue); break;
			case SET_PROLOGUE_END: builder.append("prologue-end"); break;
			case SET_EPILOGUE_BEGIN: builder.append("epilogue-begin"); break;
			case SET_FILE:     builder.append("set-file     " + op.name); break;
			default:           builder.append( Integer.toHexString(op.op) ); break;
			}
			builder.append("\n");
		}
		return builder.toString();
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
