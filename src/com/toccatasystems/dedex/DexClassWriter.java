package com.toccatasystems.dedex;

import org.objectweb.asm.ClassWriter;

import com.toccatasystems.dalvik.DexClass;
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
