package com.toccatasystems.dalvik;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.toccatasystems.util.ArrayIterator;

public class DexFile extends DexItem {

	private class MethodIterator implements Iterator<DexMethod> {
		int classIdx;
		int methodIdx;
		
		protected MethodIterator() {
			classIdx = 0;
			methodIdx = 0;
			skipToNext();
		}
		
		public boolean hasNext() {
			return classIdx < DexFile.this.classDefTable.length;
		}
		
		public DexMethod next() {
			DexClass clz = classDefTable[classIdx];
			DexMethod method = clz.getMethod(methodIdx);
			methodIdx++;
			skipToNext();
			return method;
		}

		public void remove() {
			throw new UnsupportedOperationException("Remove not supported");
		}
		
		private void skipToNext() {
			while( classIdx < DexFile.this.classDefTable.length ) {
				DexClass clz = classDefTable[classIdx];
				if( methodIdx < clz.getNumMethods() )
					return;
				classIdx++;
				methodIdx = 0;
			}
		}
	}
	
	private class MethodBodyIterator implements Iterator<DexMethodBody> {
		int classIdx;
		int methodIdx;
		
		protected MethodBodyIterator() {
			classIdx = 0;
			methodIdx = 0;
			skipToNext();
		}
		
		public boolean hasNext() {
			return classIdx < DexFile.this.classDefTable.length;
		}
		
		public DexMethodBody next() {
			DexClass clz = classDefTable[classIdx];
			DexMethod method = clz.getMethod(methodIdx);
			methodIdx++;
			skipToNext();
			return method.getBody();
		}

		public void remove() {
			throw new UnsupportedOperationException("Remove not supported");
		}
		
		private void skipToNext() {
			while( classIdx < DexFile.this.classDefTable.length ) {
				DexClass clz = classDefTable[classIdx];
				while( methodIdx < clz.getNumMethods() ) {
					if( clz.getMethod(methodIdx).hasBody() )
						return;
					methodIdx++;
				}
				classIdx++;
				methodIdx = 0;
			}
		}
		
	}
	
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
	
	/**
	 * Construct a filtered dex file that only contains the class specified.
	 * @param parent
	 * @param className class name in java format (java.lang.Foo)
	 */
	public DexFile( DexFile parent, String className ) {
		super(parent.getName(), 0);
		this.stringTable = parent.stringTable;
		this.typeNameTable = parent.typeNameTable;
		this.fieldTable = parent.fieldTable;
		this.methodTable = parent.methodTable;
		this.classLookupTable = new TreeMap<String,DexClass>();
		for( int i=0; i<parent.classDefTable.length; i++ ) {
			if( parent.getClass(i).getDisplayName().equals(className) ) {
				this.classDefTable = new DexClass[1];
				this.classDefTable[0] = parent.getClass(i);
				this.classLookupTable.put(this.classDefTable[0].getInternalName(), this.classDefTable[0]);
				break;
			}
		}
		if( this.classDefTable == null ) {
			this.classDefTable = new DexClass[0];
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
	
	public Iterator<DexClass> iterator() {
		return new ArrayIterator<DexClass>(classDefTable);
	}
	
	public Iterator<DexMethod> methodIterator() {
		return new MethodIterator();
	}
	
	public Iterator<DexMethodBody> methodBodyIterator() {
		return new MethodBodyIterator();
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
