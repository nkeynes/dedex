package com.toccatasystems.dalvik;

public class DexFile extends DexItem {

	private String []stringTable;
	private String []typeNameTable;
	private DexField []fieldTable;
	private DexMethod []methodTable;
	
	private DexClass []classDefTable;
	
	public DexFile( String filename, String []stringTable, String []typeNameTable,
			DexField[] fieldTable, DexMethod []methodTable, DexClass[] classDefTable ) {
		super(filename, 0);
		this.stringTable = stringTable;
		this.typeNameTable = typeNameTable;
		this.fieldTable = fieldTable;
		this.methodTable = methodTable;
		this.classDefTable = classDefTable;
		for( int i=0; i<this.classDefTable.length; i++ ) {
			this.classDefTable[i].setParent(this);
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
		return formatTypeName(getTypeName(idx));
	}
	
	public String getDisplayFieldName( int idx ) {
		DexField field = getField(idx);
		return formatTypeName(field.getClassType()) + "." + field.getName();
	}
	
	public String getDisplayMethodSignature( int idx ) {
		return formatMethodSignature(getMethod(idx));
	}
		
	
	public static String formatTypeName( String typeName ) {
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
	
	public static String formatMethodSignature( DexMethod method ) {
		StringBuilder builder = new StringBuilder("(");
		builder.append(formatTypeName(method.getReturnType()));
		builder.append(")");
		builder.append(formatTypeName(method.getClassType()));
		builder.append(".");
		builder.append(method.getName());
		builder.append("(");
		for( int i=0; i<method.getNumParamTypes(); i++ ) {
			if( i != 0 )
				builder.append(",");
			builder.append(formatTypeName(method.getParamType(i)));
		}
		builder.append(")");
		return builder.toString();
	}
	
}
