package com.j256.ormlite.android.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

/*
 * Eclipse doesn't like the link to rt.jar and classes therein. This is a
 * spurious warning that can be ignored. It is intended to prevent referencing
 * com.sun packages that may not be in every JVM, but the annotation processing
 * stuff is part of JSR-269, so will always be present.
 */
@SuppressWarnings("restriction")
class ParsedClassName {
	private String packageName;
	private List<String> nestedClasses = new ArrayList<String>();

	ParsedClassName(Element element) {
		Element elementIterator = element;
		do {
			nestedClasses.add(elementIterator.getSimpleName().toString());
			elementIterator = elementIterator.getEnclosingElement();
		} while (elementIterator.getKind().isClass());
		Collections.reverse(nestedClasses);
		packageName = ((PackageElement) elementIterator).getQualifiedName()
				.toString();
	}

	String getPackageName() {
		return packageName;
	}

	String getInputFullyQualifiedClassName() {
		StringBuilder sb = new StringBuilder();
		if (!packageName.isEmpty()) {
			sb.append(packageName);
			sb.append('.');
		}
		for (int i = 0; i < nestedClasses.size(); ++i) {
			if (i != 0) {
				sb.append('.');
			}
			sb.append(nestedClasses.get(i));
		}
		return sb.toString();
	}

	String getInputSimpleClassName() {
		return nestedClasses.get(nestedClasses.size() - 1);
	}

	String getGeneratedClassName() {
		final String SUFFIX = "_TableConfig";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nestedClasses.size(); ++i) {
			if (i != 0) {
				sb.append('_');
			}
			sb.append(nestedClasses.get(i));
		}
		sb.append(SUFFIX);
		return sb.toString();
	}

	String getGeneratedFullyQualifiedClassName() {
		StringBuilder sb = new StringBuilder();
		if (!packageName.isEmpty()) {
			sb.append(packageName);
			sb.append('.');
		}
		sb.append(getGeneratedClassName());
		return sb.toString();
	}
}
