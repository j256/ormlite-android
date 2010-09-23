package com.j256.ormlite.android;

import android.util.Log;

import com.j256.ormlite.logger.LoggerFactory;

/**
 * Implementation of our logger which delegates to the internal Android logger.
 * 
 * @author graywatson
 */
public class AndroidLog implements com.j256.ormlite.logger.Log {

	private String className;

	public AndroidLog(String className) {
		// get the last part of the class name
		this.className = LoggerFactory.getSimpleClassName(className);
	}

	public boolean isTraceEnabled() {
		return Log.isLoggable(className, Log.VERBOSE);
	}

	public boolean isDebugEnabled() {
		return Log.isLoggable(className, Log.DEBUG);
	}

	public boolean isInfoEnabled() {
		return Log.isLoggable(className, Log.INFO);
	}

	public boolean isWarnEnabled() {
		return Log.isLoggable(className, Log.WARN);
	}

	public boolean isErrorEnabled() {
		return Log.isLoggable(className, Log.ERROR);
	}

	public boolean isFatalEnabled() {
		return Log.isLoggable(className, Log.ERROR);
	}

	public void trace(String message) {
		Log.v(className, message);
	}

	public void trace(String message, Throwable t) {
		Log.v(className, message, t);
	}

	public void debug(String message) {
		Log.d(className, message);
	}

	public void debug(String message, Throwable t) {
		Log.d(className, message, t);
	}

	public void info(String message) {
		Log.i(className, message);
	}

	public void info(String message, Throwable t) {
		Log.i(className, message, t);
	}

	public void warn(String message) {
		Log.w(className, message);
	}

	public void warn(String message, Throwable t) {
		Log.w(className, message, t);
	}

	public void error(String message) {
		Log.e(className, message);
	}

	public void error(String message, Throwable t) {
		Log.e(className, message, t);
	}

	public void fatal(String message) {
		Log.e(className, message);
	}

	public void fatal(String message, Throwable t) {
		Log.e(className, message, t);
	}
}
