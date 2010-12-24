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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.toccatasystems.dalvik.DexArgument;
import com.toccatasystems.dalvik.DexBasicBlock;
import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexInstruction;
import com.toccatasystems.dalvik.DexMethodBody;

/**
 * Compute bidirectional use-def sets using standard reaching-definitions
 * method. Note: moves between regs do not count as definitions, instead
 * we copy the definition sets.
 */ 
public class ComputeUseDefInfo extends ForwardDataflowAnalysis<ComputeUseDefInfo.Params> {

	protected static class Params {
		Set<DexInstruction> defns[];
		
		@SuppressWarnings("unchecked")
		public Params(DexMethodBody body) {
			defns = (Set<DexInstruction>[])new Set[body.getNumRegisters()];
			for( int i=0; i<body.getNumRegisters(); i++ ) {
				defns[i] = new HashSet<DexInstruction>();
			}
			setArgDefs(body);
		}
		
		@SuppressWarnings("unchecked")
		public Params(Params o) {
			defns = (Set<DexInstruction>[])new Set[o.defns.length];
			for( int i=0; i<o.defns.length; i++ ) {
				defns[i] = new HashSet<DexInstruction>();
				defns[i].addAll(o.defns[i]);
			}
		}
		
		public void define( int reg, DexInstruction inst ) {
			defns[reg].clear();
			defns[reg].add(inst);
		}
		
		public void use( int reg, DexInstruction inst, int operandIdx ) {
			inst.setRegisterDefs(operandIdx, defns[reg]);
			for( Iterator<DexInstruction> it = defns[reg].iterator(); it.hasNext(); ) {
				DexInstruction used = it.next();
				used.addUse(inst, operandIdx);
			}
		}
		
		public void move( int dest, int src ) {
			if( dest != src ) {
				defns[dest].clear();
				defns[dest].addAll(defns[src]);
			}
		}
		
		public void merge( Params o ) {
			for( int i=0; i<defns.length; i++ ) {
				defns[i].addAll(o.defns[i]);
			}
		}
		
		public boolean equals( Object other ) {
			if( other instanceof Params ) {
				Params o = (Params)other;
				for( int i=0; i<defns.length; i++ ) {
					if( !defns[i].equals(o.defns[i]) ) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder("[");
			for( int i=0; i<defns.length; i++ ) {
				if( i != 0 )
					builder.append( ", " );
				builder.append('v');
				builder.append(Integer.toString(i));
				builder.append(" = {");
				for( Iterator<DexInstruction> it = defns[i].iterator(); it.hasNext(); ) {
					DexInstruction inst = it.next();
					builder.append( inst.disassemble() );
				}
				builder.append("}");
			}
			builder.append("]");
			return builder.toString();
		}
		
		private void setArgDefs( DexMethodBody body ) {
			for( int i=0; i < body.getNumArguments(); i++ ) {
				DexArgument arg = body.getArgument(i);
				defns[arg.getRegister(0)].add(arg);
			}
		}
	}
	
	public void analyse( DexMethodBody body ) {
		computeDataflow(body, new Params(body));
	}
	
	public void analyse( DexFile file ) {
		for( Iterator<DexMethodBody> it = file.methodBodyIterator(); it.hasNext(); ) {
			analyse(it.next());
		}
	}
	
	@Override
	protected Params enterBlock(DexBasicBlock block, Map<DexBasicBlock,Params> params) {
		Iterator<Params> it = params.values().iterator();
		Params result = new Params(it.next());
		while(it.hasNext()) {
			result.merge(it.next());
		}
		return result;
	}

	@Override
	protected Params visit(DexInstruction inst, Params param) {
		if( inst.isMove() ) {
			param.use(inst.getRegister(1), inst, 1);
			param.move(inst.getRegister(0), inst.getRegister(1));
		} else {
			for( int i=0; i<inst.getNumRegisters(); i++ ) {
				if( inst.readsOperand(i) ) {
					param.use(inst.getRegister(i), inst, i);
				}
			}
			if( inst.writesOperand(0) ) {
				param.define(inst.getRegister(0), inst);
			}
		}
		return param;	
	}

}
