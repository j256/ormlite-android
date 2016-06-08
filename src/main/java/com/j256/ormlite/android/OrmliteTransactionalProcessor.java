package com.j256.ormlite.android;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Annotation processor that automatically writes the Android entity config file.
 * 
 * @author graywatson
 */
public class OrmliteTransactionalProcessor implements Processor {

	private static final Set<Class<? extends Annotation>> annotationSet = new HashSet<Class<? extends Annotation>>();
	private ProcessingEnvironment processingEnv;

	static {
		annotationSet.add(DatabaseTable.class);
		annotationSet.add(DatabaseField.class);
	}

	@Override
	public Set<String> getSupportedOptions() {
		return Collections.emptySet();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> typeSet = new HashSet<String>();
		for (Class<? extends Annotation> clazz : annotationSet) {
			typeSet.add(clazz.getName());
		}
		return typeSet;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.RELEASE_6;
	}

	@Override
	public void init(ProcessingEnvironment processingEnv) {
		this.processingEnv = processingEnv;
	}

	@Override
	public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation,
			ExecutableElement member, String userText) {
		return Collections.emptyList();
	}

	@Override
	public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
		for (Class<? extends Annotation> annotationClazz : annotationSet) {
			for (Element element : roundEnvironment.getElementsAnnotatedWith(annotationClazz)) {
				// if (element.getKind() == ElementKind.CLASS) {
				// if (annotationClazz == DatabaseTable.class) {
				// System.out.println("Element " + element.getSimpleName() + " has annotation " + annotationClazz);
				// }
				// }
				System.out.println("-- Element " + element + " has annotation " + annotationClazz);
				processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, element + " error");
			}
		}
		return true;
	}
}
