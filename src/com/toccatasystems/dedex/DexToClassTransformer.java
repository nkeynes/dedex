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

package com.toccatasystems.dedex;

import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;


import com.toccatasystems.dalvik.DexAnnotation;
import com.toccatasystems.dalvik.DexClass;
import com.toccatasystems.dalvik.DexField;
import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexItem;
import com.toccatasystems.dalvik.DexMethod;
import com.toccatasystems.dalvik.DexMethodBody;
import com.toccatasystems.dalvik.DexValue;
import com.toccatasystems.dalvik.DexVisitor;

/**
 * @author nkeynes
 *
 */

public class DexToClassTransformer implements DexVisitor {
	private final static int IN_FILE = 0;
	private final static int IN_CLASS = 1;
	private final static int IN_FIELD = 2;
	private final static int IN_METHOD = 3;
	
	DexFile file;
	ClassOutputWriter output;
	ClassWriter writer;
	FieldVisitor fv;
	MethodVisitor mv;
	BytecodeTransformer bct;
	int state;
	boolean verifyBytecode = false;
	
	public DexToClassTransformer( ClassOutputWriter output, boolean verifyBytecode ) {
		this.output = output;
		this.bct = new BytecodeTransformer();
		this.verifyBytecode = verifyBytecode;
		state = IN_FILE;
	}
	
	public void enterFile(DexFile file) {
		state = IN_FILE;
		this.file = file;
		output.begin(file.getName(), file.getTimestamp());
	}

	public void enterClass(DexClass clz) {
		writer = new DexClassWriter(file, ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);
		writer.visit(49, clz.getFlags(), clz.getInternalName(), clz.getSignature(), clz.getInternalSuperName(),
				clz.getInternalInterfaces() );
		
		writer.visitSource(clz.getSourceFile(), null);
		
		DexMethod enclosingMethod = clz.getEnclosingMethod();
		String enclosingClass = clz.getInternalEnclosingClass();
		if( enclosingMethod != null ) {
			enclosingClass = enclosingMethod.getInternalClassType();
			writer.visitOuterClass(enclosingClass, 
					enclosingMethod.getName(), enclosingMethod.getDescriptor());
			writer.visitInnerClass(clz.getInternalName(), enclosingClass, 
					clz.getInnerClassName(), clz.getInnerClassFlags());
		} else if( enclosingClass != null ) {
			enclosingClass = DexItem.formatInternalName(enclosingClass);
			writer.visitInnerClass(clz.getInternalName(), enclosingClass, 
					clz.getInnerClassName(), clz.getInnerClassFlags());
		}
		
		String []innerClasses = clz.getMemberClasses();
		if( innerClasses != null ) {
			for( int i=0; i<innerClasses.length; i++ ) {
				String internalInner = DexItem.formatInternalName(innerClasses[i]);
				DexClass inner = file.getClass(internalInner);
				String innerName = null;
				int innerAccess = 0;
				if( inner != null ) {
					innerName = inner.getInnerClassName();
					innerAccess = inner.getInnerClassFlags();
				}
				writer.visitInnerClass(DexItem.formatInternalName(innerClasses[i]), clz.getInternalName(), innerName, innerAccess);
			}
		}
		state = IN_CLASS;
		
	}

	public void enterField(DexField field) {
		DexValue value = field.getInitializer();
		fv = writer.visitField(field.getFlags(), field.getName(), field.getType(), 
				field.getSignature(), value == null ? null : value.getValue());
		state = IN_FIELD;
	}
	
	public void visitAnnotationValue( AnnotationVisitor av, String name, DexValue value ) {
		AnnotationVisitor sub;
		switch( value.getType() ) {
		case DexValue.ANNOTATION:
			DexAnnotation ann = (DexAnnotation)value.getValue();
			sub = av.visitAnnotation(name, ann.getType());
			for( Iterator<Map.Entry<String,DexValue>> it = ann.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String,DexValue> ent = it.next();
				visitAnnotationValue( sub, ent.getKey(), ent.getValue());
			}
			sub.visitEnd();
			break;
		case DexValue.ENUM:
			DexField field = (DexField)value.getValue();
			av.visitEnum(name, field.getClassType(), field.getName());
			break;
		case DexValue.ARRAY:
			DexValue[] arr = (DexValue[])value.getValue();
			sub = av.visitArray(name);
			for( int i=0; i<arr.length; i++ ) {
				visitAnnotationValue( sub, name, arr[i] ); 
			}
			sub.visitEnd();
			break;
		default:
			av.visit(name, value.getValue());
		}
	}
	

	public void visitAnnotation(DexAnnotation annotation) {
		if( !annotation.isSystemAnnotation() ) {
			AnnotationVisitor av = null;
			String type = annotation.getType();
			switch( state ) {
			case IN_CLASS:
				av = writer.visitAnnotation(type, annotation.isVisible());
				break;
			case IN_FIELD:
				av = fv.visitAnnotation(type, annotation.isVisible());
				break;
			case IN_METHOD:
				av = mv.visitAnnotation(type, annotation.isVisible());
				break;
			}

			for( Iterator<Map.Entry<String,DexValue>> it = annotation.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String,DexValue> ent = it.next();
				visitAnnotationValue( av, ent.getKey(), ent.getValue());
			}
			av.visitEnd();
		}
	}

	public void leaveField(DexField field) {
		fv.visitEnd();
		state = IN_CLASS;
	}

	public void enterMethod(DexMethod method) {
		mv = writer.visitMethod( method.getFlags(), method.getName(), method.getDescriptor(), 
				method.getSignature(), method.getInternalThrows() );
		state = IN_METHOD;
		
	}

	public void visitParamAnnotation(int paramIndex, DexAnnotation annotation) {
		if( !annotation.isSystemAnnotation() ) {
			AnnotationVisitor av = mv.visitParameterAnnotation(paramIndex, annotation.getType(), annotation.isVisible());
			for( Iterator<Map.Entry<String,DexValue>> it = annotation.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<String,DexValue> ent = it.next();
				visitAnnotationValue( av, ent.getKey(), ent.getValue());
			}
			av.visitEnd();
		}
	}

	public void visitMethodBody(DexMethodBody body) {
		bct.transform(body, mv);
	}

	public void leaveMethod(DexMethod method) {
		mv.visitEnd();
		state = IN_CLASS;
	}

	public void leaveClass(DexClass clz) {
		writer.visitEnd();
		byte []classData = writer.toByteArray();
		if( verifyBytecode ) {
			verify(clz, classData);
		}
		state = IN_FILE;
		output.write(clz.getInternalName(), classData);
	}

	public void leaveFile(DexFile file) {
		output.end(file.getName());
	}

	private void verify( DexClass clz, byte[]data ) {
		ClassReader cr = new ClassReader(data);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_DEBUG);

        for (int i = 0; i < cn.methods.size(); ++i) {
            MethodNode method = (MethodNode) cn.methods.get(i);
            BasicVerifier verifier = new BasicVerifier();
            try {
            	Analyzer a = new Analyzer(verifier);
            	a.analyze(cn.name, method);
            } catch( Exception e ) {
            	clz.getMethod(i).getBody().disassemble(System.err);
            	throw new RuntimeException("Verification failure in " + clz.getDisplayName() + "." + method.name, e);
            }
        }
	}

}
