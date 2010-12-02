package com.toccatasystems.dalvik;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DexAnnotation {
	
	public final static int VISIBILITY_NONE = -1;
	public final static int VISIBILITY_BUILD = 0;
	public final static int VISIBILITY_RUNTIME = 1;
	public final static int VISIBILITY_SYSTEM = 2;

	/* System-defined annotations */
	public final static String DALVIK_ANNOTATIONDEFAULT = "Ldalvik/annotation/AnnotationDefault;";
	public final static String DALVIK_ENCLOSINGCLASS    = "Ldalvik/annotation/EnclosingClass;";
	public final static String DALVIK_ENCLOSINGMETHOD   = "Ldalvik/annotation/EnclosingMethod;";
	public final static String DALVIK_INNERCLASS        = "Ldalvik/annotation/InnerClass;";
	public final static String DALVIK_MEMBERCLASSES     = "Ldalvik/annotation/MemberClasses;";
	public final static String DALVIK_SIGNATURE         = "Ldalvik/annotation/Signature;";
	public final static String DALVIK_THROWS            = "Ldalvik/annotation/Throws;";
	
	private String type;
	private int visibility;
	private Map<String,DexValue> elements;
	private DexItem parent;
	
	public DexAnnotation(String type, int visibility) {
		this.type = type;
		this.visibility = visibility;
		this.elements = new TreeMap<String,DexValue>();
	}
	
	protected void add( String name, DexValue value ) {
		elements.put(name, value);
	}
	
	public String getType() {
		return type;
	}
	
	public DexValue get( String name ) {
		return elements.get(name);
	}
	
	
	public String getString( String name ) {
		DexValue value = get(name);
		if( value != null && (value.getType() == DexValue.STRING || value.getType() == DexValue.TYPE) ) {
			return value.toString();
		}
		return null;
	}
	
	public DexValue[] getArray( String name ) {
		DexValue value = get(name);
		if( value != null && value.getType() == DexValue.ARRAY ) {
			return ((DexValue[])value.getValue());
		}
		return null;
	}
	
	public String[] getStringArray( String name ) {
		DexValue []arr = getArray(name);
		if( arr != null ) {
			String []str = new String[arr.length];
			for( int i=0; i<arr.length; i++ ) {
				if( arr[i].getType() == DexValue.STRING || arr[i].getType() == DexValue.TYPE ) {
					str[i] = arr[i].toString();
				} else {
					return null; /* Not an array of String */
				}
			}
			return str;
		}
		return null;
	}
	
	public Integer getInteger( String name ) {
		DexValue value = get(name);
		if( value != null && value.getType() == DexValue.INT ) {
			return (Integer)value.getValue();
		}
		return null;
	}
	
	public DexMethod getMethod( String name ) {
		DexValue value = get(name);
		if( value != null && value.getType() == DexValue.METHOD ) {
			return (DexMethod)value.getValue();
		}
		return null;
	}
	
	public int getVisibility() {
		return visibility;
	}
	
	public Map<String,DexValue> getMap() {
		return elements;
	}
	
	public Set<Map.Entry<String,DexValue>> entrySet() {
		return elements.entrySet();
	}
	
	public int size() {
		return elements.size();
	}

	protected void setParent( DexItem parent ) { this.parent = parent; }
	public DexItem getParent() { return parent; }
	
	public boolean isSystemAnnotation() {
		return type.startsWith("Ldalvik/annotation/");
	}
	
	public boolean isVisible() {
		return this.visibility >= VISIBILITY_RUNTIME;
	}
}
