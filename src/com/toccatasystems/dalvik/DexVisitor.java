package com.toccatasystems.dalvik;

public interface DexVisitor {
	public void enterFile( DexFile file );
	public void enterClass( DexClass clz );
	public void enterField( DexField field );
	public void visitAnnotation( DexAnnotation annotation );
	public void leaveField( DexField field );
	public void enterMethod( DexMethod method );
	public void visitParamAnnotation( int paramIndex, DexAnnotation annotation );
	public void visitMethodBody( DexMethodBody body );
	public void leaveMethod( DexMethod method );
	public void leaveClass( DexClass clz );
	public void leaveFile( DexFile file );
}
