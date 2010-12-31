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

/**
 * Synthetic subclass of DexInstruction that wraps method arguments up in a
 * convenient way for use-def chains. 
 * @author nkeynes
 *
 */
public class DexArgument extends DexInstruction {

	public DexArgument(DexMethodBody body, int reg, int paramIdx) {
		super(body, 0, DexOpcodes.ARG, new int[1], paramIdx);
		setRegister(0, reg);
	}
	
	public String disassemble() {
		return "arg " + getIntOperand() + " " + formatOperands() + " " + formatUses();
	}
	
	static DexArgument[] getArguments(DexMethodBody body) {
		DexMethod method = body.getParent();
		int numParams = method.getNumCallingParamTypes();
		int paramRegOffset = body.getNumRegisters() - body.getInArgWords();
		DexArgument[] args = new DexArgument[numParams];
		for( int i=0; i<numParams; i++ ) {
			args[i] = new DexArgument(body, paramRegOffset+i,i);
			String type = method.getCallingParamType(i);
			if( type.equals("D") || type.equals("J") ) {
				paramRegOffset++;
			}
		}
		return args;
	}
}
