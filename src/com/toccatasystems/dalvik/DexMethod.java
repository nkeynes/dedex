package com.toccatasystems.dalvik;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class DexMethod extends DexItem {
	private String classType;
	private String returnType;
	private String paramTypes[];
	private DexMethodBody code;
	private List<List<DexAnnotation>> paramAnnotations;
	
	public DexMethod(String name, String returnType, String paramTypes[], int flags) {
		super(name, flags);
		this.returnType = returnType;
		this.paramTypes = paramTypes;
		this.code = null;
		if( this.paramTypes == null ) {
			this.paramTypes = new String[0];
		}
	}
	
	public DexMethod(String type, String name, DexMethod prototype) {
		super(name, prototype.flags);
		this.classType = type;
		this.returnType = prototype.returnType;
		this.paramTypes = prototype.paramTypes;
		this.code = prototype.code;
		if( this.paramTypes == null ) {
			this.paramTypes = new String[0];
		}
		this.paramAnnotations = new ArrayList<List<DexAnnotation>>();
		for( int i=0; i<this.paramTypes.length; i++ ) {
			this.paramAnnotations.add( new ArrayList<DexAnnotation>() );
		}
	}
	
	public DexMethod(DexMethod method, int flags, DexMethodBody code) {
		super(method.name, flags);
		this.returnType = method.returnType;
		this.paramTypes = method.paramTypes;
		this.code = code;
		if( this.paramTypes == null ) {
			this.paramTypes = new String[0];
		}
	}
	
	protected void visitAnnotations( DexVisitor visitor ) {
		super.visitAnnotations(visitor);
		int paramCount = 0;
		for( Iterator<List<DexAnnotation>> it = paramAnnotations.iterator(); it.hasNext(); ) {
			List<DexAnnotation> list = it.next();
			for( Iterator<DexAnnotation> sub = list.iterator(); sub.hasNext(); ) {
				visitor.visitParamAnnotation(paramCount, sub.next());
			}
			paramCount++;
		}		
	}
	
	public void visit( DexVisitor visitor ) {
		visitor.enterMethod(this);
		visitAnnotations(visitor);
		if(code != null) {
			visitor.visitMethodBody(code);
		}
		visitor.leaveMethod(this);
	}
	
	protected void setBody( DexMethodBody code ) { this.code = code; }
	
	protected void addParamAnnotations( int idx, DexAnnotation[]ann ) {
		List<DexAnnotation> param = this.paramAnnotations.get(idx);
		for( int i=0; i<ann.length; i++ ) {
			ann[i].setParent(this);
			param.add(ann[i]);
		}
	}

	public boolean isStatic() { return (flags & STATIC) != 0; }
	public String getReturnType() { return returnType; }
	public int getNumParamTypes() { return paramTypes.length; }
	public String getParamType(int idx) { return paramTypes[idx]; }
	public String getDescriptor() {
		StringBuffer buf = new StringBuffer("(");
		for( int i=0; i<paramTypes.length; i++ ) {
			buf.append(paramTypes[i]);
		}
		buf.append(")");
		buf.append(returnType);
		return buf.toString();
	}
	
	/**
	 * @return the number of parameter types at the actual call level, which
	 * includes the implicit this argument.
	 */
	public int getNumCallingParamTypes() {
		if( isStatic() )
			return paramTypes.length;
		else
			return paramTypes.length+1;
	}
	
	/**
	 * @return the parameter type at the actual call level specified by the
	 * given index. Parameter 0 is always 'this' for non-static methods.
	 */
	public String getCallingParamType( int idx ) {
		if( isStatic() ) {
			return paramTypes[idx];
		} else if( idx == 0 ) {
			return classType;
		} else {
			return paramTypes[idx-1];
		}
	}
	
	public DexMethodBody getBody() { return code; }
	public boolean hasBody() { return code != null; }
	
	public String getClassType() { return classType; }
	public String getInternalClassType() { return formatInternalName(classType); }
	
	public String[] getThrows() {
		return getAnnotationStringArray(DexAnnotation.DALVIK_THROWS, "value");
	}
	
	public String getDisplaySignature( ) {
		StringBuilder builder = new StringBuilder("(");
		builder.append(DexType.format(returnType));
		builder.append(")");
		builder.append(DexType.format(classType));
		builder.append(".");
		builder.append(getName());
		builder.append("(");
		for( int i=0; i<getNumParamTypes(); i++ ) {
			if( i != 0 )
				builder.append(",");
			builder.append(DexType.format(getParamType(i)));
		}
		builder.append(")");
		return builder.toString();
	}
	
	public String[] getInternalThrows() {
		String[] arr = getThrows();
		if( arr != null ) {
			for( int i=0; i<arr.length; i++ ) {
				arr[i] = formatInternalName(arr[i]);
			}
		}
		return arr;
	}
			
}
