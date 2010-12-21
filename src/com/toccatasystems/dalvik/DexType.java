package com.toccatasystems.dalvik;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DexType implements Comparable<DexType> {

	public final static DexType BYTE = new DexType("B");
	public final static DexType CHAR = new DexType("C");
	public final static DexType DOUBLE = new DexType("D"); 
	public final static DexType FLOAT = new DexType("F"); 
	public final static DexType INT = new DexType("I"); 
	public final static DexType LONG = new DexType("J"); 
	public final static DexType SHORT = new DexType("S"); 
	public final static DexType VOID = new DexType("V"); 
	public final static DexType BOOLEAN = new DexType("Z");
	public final static DexType OBJECT = new DexType("Ljava/lang/Object;");
	public final static DexType CLASS = new DexType("Ljava/lang/Class;");
	public final static DexType STRING = new DexType("Ljava/lang/String;");
	public final static DexType ABYTE = new DexType("[B");
	public final static DexType ACHAR = new DexType("[C");
	public final static DexType ASHORT = new DexType("[S");
	public final static DexType AINT = new DexType("[I");
	public final static DexType ALONG = new DexType("[J");
	public final static DexType ABOOLEAN = new DexType("[Z");
	public final static DexType AOBJECT = new DexType("[Ljava/lang/Object;");
	
	
	
	/** The const* bytecodes are polymorphic, so represent them as the
	 * special types below until they can be flattened to a real type.
	 */
	public final static DexType CONST32 = new DexType("const32");
	public final static DexType CONST64 = new DexType("const64");
	
	public final static DexType THROWABLE = new DexType("Ljava/lang/Throwable;");

	public final static DexType EXCEPTION = new DexType("Ljava/lang/Exception;");
	public final static DexType CLASS_NOT_FOUND_EXCEPTION = new DexType("Ljava/lang/ClassNotFoundException;");

	public final static DexType RUNTIME_EXCEPTION = new DexType("Ljava/lang/RuntimeException;");
	public final static DexType NULL_POINTER_EXCEPTION = new DexType("Ljava/lang/NullPointerException;");
	public final static DexType ARRAY_STORE_EXCEPTION = new DexType("Ljava/lang/ArrayStoreException;");
	public final static DexType INDEX_OUT_OF_BOUNDS_EXCEPTION = new DexType("Ljava/lang/IndexOutOfBoundsException;");
	public final static DexType ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION = new DexType("Ljava/lang/ArrayIndexOutOfBoundsException;");
	public final static DexType STRING_INDEX_OUT_OF_BOUNDS_EXCEPTION = new DexType("Ljava/lang/StringIndexOutOfBoundsException;");
	public final static DexType CLASS_CAST_EXCEPTION = new DexType("Ljava/lang/ClassCastException;");
	public final static DexType ARITHMETIC_EXCEPTION = new DexType("Ljava/lang/ArithmeticException;");
	public final static DexType SECURITY_EXCEPTION = new DexType("Ljava/lang/SecurityException;");
	public final static DexType TYPE_NOT_PRESENT_EXCEPTION = new DexType("Ljava/lang/TypeNotPresentException;");
	public final static DexType UNSUPPORTED_OPERATION_EXCEPTION = new DexType("Ljava/lang/UnsupportedOperationException;");
	public final static DexType ILLEGAL_ARGUMENT_EXCEPTION = new DexType("Ljava/lang/IllegalArgumentException;");
	public final static DexType NUMBER_FORMAT_EXCEPTION = new DexType("Ljava/lang/NumberFormatException;");
	public final static DexType ILLEGAL_ACCESS_EXCEPTION = new DexType("Ljava/lang/IllegalAccessException;");
	public final static DexType ILLEGAL_MONITOR_STATE_EXCEPTION = new DexType("Ljava/lang/IllegalMonitorStateException;");
	public final static DexType ILLEGAL_STATE_EXCEPTION = new DexType("Ljava/lang/IllegalStateException;");
	public final static DexType ILLEGAL_THREAD_STATE_EXCEPTION = new DexType("Ljava/lang/IllegalThreadStateException;");
	public final static DexType INCOMPLETE_ANNOTATION_EXCEPTION = new DexType("Ljava/lang/annotation/IncompleteAnnotationException;");
	public final static DexType NO_SUCH_FIELD_EXCEPTION = new DexType("Ljava/lang/NoSuchFieldException;");
	public final static DexType NO_SUCH_METHOD_EXCEPTION = new DexType("Ljava/lang/NoSuchMethodException;");
	public final static DexType NEGATIVE_ARRAY_SIZE_EXCEPTION = new DexType("Ljava/lang/NegativeArraySizeException;");
	
	public final static DexType SERIALIZABLE = new DexType("Ljava/io/Serializable;");

	public final static DexType ERROR = new DexType("Ljava/lang/Error;");

	
	/**
	 * Map from Class Type to its (transitively complete) set of supertypes.
	 */
	private final static Map<DexType, Set<DexType>> SYSTEM_SUPERTYPE_TABLE;

	/**
	 * Add a new type to the supertype table. Note that this must be constructed 
	 * from the top down (ie java.lang.Object first)
	 * @param map
	 * @param type
	 * @param parentType
	 * @param interfaces
	 */
	private final static void add( Map<DexType, Set<DexType>> map, DexType type, DexType parentType, DexType []interfaces ) {
		Set<DexType> types = new TreeSet<DexType>();
		if(parentType != null ) {
			types.add(parentType);
			if( map.containsKey(parentType) ) {
				types.addAll( map.get(parentType) );
			}
		}
		if( interfaces != null ) {
			for( int i=0; i<interfaces.length; i++ ) {
				types.add(interfaces[i]);
				if( map.containsKey(interfaces[i]) ) {
					types.addAll( map.get(interfaces[i]) );
				}
			}
		}
	}
	
	private final static void add( Map<DexType, Set<DexType>> map, DexType type, DexType parentType ) {
		add( map, type, parentType, (DexType [])null );
	}
	
	private final static void add( Map<DexType, Set<DexType>> map, DexType type, DexType parentType, DexType singleIface ) {
		DexType[] arr = new DexType[1];
		arr[0] = singleIface;
		add( map, type, parentType, arr );
	}
	

		/**
	 * Construct the super-type table for the system classes.
	 */
	static {
		Map<DexType, Set<DexType>> map = new HashMap<DexType, Set<DexType>>();
		add( map, OBJECT, null );
		add( map, SERIALIZABLE, null );
		add( map, THROWABLE, OBJECT, SERIALIZABLE );
		add( map, ERROR, THROWABLE );
		add( map, EXCEPTION, THROWABLE );
		add( map, CLASS_NOT_FOUND_EXCEPTION, EXCEPTION );
		add( map, ILLEGAL_ACCESS_EXCEPTION, EXCEPTION );
		add( map, NO_SUCH_FIELD_EXCEPTION, EXCEPTION );
		add( map, NO_SUCH_METHOD_EXCEPTION, EXCEPTION );
		add( map, RUNTIME_EXCEPTION, EXCEPTION );
		add( map, ARITHMETIC_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, ARRAY_STORE_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, CLASS_CAST_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, ILLEGAL_ARGUMENT_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, ILLEGAL_THREAD_STATE_EXCEPTION, ILLEGAL_ARGUMENT_EXCEPTION );
		add( map, INDEX_OUT_OF_BOUNDS_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION, INDEX_OUT_OF_BOUNDS_EXCEPTION );
		add( map, ILLEGAL_MONITOR_STATE_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, ILLEGAL_STATE_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, STRING_INDEX_OUT_OF_BOUNDS_EXCEPTION, INDEX_OUT_OF_BOUNDS_EXCEPTION );
		add( map, INCOMPLETE_ANNOTATION_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, NEGATIVE_ARRAY_SIZE_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, SECURITY_EXCEPTION, RUNTIME_EXCEPTION ); 
		add( map, TYPE_NOT_PRESENT_EXCEPTION, RUNTIME_EXCEPTION );
		add( map, UNSUPPORTED_OPERATION_EXCEPTION, RUNTIME_EXCEPTION );
		SYSTEM_SUPERTYPE_TABLE = map;
	}		
	
	private String name;
	
	public DexType(String name) {
		this.name = name.intern();
	}	
	
	public String getName() {
		return name;
	}
	
	public String getInternalName() {
		return DexItem.formatInternalName(name);
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
	
	public String format() {
		return Integer.toHexString(super.hashCode()) + ":" + format(name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public boolean isPrimitive() {
		return name.length() == 1;
	}
	
	public boolean isPolymorphic() {
		return name.startsWith("const");
	}
	
	public boolean isArray() {
		return name.startsWith("[");
	}
	
	public boolean isObject() {
		return name.startsWith("L") || name.startsWith("[");
	}
	
	public boolean equals( Object o ) {
		return (o instanceof DexType) && equals((DexType)o);
	}
	
	public boolean equals( DexType o ) {
		return name == o.name;
	}
	
	/**
	 * @return true if the type is a primitive that fits in a single 32-bit word. 
	 */
	public boolean isPrimWord() {
		return name == "B" || name == "C" || name == "S" || name == "I" || name == "Z" || name == "F" || name == "const32";
	}
	
	public boolean isPrimDWord() {
		return name == "D" || name == "J" || name == "const64";
	}
	
	public DexType getElementType() {
		if( name.startsWith("[") ) {
			return new DexType(name.substring(1));
		} else {
			return null;
		}
	}

	/**
	 * Return the element type of an array type. If the element type cannot be
	 * determined, returns defaultType instead.
	 */
	public DexType getElementType( String defaultType ) {
		if( name.startsWith("[") ) {
			return new DexType(name.substring(1));
		} else {
			return new DexType(defaultType);
		}
	}

	/**
	 * Return the element type of an array type. If the element type cannot be
	 * determined, returns defaultType instead.
	 */
	public DexType getElementType( DexType defaultType ) {
		if( name.startsWith("[") ) {
			return new DexType(name.substring(1));
		} else {
			return defaultType;
		}
	}

	/**
	 * Returns the human readable (java source) version of the given type name.
	 * @param typeName
	 * @return
	 */
	public static String format( String typeName ) {
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
		case '[': return format( typeName.substring(1)) + "[]";
		default: return typeName;
		}
	}

	@Override
	public int compareTo(DexType o) {
		return name.compareTo(o.name);
	}
	
	public boolean isSubtypeOf( DexType supertype ) {
		Set<DexType> superset = SYSTEM_SUPERTYPE_TABLE.get(this);
		return superset != null && superset.contains(supertype);
	}
	
}
