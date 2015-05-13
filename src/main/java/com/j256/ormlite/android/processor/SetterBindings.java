package com.j256.ormlite.android.processor;

class SetterBindings {
	private final String format;
	private final Object parameter;

	SetterBindings(String format, Object parameter) {
		this.format = format;
		this.parameter = parameter;
	}

	String getFormat() {
		return format;
	}

	Object getParameter() {
		return parameter;
	}
}
