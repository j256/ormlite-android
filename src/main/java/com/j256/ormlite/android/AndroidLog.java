package com.j256.ormlite.android;

import android.util.Log;

import com.j256.ormlite.logger.LoggerFactory;

/**
 * Implementation of our logger which delegates to the internal Android logger.
 * 
 * @author graywatson
 */
public class AndroidLog implements com.j256.ormlite.logger.Log {

	private final static int MAX_TAG_LENGTH = 23;
	private String className;

	public AndroidLog(String className) {
		// get the last part of the class name
		this.className = LoggerFactory.getSimpleClassName(className);
		// make sure that our tag length is not too long
		int length = this.className.length();
		if (length > MAX_TAG_LENGTH) {
			this.className = this.className.substring(length - MAX_TAG_LENGTH, length);
		}
	}

	public boolean isLevelEnabled(Level level) {
		return Log.isLoggable(className, levelToJavaLevel(level));
	}

	public void log(Level level, String msg) {
		switch (level) {
			case TRACE :
				Log.v(className, msg);
				break;
			case DEBUG :
				Log.d(className, msg);
				break;
			case INFO :
				Log.i(className, msg);
				break;
			case WARNING :
				Log.w(className, msg);
				break;
			case ERROR :
				Log.e(className, msg);
				break;
			case FATAL :
				Log.e(className, msg);
				break;
			default :
				Log.i(className, msg);
				break;
		}
	}

	public void log(Level level, String msg, Throwable t) {
		switch (level) {
			case TRACE :
				Log.v(className, msg, t);
				break;
			case DEBUG :
				Log.d(className, msg, t);
				break;
			case INFO :
				Log.i(className, msg, t);
				break;
			case WARNING :
				Log.w(className, msg, t);
				break;
			case ERROR :
				Log.e(className, msg, t);
				break;
			case FATAL :
				Log.e(className, msg, t);
				break;
			default :
				Log.i(className, msg, t);
				break;
		}
	}

	private int levelToJavaLevel(com.j256.ormlite.logger.Log.Level level) {
		switch (level) {
			case TRACE :
				return Log.VERBOSE;
			case DEBUG :
				return Log.DEBUG;
			case INFO :
				return Log.INFO;
			case WARNING :
				return Log.WARN;
			case ERROR :
				return Log.ERROR;
			case FATAL :
				return Log.ERROR;
			default :
				return Log.INFO;
		}
	}
}
