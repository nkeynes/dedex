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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Class representing a generic value holder
 * @author nkeynes
 *
 */
public class DexValue {
	public final static int BYTE    = 0x00;
	public final static int SHORT   = 0x02;
	public final static int CHAR    = 0x03;
	public final static int INT     = 0x04;
	public final static int LONG    = 0x06;
	public final static int FLOAT   = 0x10;
	public final static int DOUBLE  = 0x11;
	public final static int STRING  = 0x17;
	public final static int TYPE    = 0x18;
	public final static int FIELD   = 0x19;
	public final static int METHOD  = 0x1a;
	public final static int ENUM    = 0x1b;
	public final static int ARRAY   = 0x1c;
	public final static int ANNOTATION = 0x1d;
	public final static int NULL    = 0x1e;
	public final static int BOOLEAN = 0x1f;
	
	private int type;
	private Object value;
	
	public DexValue(int type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public int getType() { return type; }
	public Object getValue() { return value; }
	
	/**
	 * Format the value as a string.
	 */
	public String toString() {
		if( value == null ) {
			return "null";
		} else if( type == ARRAY ) {
			DexValue[] arr = (DexValue[])value;
			return "{" + StringUtils.join(arr, ",") + "}";
		} else {
			return value.toString();
		}
	}

	/**
	 * Format the value for validity in Java source
	 * @return
	 */
	public String toLiteral() {
		if( value == null ) {
			return "null";
		}
		switch( type ) {
		case ARRAY:
			DexValue [] arr = (DexValue [])value;
			String[] str = new String[arr.length];
			for( int i=0; i<arr.length; i++ ) {
				str[i] = arr[i].toLiteral();
			}
			return "{" + StringUtils.join(str, ", ") + "}";
		case TYPE:
			return DexType.format(value.toString());
		case STRING:
			return "\"" + StringEscapeUtils.escapeJava(value.toString()) + "\"";
		case METHOD:
			return ((DexMethod)value).getDisplayName();
		case FIELD:
		case ENUM:
			return ((DexField)value).getDisplayName();
		default:
			return value.toString();
		}

	}
}
