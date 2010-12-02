package com.toccatasystems.dedex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * A ClassOutputWriter that writes multiple .dex input files into individual
 * .jar files. 
 * 
 * @author nkeynes
 *
 */
public class MultiJarClassWriter implements ClassOutputWriter {

	JarOutputStream jar;
	String filename;
	boolean failure;
	
	public MultiJarClassWriter( ) {
		jar = null;
		failure = false;
	}
	
	public boolean hasFailure() {
		return failure;
	}

	public void begin( String filename ) {
		File f = new File(filename);
		String jarFile = f.getName();
		if( jarFile.endsWith(".dex") ) {
			jarFile = jarFile.substring(0,jarFile.length()-4) + ".jar";
		} else {
			jarFile = jarFile + ".jar";
		}
		this.filename = jarFile;
		
		try {
			jar = new JarOutputStream(new FileOutputStream(jarFile));
		} catch( IOException e ) {
			jar = null;
			System.err.println( "Error creating jar file '" + filename + "': " + e.getMessage() );
			failure = true;
		}
	}
	
	public void end( String filename ) { 
		try {
			if( jar != null )
				jar.close();
			jar = null;
		} catch( IOException e ) {
			System.err.println( "Error finalizing jar '" + filename + "': " + e.getMessage() );
			failure = true;
		}		
	}
	
	public void close() {
		/* Nothing */
	}
	
	public void write(String internalClassName, byte[] classData) {
		try {
			if( jar != null ) {
				jar.putNextEntry( new JarEntry(internalClassName + ".class") );
				jar.write(classData);
				jar.closeEntry();
			}
		} catch( IOException e ) {
			if( !failure ) {
				failure = true;
				System.err.println( "Error writing to jar '" + filename + "': " + e.getMessage() );
			}
		}
	}

}
