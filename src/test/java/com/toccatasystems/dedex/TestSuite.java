package com.toccatasystems.dedex;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.toccatasystems.dalvik.DexFile;
import com.toccatasystems.dalvik.DexParser;
import com.toccatasystems.dalvik.ParseException;
import com.toccatasystems.dedex.DedexCase;

/**
 * Entry point to the test suite. This is organized as a set of junit test cases
 * which are individually compiled, dexified, translated back, and then executed.
 * @author nkeynes
 *
 */

public class TestSuite {
	
	private final static String TEST_SOURCES = "/src/test/cases";
	private final static String TEST_CLASSES = "/target/test-cases";

	private int failCount = 0;
	
	@Test
	public void run() throws IOException, ParseException {
		Tools env = new Tools();
		String basedir = System.getProperty("basedir");
		File testSrcDir = new File(basedir + TEST_SOURCES);
		System.err.println( "src: " + testSrcDir );
		if( !testSrcDir.isDirectory() ) {
			basedir = System.getProperty("user.dir");
			testSrcDir = new File(basedir + TEST_SOURCES);
			System.err.println( "src: " + testSrcDir );
			if( !testSrcDir.isDirectory() ) {
				throw new RuntimeException("Unable to locate test cases");
			}
		}
		File testTargetDir = new File(basedir + TEST_CLASSES);
		testTargetDir.mkdir();
		List<DedexCase> cases = DedexCase.getCases(env, testSrcDir, testTargetDir);
		for( Iterator<DedexCase> it = cases.iterator(); it.hasNext(); ) {
			runTest(it.next());
		}
		if( failCount > 0 ) {
			throw new RuntimeException( Integer.toString(failCount) + " test cases failed!" );
		}
	}
	
	private void runTest( DedexCase test ) throws IOException, ParseException {
		String name = test.getName();
		System.out.print( name + "...");
		test.prepare();
		DexParser parser = new DexParser();
		DexFile dex = parser.parseFile(test.getDexFile());
		ClassLoaderWriter writer = new ClassLoaderWriter();
		DexToClassTransformer transform = new DexToClassTransformer(writer,true);
		dex.visit(transform);
		Result result = JUnitCore.runClasses(filterTests(writer.getClasses()));
		int total = result.getRunCount();
		int failed = result.getFailureCount();
		System.out.println( Integer.toString(total-failed) + "/" + total + " " +
				(result.wasSuccessful() ? "OK" : "Failed") );
		for( Iterator<Failure> it = result.getFailures().iterator(); it.hasNext(); ) {
			Failure fail = it.next();
			System.out.println( fail.toString() );
		}
		if( !result.wasSuccessful() ) {
			failCount++;
		}
	}
	
	/**
	 * Filter the class array to return only the classes that actually contain
	 * tests. Otherwise JUnit will complain about them and fail the tests.
	 * @param classes a list of classes
	 * @return a new array of testable classes.
	 */
	private Class<?>[] filterTests( Class<?>[] classes ) {
		List<Class<?>> result = new ArrayList<Class<?>>();
		for( int i=0; i<classes.length; i++ ) {
			if( hasTests(classes[i]) ) {
				result.add(classes[i]);
			}
		}
		Class<?>[] arr = new Class<?>[result.size()];
		result.toArray(arr);
		return arr;
	}
	
	/**
	 * Determine if the class has any methods marked with @Test, ie
	 * if it contains any JUnit tests.
	 * @param clz
	 * @return true if the class is testable.
	 */
	private boolean hasTests( Class<?> clz ) {
		Method[] methods = clz.getMethods();
		if( methods != null ) { 
			for( int i=0; i<methods.length; i++ ) {
				Annotation ann = methods[i].getAnnotation(Test.class);
				if( ann != null ) 
					return true;
			}
		}
		return false;
	}
	
	public static void main(String args[]) throws Exception {
		new TestSuite().run();
	}
}
