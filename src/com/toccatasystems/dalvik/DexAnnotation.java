package com.toccatasystems.dalvik;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DexAnnotation {
	
	public final static int VISIBILITY_NONE = -1;
	public final static int VISIBILITY_BUILD = 0;
	public final static int VISIBILTY_RUNTIME = 1;
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
}
