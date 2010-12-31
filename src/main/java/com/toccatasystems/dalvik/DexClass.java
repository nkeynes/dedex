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
	
	public String getSuperName() {
		return superclass;
	}
	
	public DexClass getSuperclass() {
		if( superclass == null )
			return null;
		else
			return getFile().getClass(formatInternalName(superclass));
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
		return getAnnotationString(DexAnnotation.DALVIK_INNERCLASS, "name" );
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
	
	public boolean isAssignableTo( DexClass other ) {
		if( other.name.equals(name) )
			return true;
		for( int i=0; i<interfaces.length; i++ ) {
			if( interfaces[i].equals(other.name) )
				return true;
		}
		if( superclass != null ) {
			DexClass superclz = getSuperclass();
			if( superclz != null ) {
				return superclz.isAssignableTo(other);
			}
			if( superclass.equals(other.name) )
				return true;
		}
		return false;
	}
	
	public boolean isAssignableFrom( DexClass other ) {
		return other.isAssignableTo(this);
	}
	
	public boolean isAssignableTo( Class<?> other ) {
		String otherName = other.getName();
		if( getInternalName().equals(otherName) ) 
			return true; /* Unlikely, but just in case */
		for( int i=0; i<interfaces.length; i++ ) {
			if( formatInternalName(interfaces[i]).equals(otherName) )
				return true;
		}
		if( superclass != null ) {
			DexClass superclz = getSuperclass();
			if( superclz != null ) {
				return superclz.isAssignableTo(other);
			}
			if( getInternalSuperName().equals(otherName) )
				return true;
			try {
				Class<?> clz = Class.forName(getInternalSuperName().replace('/', '.'));
				return other.isAssignableFrom(clz);
			} catch( ClassNotFoundException e ) {
			}
		}
		return false;
	}
	
	/**
	 * @return The first non-dex superclass, if one can be found. Otherwise null.
	 */
	public Class<?> getJavaSuperclass() {
		if( superclass == null )
			return null;
		DexClass superclz = getSuperclass();
		if( superclz != null )
			return superclz.getJavaSuperclass();
		try {
			return Class.forName(getInternalSuperName().replace('/', '.'));
		} catch( ClassNotFoundException e ) {
			return null;
		}
	}
		
}
