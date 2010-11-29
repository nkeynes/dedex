package com.toccatasystems.dalvik;

public class DexAccessFlags {

	public final static int PUBLIC = 0x01;
	public final static int PRIVATE = 0x02;
	public final static int PROTECTED = 0x04;
	public final static int STATIC = 0x08;
	public final static int FINAL = 0x10;
	public final static int SYNCHRONIZED = 0x20;
	public final static int VOLAILTE = 0x40;
	public final static int TRANSIENT = 0x80;
	public final static int NATIVE = 0x100;
	public final static int INTERFACE = 0x200;
	public final static int ABSTRACT = 0x400;
	public final static int STRICT = 0x800;
	public final static int SYNTHETIC = 0x1000;
	public final static int ANNOTATION = 0x2000;
	public final static int ENUM = 0x4000;
	public final static int CONSTRUCTOR = 0x10000;
	
	public final static String []FLAG_NAMES = {
		"public", "private", "protected", "static", "final", "synchronized", "volatile",
		"transient", "native", "interface", "abstract", "strict", "synthetic", "annotation",
		"enum", null, "constructor" };
	
	public static String toString( int flags ) {
		String result = "";
		for( int i=0; i < 13; i++ ) {
			if( i != 10 && (flags & (1<<i)) != 0 ) {
				result += FLAG_NAMES[i] + " "; 
			}
		}
		return result;
	}
	
	public static String getClassKind( int flags ) {
		if( (flags & INTERFACE) != 0 ) 
			return "interface";
		else if( (flags & ANNOTATION) != 0 )
			return "annotation";
		else if( (flags & ENUM) != 0 )
			return "enum";
		else return "class";
	}
}
