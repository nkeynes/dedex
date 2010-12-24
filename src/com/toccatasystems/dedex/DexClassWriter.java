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

import org.objectweb.asm.ClassWriter;

import com.toccatasystems.dalvik.DexFile;

/**
 * Extension of ClassWriter to lookup classes in the dex file information,
 * as well as just assume worst case if we can't find the class.
 * @author nkeynes
 *
 */
public class DexClassWriter extends ClassWriter {

	private DexFile file;
	
	public DexClassWriter(DexFile file, int flags) {
		super(flags);
		this.file = file;
	}
	
	/**
	 * Find the least-common-ancestor of two class types.
	 */
	protected String getCommonSuperClass(String type1, String type2) {
		try {
			return super.getCommonSuperClass(type1, type2);
		} catch( Exception e ) {
			return "java/lang/Object";
		}
	}
	
}
