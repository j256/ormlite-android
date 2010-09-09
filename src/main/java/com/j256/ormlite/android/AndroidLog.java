package com.j256.ormlite.android;

import java.lang.reflect.Method;

import com.j256.ormlite.logger.Log;
import com.j256.ormlite.logger.LoggerFactory;

/**
 * Implementation of our logger which delegates to the internal Android logger via reflection. We use reflection so we
 * can avoid the dependency.
 * 
 * @author graywatson
 */
public class AndroidLog implements Log {

	private String className;

	private static Method isLoggableMethod;
	private static int VERBOSE;
	private static int DEBUG;
	private static int INFO;
	private static int WARN;
	private static int ERROR;

	private static Method vMethod;
	private static Method vThrowableMethod;
	private static Method dMethod;
	private static Method dThrowableMethod;
	private static Method iMethod;
	private static Method iThrowableMethod;
	private static Method wMethod;
	private static Method wThrowableMethod;
	private static Method eMethod;
	private static Method eThrowableMethod;

	public AndroidLog(String className) {
		// get the last part of the class name
		this.className = LoggerFactory.getSimpleClassName(className);
		if (isLoggableMethod == null) {
			findMethods();
		}
	}

	public boolean isTraceEnabled() {
		try {
			return (Boolean) isLoggableMethod.invoke(null, className, VERBOSE);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isDebugEnabled() {
		return isLevelEnabled(DEBUG);
	}

	public boolean isInfoEnabled() {
		return isLevelEnabled(INFO);
	}

	public boolean isWarnEnabled() {
		return isLevelEnabled(WARN);
	}

	public boolean isErrorEnabled() {
		return isLevelEnabled(ERROR);
	}

	public boolean isFatalEnabled() {
		return isLevelEnabled(ERROR);
	}

	public void trace(String message) {
		logMessage(vMethod, message);
	}

	public void trace(String message, Throwable t) {
		logMessage(vThrowableMethod, message, t);
	}

	public void debug(String message) {
		logMessage(dMethod, message);
	}

	public void debug(String message, Throwable t) {
		logMessage(dThrowableMethod, message, t);
	}

	public void info(String message) {
		logMessage(iMethod, message);
	}

	public void info(String message, Throwable t) {
		logMessage(iThrowableMethod, message, t);
	}

	public void warn(String message) {
		logMessage(wMethod, message);
	}

	public void warn(String message, Throwable t) {
		logMessage(wThrowableMethod, message, t);
	}

	public void error(String message) {
		logMessage(eMethod, message);
	}

	public void error(String message, Throwable t) {
		logMessage(eThrowableMethod, message, t);
	}

	public void fatal(String message) {
		logMessage(eMethod, message);
	}

	public void fatal(String message, Throwable t) {
		logMessage(eThrowableMethod, message, t);
	}

	private static void findMethods() {
		Class<?> clazz;
		try {
			clazz = Class.forName("android.util.Log");
		} catch (ClassNotFoundException e) {
			// oh well, fail
			return;
		}

		isLoggableMethod = getMethod(clazz, "isLoggable", String.class, int.class);
		VERBOSE = (Integer) getIntField(clazz, "VERBOSE");
		DEBUG = (Integer) getIntField(clazz, "DEBUG");
		INFO = (Integer) getIntField(clazz, "INFO");
		WARN = (Integer) getIntField(clazz, "WARN");
		ERROR = (Integer) getIntField(clazz, "ERROR");

		vMethod = getMethod(clazz, "v", String.class, String.class);
		vThrowableMethod = getMethod(clazz, "v", String.class, String.class, Throwable.class);
		dMethod = getMethod(clazz, "d", String.class, String.class);
		dThrowableMethod = getMethod(clazz, "d", String.class, String.class, Throwable.class);
		iMethod = getMethod(clazz, "i", String.class, String.class);
		iThrowableMethod = getMethod(clazz, "i", String.class, String.class, Throwable.class);
		wMethod = getMethod(clazz, "w", String.class, String.class);
		wThrowableMethod = getMethod(clazz, "w", String.class, String.class, Throwable.class);
		eMethod = getMethod(clazz, "e", String.class, String.class);
		eThrowableMethod = getMethod(clazz, "e", String.class, String.class, Throwable.class);
	}

	private boolean isLevelEnabled(int level) {
		if (isLoggableMethod == null) {
			return false;
		} else {
			try {
				return (Boolean) isLoggableMethod.invoke(null, className, level);
			} catch (Exception e) {
				return false;
			}
		}
	}

	private void logMessage(Method method, String message) {
		if (method != null) {
			try {
				method.invoke(null, className, message);
			} catch (Exception e) {
				// oh well, drop the message
			}
		}
	}

	private void logMessage(Method method, String message, Throwable t) {
		if (method != null) {
			try {
				method.invoke(null, className, message, t);
			} catch (Exception e) {
				// oh well, drop the message
			}
		}
	}

	private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		try {
			return clazz.getMethod(methodName, parameterTypes);
		} catch (Exception e) {
			return null;
		}
	}

	private static int getIntField(Class<?> clazz, String fieldName) {
		try {
			return (Integer) clazz.getField(fieldName).get(null);
		} catch (Exception e) {
			return 0;
		}
	}
}
