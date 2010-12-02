package com.toccatasystems.dalvik;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Base class for fields and methods to hold common functionality.
 * @author nkeynes
 *
 */
public class DexItem {
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
	
	
	protected DexItem parent;
	protected List<DexAnnotation> annotations;
	protected String name;
	protected int flags;
	
	protected DexItem( String name, int flags ) {
		this.parent = null;
		this.name = name;
		this.flags = flags;
		this.annotations = new ArrayList<DexAnnotation>();
	}
	
	protected void setParent( DexItem item ) { this.parent = item; }
	public DexItem getParent() { return parent; }
	public DexFile getFile() {
		if( parent == null ) {
			return null;
		} else {
			return parent.getFile();
		}
	}
	
	public String getName() { return name; }
	
	public String getDisplayName() { return formatTypeName(name); }

	protected void setFlags( int flags ) { this.flags = flags; }
	protected void add( DexAnnotation ann[] ) {
		for( int i=0; i<ann.length; i++ ) {
			add(ann[i]);
		}
	}
	protected void add( DexAnnotation ann ) {
		ann.setParent(this);
		annotations.add(ann);
	}
	
	public String toString() { return name; }
	
	public int getFlags() { return flags; }
	
	public String getFlagsString( ) {
		String result = "";
		for( int i=0; i < 13; i++ ) {
			if( i != 10 && (flags & (1<<i)) != 0 ) {
				result += FLAG_NAMES[i] + " "; 
			}
		}
		return result;
	}
	
	public int getAnnotationCount() { return annotations.size(); }
	
	public DexAnnotation getAnnotation( int idx ) { return annotations.get(idx); }
	
	public boolean hasAnnotation( String type ) {
		return getAnnotation(type) != null;
	}
	
	public DexAnnotation getAnnotation( String type ) {
		for( Iterator<DexAnnotation> it = annotations.iterator(); it.hasNext(); ) {
			DexAnnotation ann = it.next();
			if( ann.getType().equals(type) ) {
				return ann;
			}
		}
		return null;
	}
	
	public DexValue getAnnotationValue( String type, String field ) {
		DexAnnotation ann = getAnnotation(type);
		if( ann != null ) {
			return ann.get(field);
		}
		return null;
	}
	
	public String getAnnotationString( String type, String field ) {
		DexAnnotation ann = getAnnotation(type);
		if( ann != null ) {
			return ann.getString(field);
		}
		return null;
	}
	
	public Integer getAnnotationInteger( String type, String field ) {
		DexAnnotation ann = getAnnotation(type);
		if( ann != null ) {
			return ann.getInteger(field);
		}
		return null;
	}
	
	public DexMethod getAnnotationMethod( String type, String field ) {
		DexAnnotation ann = getAnnotation(type);
		if( ann != null ) {
			return ann.getMethod(field);
		}
		return null;
	}

	public String[] getAnnotationStringArray( String type, String field ) {
		DexAnnotation ann = getAnnotation(type);
		if( ann != null ) {
			return ann.getStringArray(field);
		}
		return null;
	}
	
	public List<DexAnnotation> getAnnotations() { return annotations; }
	
	protected void visitAnnotations( DexVisitor visitor ) {
		for( Iterator<DexAnnotation> it = annotations.iterator(); it.hasNext(); ) {
			visitor.visitAnnotation(it.next());
		}
	}
	
	/** Extract the signature annotation */
	public String getSignature() {
		String []signature = getAnnotationStringArray(DexAnnotation.DALVIK_SIGNATURE, "value");
		if( signature != null ) {
			return StringUtils.join(signature);
		}
		return null;
	}
	
	/**
	 * If typeName represents a class, in the form Lorg/some/class;,
	 * returns the "internal name" which is "org/some/class"
	 * @param typeName
	 * @return
	 */
	public static String formatInternalName( String typeName ) {
		int idx = typeName.indexOf(';');
		if( typeName.charAt(0) == 'L' && idx != -1 ) {
			return typeName.substring(1,idx);
		} else {
			return typeName;
		}
	}
		
	/**
	 * Returns the human readable (java source) version of the given type name.
	 * @param typeName
	 * @return
	 */
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
}
