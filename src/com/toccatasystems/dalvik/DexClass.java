package com.toccatasystems.dalvik;

public class DexClass extends DexItem {
	private String superclass;
	private String []interfaces;
	private String sourceFile;
	private DexField[] staticFields;
	private DexField[] instanceFields;
	private DexMethod[] directMethods;
	private DexMethod[] virtualMethods;
	
	public DexClass( String name, int flags, String superclass,
			String []interfaces, String sourceFile,
			DexField[] staticFields, DexField[] instanceFields,
			DexMethod[] directMethods, DexMethod[] virtualMethods) {
		super(name, flags);
		this.superclass = superclass;
		this.interfaces = interfaces;
		this.sourceFile = sourceFile;
		this.staticFields = staticFields;
		this.instanceFields = instanceFields;
		this.directMethods = directMethods;
		this.virtualMethods = virtualMethods;

		if( this.interfaces == null ) {
			this.interfaces = new String[0];
		}
		for( int i=0; i<staticFields.length; i++ ) {
			staticFields[i].setParent(this);
		}
		for( int i=0; i<instanceFields.length; i++ ) {
			instanceFields[i].setParent(this);
		}
		for( int i=0; i<directMethods.length; i++ ) {
			directMethods[i].setParent(this);
		}
		for( int i=0; i<virtualMethods.length; i++ ) {
			virtualMethods[i].setParent(this);
		}
	}
	
	public void visit( DexVisitor visitor ) {
		visitor.enterClass(this);
		visitAnnotations(visitor);
		for( int i=0; i<staticFields.length; i++ ) {
			staticFields[i].visit(visitor);
		}
		for( int i=0; i<instanceFields.length; i++ ) {
			instanceFields[i].visit(visitor);
		}
		for( int i=0; i<directMethods.length; i++ ) {
			directMethods[i].visit(visitor);
		}
		for( int i=0; i<virtualMethods.length; i++ ) {
			virtualMethods[i].visit(visitor);
		}
		visitor.leaveClass(this);
	}
	
	public String getSuperclass() {
		return superclass;
	}
	
	public String[] getInterfaces() {
		return interfaces;
	}
	
	public String getSourceFile() {
		return sourceFile;
	}
	
	public String getInternalName() {
		return formatInternalName(name);
	}
	
	public String getInternalSuperName() {
		return formatInternalName(superclass);
	}
	
	public String[] getInternalInterfaces() {
		if( interfaces == null ) {
			return null;
		} else {
			String []result = new String[interfaces.length];
			for( int i=0; i<interfaces.length; i++ ) {
				result[i] = formatInternalName(interfaces[i]);
			}
			return result;
		}
	}
			
	
	/**
	 * Return the class kind, which may be one of "interface", "annotation",
	 * "enum", or "class", depending on the class flags.
	 * @param flags
	 * @return
	 */
	public String getKind( ) {
		if( (flags & INTERFACE) != 0 ) 
			return "interface";
		else if( (flags & ANNOTATION) != 0 )
			return "annotation";
		else if( (flags & ENUM) != 0 )
			return "enum";
		else return "class";
	}
	
	public String getEnclosingClass( ) {
		return getAnnotationString(DexAnnotation.DALVIK_ENCLOSINGCLASS, "value");
	}
	
	public String getInternalEnclosingClass( ) {
		String str = getEnclosingClass();
		if( str == null ) {
			return null;
		} else {
			return formatInternalName(str);
		}
	}
	
	public DexMethod getEnclosingMethod( ) {
		return getAnnotationMethod(DexAnnotation.DALVIK_ENCLOSINGMETHOD, "value");
	}
	
	public String[] getMemberClasses( ) {
		return getAnnotationStringArray(DexAnnotation.DALVIK_MEMBERCLASSES, "value");
	}
	
	public String getInnerClassName( ) {
		return getAnnotationString(DexAnnotation.DALVIK_INNERCLASS, "value" );
	}
	
	public int getInnerClassFlags( ) {
		Integer i = getAnnotationInteger(DexAnnotation.DALVIK_INNERCLASS, "accessFlags" );
		return i == null ? 0 : i.intValue();
	}
	
	public boolean isInnerClass( ) {
		return hasAnnotation(DexAnnotation.DALVIK_INNERCLASS);
	}
	
	public int getNumMethods() {
		return directMethods.length + virtualMethods.length;
	}
	
	public DexMethod getMethod( int idx ) {
		if( idx < directMethods.length ) {
			return directMethods[idx];
		} else {
			return virtualMethods[idx - directMethods.length];
		}
	}
}
