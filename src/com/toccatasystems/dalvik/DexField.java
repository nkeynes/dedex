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

public class DexField extends DexItem {
	private String type;
	private String classType;
	private DexValue initValue;
	
	public DexField(String classType, String name, String type, int flags) {
		super(name, flags);
		this.type = type;
		this.classType = classType;
	}
	
	public DexField(DexField field, int flags) {
		super(field.name, flags);
		this.type = field.type;
		this.classType = field.classType;
	}

	public String getType() { return type; }
	
	public String getClassType() { return classType; }
	
	public String getInternalClassType() { return formatInternalName(classType); }
	
	public String getDisplayName( ) {
		return DexType.format(classType) + "." + getName();
	}
	
	protected void setInitializer( DexValue init ) { this.initValue = init; }
	
	public DexValue getInitializer( ) { return initValue; }
	
	public boolean hasInitializer( ) { return initValue != null; }
	
	public void visit( DexVisitor visitor ) {
		visitor.enterField( this );
		visitAnnotations(visitor);
		visitor.leaveField(this);
	}
}
