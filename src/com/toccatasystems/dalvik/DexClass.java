package com.toccatasystems.dalvik;

public class DexClass extends DexItem {
	public String superclass;
	public String []interfaces;
	String sourceFile;
	public DexField[] staticFields;
	public DexField[] instanceFields;
	public DexMethod[] directMethods;
	public DexMethod[] virtualMethods;
	
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
}
