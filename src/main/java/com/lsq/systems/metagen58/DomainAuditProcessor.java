/*
 * @(#)DomainAuditProcessor.java Copyright 2011 LSQ Systems, Inc. All rights reserved.
 */
package com.lsq.systems.metagen58;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.persistence.Column;
import javax.tools.Diagnostic;
import javax.tools.FileObject;

import org.hibernate.jpamodelgen.util.TypeUtils;

/**
 * This class is used to create audit objects from regular entity objects.
 * 
 * @author Viet Trinh
 * @since 0.7
 */
@SupportedAnnotationTypes("com.lsq.systems.metagen58.Auditable")
@SupportedSourceVersion(RELEASE_6)
public class DomainAuditProcessor extends AbstractProcessor {

	public static final String SUFFIX = "Audit";

	// METHODS

	public void init(ProcessingEnvironment env) {
		super.init(env);
		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
				"Entity Audit Generator");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean process(final Set<? extends TypeElement> annotations,
			final RoundEnvironment roundEnvironment) {
		if (roundEnvironment.processingOver() || annotations.size() == 0)
			return false;

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for (Element element : elements) {
			if (TypeUtils.containsAnnotation(element, Auditable.class)) {
				processingEnv.getMessager().printMessage(Diagnostic.Kind.OTHER,
						"Processing annotated class " + element.toString());
				_write_audit_domain_class(element);
			}
		}
		return false;
	}

	/**
	 * Use this function to write the domain audit class directly from the given
	 * element.
	 * 
	 * @param element
	 *            The element to base the domain audit class from.
	 */
	private void _write_audit_domain_class(Element element) {
		try {
			String element_name = element.getSimpleName().toString();
			String audit_class_name = String.format("%s%s", element_name,
					SUFFIX);
			String package_name = AuditInterface.class.getPackage().getName();
			Filer filer = processingEnv.getFiler();
			FileObject fo = filer.createSourceFile(String.format("%s.%s",
					package_name, audit_class_name));
			OutputStream os = fo.openOutputStream();
			PrintWriter pw = new PrintWriter(os);

			// Use the string "+" for ease of reading, this does not need to run
			// fast.
			pw.println("package " + package_name + ";");
			pw.println();
			_generate_imports_domain(pw);
			pw.println();
			// Commented out to calm Spring down for now
			pw.println("@Entity");
			pw.println("@Table(name = \"" + audit_class_name + "\")");
			pw.println("@XmlRootElement(name = \"" + audit_class_name + "\")");
			pw.println("public class " + audit_class_name + " extends "
					+ element_name + " implements "
					+ AuditInterface.class.getSimpleName());
			pw.println("{");
			_generate_general_audit_code(pw, element_name);
			_generate_methods(pw, element);
			pw.println("}");
			pw.flush();
			pw.close();
		} catch (IOException e) {
			processingEnv
					.getMessager()
					.printMessage(
							Diagnostic.Kind.ERROR,
							String.format(
									"Problem opening file to write the %sAudit class file",
									element.getSimpleName()));
		} catch (ClassNotFoundException e) {
			processingEnv
					.getMessager()
					.printMessage(
							Diagnostic.Kind.ERROR,
							String.format(
									"Problem finding the given return type of a method to write the %sAudit class file",
									element.getSimpleName()));
		}
	}

	/**
	 * Use this function to generate the audit imports code for the domain
	 * object.
	 * 
	 * @param pw
	 *            The print writer to write to.
	 */
	private void _generate_imports_domain(PrintWriter pw) {
		pw.println("import java.util.Date;");
		pw.println("import javax.persistence.Basic;");
		pw.println("import javax.persistence.Column;");
		pw.println("import javax.persistence.Entity;");
		pw.println("import javax.persistence.GeneratedValue;");
		pw.println("import javax.persistence.Id;");
		pw.println("import javax.persistence.Table;");
		pw.println("import javax.persistence.Temporal;");
		pw.println("import javax.persistence.TemporalType;");
		pw.println("import javax.xml.bind.annotation.XmlRootElement;");
	}

	/**
	 * Use this function to generate the getter code.
	 * 
	 * @param pw
	 *            The print writer to write to.
	 * @param getter_variable
	 *            The getter variable to use. Remember the getter variable
	 *            should either underscore or camel cased.
	 * @param getter_type
	 *            The type of variable the getter is (Integer, Boolean, String,
	 *            etc.).
	 * @param optional
	 *            Set to <tt>true</tt> if the variable is optional,
	 *            <tt>false</tt> otherwise.
	 */
	private void _generate_getter(PrintWriter pw, String getter_variable,
			Class<?> getter_type, boolean optional) {
		String getter_underscore = toUnderscoreCase(getter_variable);

		pw.println("\t@Column(name = \"" + toCamelCase(getter_underscore)
				+ "\")");
		if ("Date".equals(getter_type.getSimpleName()))
			pw.println("\t@Temporal(TemporalType.TIMESTAMP)");
		if (optional == false)
			pw.println("\t@Basic(optional = false)");
		pw.println("\tpublic " + getter_type.getSimpleName() + " "
				+ toCamelCase("get_" + getter_underscore) + "()");
		pw.println("\t{");
		pw.println("\t\treturn _" + getter_underscore + ";");
		pw.println("\t}\n");
	}

	/**
	 * Use this function to generate the setter code.
	 * 
	 * @param pw
	 *            The print writer to write to.
	 * @param setter_variable
	 *            The setter variable to use. Remember the setter variable
	 *            should be underscore case (variable name) without the
	 *            beginning underscore.
	 * @param setter_type
	 *            The type of variable the setter is (Integer, Boolean, String,
	 *            etc.).
	 */
	private void _generate_setter(PrintWriter pw, String setter_variable,
			Class<?> setter_type) {
		String setter_underscore = toUnderscoreCase(setter_variable);

		pw.println("\tpublic void " + toCamelCase("set_" + setter_underscore)
				+ "(" + setter_type.getSimpleName() + " " + setter_underscore
				+ ")");
		pw.println("\t{");
		pw.println("\t\t_" + setter_underscore + " = " + setter_underscore
				+ ";");
		pw.println("\t}\n");
	}

	/**
	 * Use this function to generate the audit interface code.
	 * 
	 * @param pw
	 *            The print writer to write to.
	 * @param element_name
	 *            The element name we are going to create an audit for.
	 */
	private void _generate_general_audit_code(PrintWriter pw,
			String element_name) {
		pw.println("\tprivate Integer _" + element_name.toLowerCase()
				+ "_audit_id;");
		pw.println("\tprivate Date _audit_date;\n");
		pw.println("\t@Id");
		pw.println("\t@GeneratedValue");
		_generate_getter(pw, element_name.toLowerCase() + "_audit_id",
				Integer.class, true);
		_generate_setter(pw, element_name.toLowerCase() + "_audit_id",
				Integer.class);
		_generate_getter(pw, "audit_date", Date.class, false);
		_generate_setter(pw, "audit_date", Date.class);
	}

	/**
	 * Use this function to generate the audit methods.
	 * 
	 * @param pw
	 *            The print writer to write to.
	 * @param element
	 *            The element representing the class used to generate this audit
	 *            class.
	 * @throws ClassNotFoundException
	 */
	private void _generate_methods(PrintWriter pw, Element element)
			throws ClassNotFoundException {
		List<? extends Element> methods = ElementFilter.methodsIn(element
				.getEnclosedElements());
		for (Element method : methods) {
			if (TypeUtils.containsAnnotation(method, Column.class)) {
				Class<?> type_clazz = (Class<?>) Class.forName(method.asType()
						.toString().replaceAll("^\\(\\)", ""));
				_generate_getter(pw, method.getSimpleName().toString()
						.substring(3), type_clazz, true);
				_generate_setter(pw, method.getSimpleName().toString()
						.substring(3), type_clazz);
			}
		}
	}

	/**
	 * Use this function to capitalize the first letter of the string.
	 * 
	 * @param s
	 *            The string to capitalize first letter.
	 * @return A string with the first letter capitalized, <tt>null</tt> if no
	 *         string is provided.
	 */
	public static final String capitalizeFirstLetter(String s) {
		if (s == null)
			return null;
		else if (s.length() == 1)
			return s.toUpperCase();
		return String.format("%s%s", s.substring(0, 1).toUpperCase(),
				s.substring(1));
	}

	/**
	 * Use this function to turn the underscore string into a camel case string.
	 * 
	 * @param s
	 *            The underscore string to convert
	 * @return The converted camel case string.
	 */
	public static final String toCamelCase(String s) {
		if (s == null || s.indexOf('_') < 0)
			return s;
		String ss[] = s.replaceAll("^_", "").split("_");
		StringBuilder sb = new StringBuilder(s.length());
		sb.append(ss[0].toLowerCase());
		for (int i = 1; i < ss.length; i++)
			sb.append(capitalizeFirstLetter(ss[i]));
		return sb.toString();
	}

	/**
	 * Use this function to turn the camel case string into an underscore
	 * string.
	 * 
	 * @param s
	 *            The camel case string to convert
	 * @return The converted underscore string.
	 */
	public static final String toUnderscoreCase(String s) {
		if (s == null)
			return s;
		else if (s.indexOf('_') >= 0)
			return s.toLowerCase();

		Matcher m = Pattern.compile("([A-Z]?[a-z]+)").matcher(s);
		StringBuilder sb = new StringBuilder(s.length());
		while (m.find()) {
			sb.append(m.group());
			sb.append('_');
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString().toLowerCase();
	}

}
