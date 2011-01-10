package com.toccatasystems.dedex;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DedexCase {
	protected Tools env;
	protected File srcDir;
	protected File targetDir;
	protected File []sourceFiles;
	protected File []classFiles;
	protected File dexFile;
	protected File jarFile;
	protected long sourceTimestamp;
	
	public DedexCase( Tools env, File srcDir, File targetDir ) {
		this.env = env;
		this.srcDir = srcDir;
		this.targetDir = targetDir;
		this.sourceFiles = getJavaFiles(srcDir);
		this.dexFile = new File(srcDir.toString() + ".dex");
		this.jarFile = new File(targetDir.toString() + ".jar");
		this.sourceTimestamp = calculateSourceTimestamp();
	}
	
	public static List<DedexCase> getCases( Tools env, File testSrcDir, File testTargetDir ) {
		List<DedexCase> cases = new ArrayList<DedexCase>();
		File[] files = testSrcDir.listFiles();
		for( int i=0; i<files.length; i++ ) {
			if( files[i].isDirectory() ) {
				File targetDir = new File( testTargetDir, files[i].getName() );
				DedexCase testCase = new DedexCase(env, files[i], targetDir);
				if(!testCase.isEmpty()) {
					cases.add(testCase);
				}
			}
		}
		return cases;
	}
	
	public String getName() {
		return srcDir.getName();
	}
	
	public File getDexFile() {
		return dexFile;
	}
	
	public File getJarFile() {
		return jarFile;
	}
	
	public boolean isEmpty() {
		return sourceFiles.length == 0;
	}
	
	/**
	 * Prepare the test case by ensure the dexFile for the test is up-to-date,
	 * and building it if not.
	 */
	public void prepare() throws IOException {
		if( !isDexCurrent() ) {
			targetDir.mkdir();
			env.compile(targetDir, sourceFiles);
			classFiles = getClassFiles(targetDir);
			env.dex(dexFile, targetDir);
		}
	}
	
	/**
	 * Determine the most recent timestamp on all of the source files.
	 * @return
	 */
	private long calculateSourceTimestamp() {
		long timestamp = 0;
		for( int i=0; i<sourceFiles.length; i++ ) {
			long tmp = sourceFiles[i].lastModified();
			if( tmp > timestamp )
				timestamp = tmp;
		}
		return timestamp;
	}
	
	private boolean isDexCurrent() {
		return this.dexFile.canRead() && dexFile.lastModified() >= sourceTimestamp;
	}
		
	private File[] getJavaFiles(File dir) {
		return getFilesWithExtension(dir, ".java");
	}

	private File[] getClassFiles( File dir ) {
		return getFilesWithExtension(dir, ".class");
	}
	
	private File[] getFilesWithExtension(File parentDir, final String ext) {
		File[] result = parentDir.listFiles(new FileFilter() {
			public boolean accept( File f ) {
				return f.canRead() && f.getName().endsWith(ext);
			}
		});
		if( result == null ) {
			result = new File[0];
		}
		return result;
	}
}
