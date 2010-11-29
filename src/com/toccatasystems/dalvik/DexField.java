package com.toccatasystems.dalvik;

public class DexField extends DexItem {
	private String type;
	private DexValue initValue;
	
	public DexField(String name, String type, int flags) {
		super(name, flags);
		this.type = type;
	}
	
	public DexField(DexField field, int flags) {
		super(field.name, flags);
		this.type = field.type;
	}

	public String getType() { return type; }
	
	protected void setInitializer( DexValue init ) { this.initValue = init; }
	
	public DexValue getInitializer( ) { return initValue; }
	
	public boolean hasInitializer( ) { return initValue != null; }
	
	public void visit( DexVisitor visitor ) {
		visitor.enterField( this );
		visitAnnotations(visitor);
		visitor.leaveField(this);
	}
}
