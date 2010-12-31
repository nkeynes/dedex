package com.toccatasystems.dedex;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassOutputWriter that 
 * @author nkeynes
 *
 */
public class ClassLoaderWriter extends ClassLoader implements ClassOutputWriter {

	List<Class<?>> classes = new ArrayList<Class<?>>();
	
	@Override
	public void begin(String filename, long timestamp) {
	}

	@Override
	public void write(String internalClassName, byte[] classData) {
		Class<?> clz = defineClass(internalClassName.replace('/','.'), classData, 0, classData.length);
		classes.add(clz);
	}

	@Override
	public void end(String filename) {
	}

	@Override
	public void close() {
	}

	public Class<?>[] getClasses() {
		Class<?>[] arr = new Class<?>[classes.size()];
		classes.toArray(arr);
		return arr;
	}
}
