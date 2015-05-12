package com.j256.ormlite.android.processor;

import java.util.ArrayList;
import java.util.List;

import com.j256.ormlite.table.DatabaseTable;

class TableBindings {
	private final ParsedClassName parsedClassName;
	private final DatabaseTable annotation;
	private final List<FieldBindings> fields = new ArrayList<FieldBindings>();

	TableBindings(ParsedClassName parsedClassName, DatabaseTable annotation) {
		this.parsedClassName = parsedClassName;
		this.annotation = annotation;
	}

	void addField(FieldBindings field) {
		fields.add(field);
	}

	ParsedClassName getParsedClassName() {
		return parsedClassName;
	}

	DatabaseTable getAnnotation() {
		return annotation;
	}

	List<FieldBindings> getFields() {
		return fields;
	}
}
