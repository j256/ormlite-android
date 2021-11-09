package com.j256.ormlite.android.processor;

import java.util.ArrayList;
import java.util.List;

class TableBindings {
	private final ParsedClassName parsedClassName;
	private final String tableName;
	private final List<FieldBindings> fields = new ArrayList<FieldBindings>();

	TableBindings(ParsedClassName parsedClassName, String tableName) {
		this.parsedClassName = parsedClassName;
		this.tableName = tableName;
	}

	void addField(FieldBindings field) {
		fields.add(field);
	}

	ParsedClassName getParsedClassName() {
		return parsedClassName;
	}

	String getTableName() {
		return tableName;
	}

	List<FieldBindings> getFields() {
		return fields;
	}
}
