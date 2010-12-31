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

package com.toccatasystems.dedex;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.toccatasystems.dalvik.DexAnnotation;
import com.toccatasystems.dalvik.DexClass;
import com.toccatasystems.dalvik.DexField;
import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexMethod;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexValue;
import com.toccatasystems.dalvik.DexVisitor;

public class DexDump implements DexVisitor {

	private PrintStream out;
	private String currPackageName;
	private String currClassName;
	private boolean verbose;
	
	String formatTypeName( String typeName ) {
		int idx;
		switch( typeName.charAt(0) ) {
		case 'B': return "byte";
		case 'C': return "char";
		case 'D': return "double";
		case 'F': return "float";
		case 'I': return "int";
		case 'J': return "long";
		case 'L':
			idx = typeName.indexOf(';');
			if( idx == -1 ) {
				return typeName;
			} else {
				return typeName.substring(1, idx).replace('/', '.');
			}
		case 'S': return "short";
		case 'V': return "void";
		case 'Z': return "boolean";
		case '[': return formatTypeName( typeName.substring(1)) + "[]";
		default: return typeName;
		}
	}
	
	String formatAccessModifier(int flags) {
		return "";
	}
	
	public DexDump( PrintStream out, boolean verbose ) {
		this.out = out;
		this.verbose = verbose;
		currPackageName = "";
		currClassName = "";
	}
	
	public void enterFile(DexFile file) {
	}


	public void enterClass(DexClass clz) {
		String name = formatTypeName(clz.getName());
		String packageName;
		int rpos = name.lastIndexOf('.');
		if( rpos == -1 ) {
			currClassName = name;
			packageName = "";
		} else {
			currClassName = name.substring(rpos+1);
			packageName = name.substring(0,rpos);
		}
		if( !packageName.equals(currPackageName) ) {
			out.println("\npackage " + packageName + ";\n");
			currPackageName = packageName;
		}
		
		String modifier = clz.getFlagsString();
		out.print( modifier + clz.getKind() + " " + currClassName);
		if( clz.getSuperName() != null ) 
			out.print(" extends " + formatTypeName(clz.getSuperName()));
		String interfaces[] = clz.getInterfaces();
		if( interfaces != null && interfaces.length != 0 ) {
			out.print(" implements " );
			for( int i=0; i<interfaces.length; i++ ) {
				if( i != 0 )
					out.print( ", " );
				out.print( formatTypeName(interfaces[i]) );
			}
		}
		out.println( " {" );
	}

	public void enterField(DexField field) {
	}

	public void leaveField(DexField field) {
		out.print("    " + field.getFlagsString() + formatTypeName(field.getType()) + " " + field.getName());
		if( field.hasInitializer() ) {
			out.print( " = " + formatValue(field.getInitializer()));
		}
		out.println(";");
	}

	public void enterMethod(DexMethod method) {
		out.println();
	}

	public void leaveMethod(DexMethod method) {
		String name = method.getName();
		if( name.equals("<clinit>") ) {
			out.print("    static");
		} else {
			if( name.equals("<init>") )
				name = currClassName;
			out.print("    " + method.getFlagsString() + formatTypeName(method.getReturnType()) + " " + name + "(" );
			for( int i=0; i<method.getNumParamTypes(); i++ ) {
				if( i != 0 )
					out.print(", ");
				out.print( formatTypeName(method.getParamType(i)) );
			}
			out.print( ")" );
		}
		
		String[]throwtypes = method.getThrows();
		if( throwtypes != null ) {
			out.print(" throws ");
			for( int i=0; i<throwtypes.length; i++ ) {
				if( i != 0 )
					out.print(", ");
				out.print(formatTypeName(throwtypes[i]));
			}
		}
		
		if( method.hasBody() ) {
			out.println(" {");
			method.getBody().disassemble(out, verbose);
			out.println("    }");
		} else {
			out.println(";");
		}
	}

	public void visitMethodBody(DexMethodBody body) {
		// TODO Auto-generated method stub

	}

	public void leaveClass(DexClass clz) {
		out.println( "}" );
	}

	public void leaveFile(DexFile file) {
	}

	public void visitAnnotation(DexAnnotation annotation) {
		out.print("    @" + formatTypeName(annotation.getType()) + "(");
		int size = annotation.size();
		if( size != 0 ) {
			if( size != 1 )
				out.println("        ");
			int count = 0;
			for( Iterator<Map.Entry<String,DexValue>> it = annotation.entrySet().iterator(); it.hasNext(); ) {
				if( count != 0 ) {
					out.print(",\n        ");
				}
				count++;
				Map.Entry<String,DexValue> entry = it.next();
				out.print( entry.getKey() + " = " + formatValue(entry.getValue()) );
			}
		}
		out.println(")");
	}
	
	public String formatValue(DexValue value) {
		Object o = value.getValue();
		if( o == null )
			return null;
		else if( value.getType() == DexValue.ARRAY ) {
			DexValue [] arr = (DexValue [])o;
			StringBuffer buf = new StringBuffer("{");
			for( int i=0; i<arr.length; i++ ) {
				if( i != 0 ) 
					buf.append( ", " );
				buf.append( formatValue(arr[i]) );
			}
			buf.append( "}" );
			return buf.toString();
		} else if( value.getType() == DexValue.TYPE ) {
			return formatTypeName(o.toString());
		} else if( value.getType() == DexValue.STRING ) {
			return "\"" + StringEscapeUtils.escapeJava(o.toString()) + "\"";
		} else {
			return o.toString();
		}
	}

	public void visitParamAnnotation(int paramIndex, DexAnnotation annotation) {
		out.print("@" + annotation.getType());
	}

}
