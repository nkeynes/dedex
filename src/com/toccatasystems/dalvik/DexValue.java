package com.toccatasystems.dalvik;

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
	
}
