package com.toccatasystems.dalvik;

import java.util.Map;
import java.util.TreeMap;

public class DexFile extends DexItem {

	private String []stringTable;
	private String []typeNameTable;
	private DexField []fieldTable;
	private DexMethod []methodTable;
	
	private DexClass []classDefTable;
	
	private Map<String,DexClass> classLookupTable;
	
	public DexFile( String filename, String []stringTable, String []typeNameTable,
			DexField[] fieldTable, DexMethod []methodTable, DexClass[] classDefTable ) {
		super(filename, 0);
		this.stringTable = stringTable;
		this.typeNameTable = typeNameTable;
		this.fieldTable = fieldTable;
		this.methodTable = methodTable;
		this.classDefTable = classDefTable;
		this.classLookupTable = new TreeMap<String,DexClass>();
		for( int i=0; i<this.classDefTable.length; i++ ) {
			this.classDefTable[i].setParent(this);
			classLookupTable.put(classDefTable[i].getInternalName(), classDefTable[i]);
		}
	}

	public int getNumStrings() {
		return stringTable.length;
	}
	
	public String getString(int idx) {
		return stringTable[idx];
	}
	
	public int getNumTypeNames() {
		return typeNameTable.length;
	}
	
	public String getTypeName(int idx) {
		return typeNameTable[idx];
	}
	
	public int getNumFields() {
		return fieldTable.length;
	}
	
	public DexField getField(int idx) {
		return fieldTable[idx];
	}
	
	public int getNumMethods() {
		return methodTable.length;
	}
	
	public DexMethod getMethod(int idx) {
		return methodTable[idx];
	}
	
	public int getNumClasses() {
		return classDefTable.length;
	}

	public DexClass getClass(int idx) {
		return classDefTable[idx];
	}
	
	public DexClass getClass( String internalName ) {
		return classLookupTable.get(internalName);
	}
	
	public void visit(DexVisitor visitor) {
		visitor.enterFile(this);
		for( int i=0; i<classDefTable.length; i++ ) {
			classDefTable[i].visit(visitor);
		}
		visitor.leaveFile(this);
	}
	
	public DexFile getFile() {
		return this;
	}
	
	public String getDisplayTypeName( int idx ) {
		return DexType.format(getTypeName(idx));
	}

}
