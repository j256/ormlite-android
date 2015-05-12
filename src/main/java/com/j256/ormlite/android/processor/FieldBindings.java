package com.j256.ormlite.android.processor;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

class FieldBindings {
	private final String fullyQualifiedTypeName;
	private final String fieldName;
	private final DatabaseField databaseFieldAnnotation;
	private final ForeignCollectionField foreignCollectionFieldAnnotation;

	FieldBindings(String fullyQualifiedTypeName, String fieldName,
			DatabaseField databaseFieldAnnotation,
			ForeignCollectionField foreignCollectionFieldAnnotation) {
		this.fullyQualifiedTypeName = fullyQualifiedTypeName;
		this.fieldName = fieldName;
		this.databaseFieldAnnotation = databaseFieldAnnotation;
		this.foreignCollectionFieldAnnotation = foreignCollectionFieldAnnotation;
	}

	String getFullyQualifiedTypeName() {
		return fullyQualifiedTypeName;
	}

	String getFieldName() {
		return fieldName;
	}

	DatabaseField getDatabaseFieldAnnotation() {
		return databaseFieldAnnotation;
	}

	ForeignCollectionField getForeignCollectionFieldAnnotation() {
		return foreignCollectionFieldAnnotation;
	}
}
