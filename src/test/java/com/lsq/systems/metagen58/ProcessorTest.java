/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lsq.systems.metagen58;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Base class for annotation processor tests.
 * 
 * This code is from Hibernate's {@link JPAMetaModelEntityProcessor} tests.
 * https://github.com/hibernate/hibernate-metamodelgen
 * 
 * @author Hardy Ferentschik
 * @author Viet Trinh
 */
public class ProcessorTest {

	// CONSTANTS

	static final String _SOURCE_BASE_DIRECTORY = "src/main/java";
	static final String _OUTPUT_BASE_DIRECTORY = "target/test-classes";
	static final String _PATH_SEPARATOR = System.getProperty("file.separator");

	// FIELDS

	/**
	 * Diagnostics from the compilation.
	 */
	private List<Diagnostic<JavaFileObject>> _diagnostics;

	// METHODS

	/**
	 * Default constructor
	 */
	public ProcessorTest() {
		_diagnostics = new ArrayList<Diagnostic<JavaFileObject>>();
	}

	/**
	 * Sets up the test.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		List<File> source_files = getFiles(_SOURCE_BASE_DIRECTORY, Car.class
				.getPackage().getName());
		delete(new File(_OUTPUT_BASE_DIRECTORY));
		compile(source_files, Car.class.getPackage().getName());
	}

	/**
	 * Tears down the test.
	 * 
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		// Keep the files to see what was generated.
		// delete(new File(_OUTPUT_BASE_DIRECTORY));
	}

	/**
	 * Use this function to get all of the files to compile given the base
	 * directory and the package name.
	 * 
	 * @param base_dir
	 *            The base directory to start in.
	 * @param package_name
	 *            The package name to go to when looking for files to compile
	 * @return The list of files used for compilation.
	 */
	protected List<File> getFiles(String base_dir, String package_name) {
		List<File> javaFiles = new ArrayList<File>();
		String packageDirName = base_dir;
		if (package_name != null) {
			packageDirName = packageDirName + _PATH_SEPARATOR
					+ package_name.replace(".", _PATH_SEPARATOR);
		}

		File packageDir = new File(packageDirName);
		FilenameFilter javaFileFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".java") && !name.endsWith("Test.java")
						&& !name.contains("Audit");
			}
		};
		final File[] files = packageDir.listFiles(javaFileFilter);
		if (files == null) {
			throw new RuntimeException(
					"Cannot find package directory (is your base dir correct?): "
							+ packageDirName);
		}
		javaFiles.addAll(Arrays.asList(files));
		return javaFiles;
	}

	/**
	 * Use this function to get the set of diagnostics messages issued from the
	 * compilation of sources.
	 * 
	 * @return The list of diagnostics messages.
	 */
	public final List<Diagnostic<JavaFileObject>> getDiagnostics() {
		return _diagnostics;
	}

	/**
	 * Use this function to create the set of java options used the compile the
	 * file.
	 * 
	 * @return The set of java options used for compilation.
	 */
	private List<String> createJavaOptions() {
		List<String> options = new ArrayList<String>();
		options.add("-d");
		options.add(_OUTPUT_BASE_DIRECTORY);
		options.add("-processor");
		options.add(DomainAuditProcessor.class.getName() + ","
				+ JPAMetaModelEntityProcessor.class.getName());
		return options;
	}

	/**
	 * Compiles the specified Java classes and generated the meta model java
	 * files which in turn get also compiled.
	 * 
	 * @param sourceFiles
	 *            the files containing the java source files to compile.
	 * @param packageName
	 *            the package name of the source files
	 * 
	 * @throws Exception
	 *             in case the compilation fails
	 */
	@SuppressWarnings("unchecked")
	protected void compile(List<File> sourceFiles, String packageName)
			throws Exception {
		List<String> options = createJavaOptions();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager file_manager = compiler.getStandardFileManager(
				diagnostics, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = file_manager
				.getJavaFileObjectsFromFiles(sourceFiles);
		JavaCompiler.CompilationTask task = compiler.getTask(null,
				file_manager, diagnostics, options, null, compilationUnits);
		task.call();

		_diagnostics
				.addAll((Collection<? extends Diagnostic<JavaFileObject>>) diagnostics
						.getDiagnostics());

		file_manager.close();
	}

	/**
	 * Use this function to delete all of the generated source files from the
	 * given base path.
	 * 
	 * @param path
	 *            The base path to start looking.
	 */
	public void delete(File path) {
		if (path.exists()) {
			File[] files = path.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					if (file.isDirectory()) {
						return true;
					} else {
						return file.getAbsolutePath().endsWith("Audit.java")
								|| file.getAbsolutePath().endsWith(
										"Audit.class")
								|| file.getAbsolutePath().endsWith("_.java")
								|| file.getAbsolutePath().endsWith("_.class");
					}
				}
			});
			for (File file : files) {
				if (file.isDirectory())
					delete(file);
			}
		}
	}

	/**
	 * Asserts there was no compilation errors.
	 * 
	 * @param diagnostics
	 *            The compilation diagnostics to analyze.
	 */
	@Test
	public void assertNoCompilationError() {
		StringBuilder sb = new StringBuilder();
		for (Diagnostic<JavaFileObject> diagnostic : _diagnostics) {
			if (diagnostic.getKind().equals(Diagnostic.Kind.ERROR)) {
				sb.append(diagnostic.getMessage(null) + "\n");
			}
		}
		if (sb.length() > 0)
			fail("There were compilation error(s):\n" + sb.toString());
	}
}
