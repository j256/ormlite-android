package com.j256.ormlite.android.processor;

import java.util.List;

class FieldBindings {
	private final String fieldName;
	private final List<SetterBindings> setters;

	FieldBindings(String fieldName, List<SetterBindings> setters) {
		this.fieldName = fieldName;
		this.setters = setters;
	}

	String getFieldName() {
		return fieldName;
	}

	List<SetterBindings> getSetters() {
		return setters;
	}
}
