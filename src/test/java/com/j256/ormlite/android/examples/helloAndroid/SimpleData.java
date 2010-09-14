package com.j256.ormlite.android.examples.helloAndroid;

import java.util.Date;

import com.j256.ormlite.field.DatabaseField;

/**
 * A simple demonstration object we are creating.
 */
public class SimpleData {

	@DatabaseField(generatedId = true)
	int id;
	@DatabaseField
	String string;
	@DatabaseField
	long millis;
	@DatabaseField
	Date date;
	@DatabaseField
	boolean even;

	SimpleData() {
		// needed by ormlite
	}

	public SimpleData(long millis) {
		this.date = new Date(millis);
		this.string = "millis = " + millis;
		this.millis = millis;
		this.even = ((millis % 2) == 0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id=").append(id).append(", ");
		sb.append("string=").append(string).append(", ");
		sb.append("millis=").append(millis).append(", ");
		sb.append("date=").append(date).append(", ");
		sb.append("even=").append(even).append(", ");
		return sb.toString();
	}
}
