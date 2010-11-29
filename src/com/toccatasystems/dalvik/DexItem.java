package com.toccatasystems.dalvik;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for fields and methods to hold common functionality.
 * @author nkeynes
 *
 */
public class DexItem {
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
	
	public String getName() { return name; }

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
	
	public int getAnnotationCount() { return annotations.size(); }
	
	public DexAnnotation getAnnotation( int idx ) { return annotations.get(idx); }
	
	public DexAnnotation getAnnotation( String type ) {
		for( Iterator<DexAnnotation> it = annotations.iterator(); it.hasNext(); ) {
			DexAnnotation ann = it.next();
			if( ann.getType() == type ) {
				return ann;
			}
		}
		return null;
	}
	
	public List<DexAnnotation> getAnnotations() { return annotations; }
	
	protected void visitAnnotations( DexVisitor visitor ) {
		for( Iterator<DexAnnotation> it = annotations.iterator(); it.hasNext(); ) {
			visitor.visitAnnotation(it.next());
		}
	}
}
