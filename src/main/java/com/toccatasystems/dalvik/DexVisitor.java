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
