package com.toccatasystems.dedex;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.toccatasystems.dalvik.DexAccessFlags;
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
	
	public DexDump( PrintStream out ) {
		this.out = out;
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
		
		String modifier = DexAccessFlags.toString(clz.getFlags());
		out.print( modifier + DexAccessFlags.getClassKind(clz.getFlags()) + " " + currClassName);
		if( clz.superclass != null ) 
			out.print(" extends " + formatTypeName(clz.superclass));
		if( clz.interfaces != null && clz.interfaces.length != 0 ) {
			out.print(" implements " );
			for( int i=0; i<clz.interfaces.length; i++ ) {
				if( i != 0 )
					out.print( ", " );
				out.print( formatTypeName(clz.interfaces[i]) );
			}
		}
		out.println( " {" );
	}

	public void enterField(DexField field) {
	}

	public void leaveField(DexField field) {
		out.print("    " + DexAccessFlags.toString(field.getFlags()) + formatTypeName(field.getType()) + " " + field.getName());
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
			out.print("    " + DexAccessFlags.toString(method.getFlags()) + formatTypeName(method.getReturnType()) + " " + name + "(" );
			for( int i=0; i<method.getNumParamTypes(); i++ ) {
				if( i != 0 )
					out.print(", ");
				out.print( formatTypeName(method.getParamType(i)) );
			}
			out.print( ")" );
		}
		
		if( method.hasBody() ) {
			out.println(" { }");
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
