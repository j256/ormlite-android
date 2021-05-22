package com.j256.ormlite.android;

import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.LogBackend;
import com.j256.ormlite.logger.LogBackendFactory;
import com.j256.ormlite.logger.LoggerFactory;

import android.util.Log;

/**
 * Log backend that delegates to the internal Android logger.
 * 
 * <p>
 * To see log messages you will do something like:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.YourActivity VERBOSE
 * </pre>
 * 
 * <p>
 * <b>NOTE:</b> Unfortunately, Android variables are limited in size so this class takes that last 23 (sic) characters
 * of the class name if it is larger than 23 characters. For example, if the class is AndroidDatabaseConnection you
 * would do:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.droidDatabaseConnection VERBOSE
 * </pre>
 * 
 * <p>
 * To see all log messages use:
 * </p>
 * 
 * <pre>
 * adb shell setprop log.tag.simplelogging DEBUG
 * </pre>
 * 
 * @author graywatson
 */
public class AndroidLogBackend implements LogBackend {

	private final static String ALL_LOGS_NAME = "ORMLite";
	// check to see if the android level has changed every so often
	private final static int REFRESH_LEVEL_CACHE_EVERY = 200;

	private final static int MAX_TAG_LENGTH = 23;
	private final String className;

	// we do this because supposedly Log.isLoggable() always does IO
	private volatile int levelCacheC = 0;
	private final boolean levelCache[];

	public AndroidLogBackend(String className) {
		// get the last part of the class name
		String simpleName = LoggerFactory.getSimpleClassName(className);
		// make sure that our tag length is not too long
		int length = simpleName.length();
		if (length > MAX_TAG_LENGTH) {
			simpleName = className.substring(length - MAX_TAG_LENGTH, length);
		}
		this.className = simpleName;
		// find the maximum level value
		int maxLevel = 0;
		for (Level level : Level.values()) {
			int androidLevel = levelToAndroidLevel(level);
			if (androidLevel > maxLevel) {
				maxLevel = androidLevel;
			}
		}
		levelCache = new boolean[maxLevel + 1];
		refreshLevelCache();
	}

	@Override
	public boolean isLevelEnabled(Level level) {
		// we don't care if this is not synchronized, it will be updated sooner or later and multiple updates are fine.
		if (++levelCacheC >= REFRESH_LEVEL_CACHE_EVERY) {
			refreshLevelCache();
			levelCacheC = 0;
		}
		int androidLevel = levelToAndroidLevel(level);
		if (androidLevel < levelCache.length) {
			return levelCache[androidLevel];
		} else {
			return isLevelEnabledInternal(androidLevel);
		}
	}

	@Override
	public void log(Level level, String msg) {
		switch (level) {
			case TRACE:
				Log.v(className, msg);
				break;
			case DEBUG:
				Log.d(className, msg);
				break;
			/* INFO below */
			case WARNING:
				Log.w(className, msg);
				break;
			case ERROR:
				Log.e(className, msg);
				break;
			case FATAL:
				// no fatal level
				Log.e(className, msg);
				break;
			case INFO:
			default:
				Log.i(className, msg);
				break;
		}
	}

	@Override
	public void log(Level level, String msg, Throwable t) {
		switch (level) {
			case TRACE:
				Log.v(className, msg, t);
				break;
			case DEBUG:
				Log.d(className, msg, t);
				break;
			/* INFO below */
			case WARNING:
				Log.w(className, msg, t);
				break;
			case ERROR:
				Log.e(className, msg, t);
				break;
			case FATAL:
				// no level higher than e
				Log.e(className, msg, t);
				break;
			case INFO:
			default:
				Log.i(className, msg, t);
				break;
		}
	}

	private void refreshLevelCache() {
		for (Level level : Level.values()) {
			int androidLevel = levelToAndroidLevel(level);
			if (androidLevel < levelCache.length) {
				levelCache[androidLevel] = isLevelEnabledInternal(androidLevel);
			}
		}
	}

	private boolean isLevelEnabledInternal(int androidLevel) {
		// this is supposedly expensive with an IO operation for each call so we cache them into levelCache[]
		return Log.isLoggable(className, androidLevel) || Log.isLoggable(ALL_LOGS_NAME, androidLevel);
	}

	private int levelToAndroidLevel(Level level) {
		switch (level) {
			case TRACE:
				return Log.VERBOSE;
			case DEBUG:
				return Log.DEBUG;
			/* INFO below */
			case WARNING:
				return Log.WARN;
			case ERROR:
				return Log.ERROR;
			case FATAL:
				return Log.ERROR;
			case INFO:
			default:
				return Log.INFO;
		}
	}

	/**
	 * Factory for generating AndroidLogBackend instances.
	 */
	public static class AndroidLogBackendFactory implements LogBackendFactory {
		@Override
		public LogBackend createLogBackend(String classLabel) {
			return new AndroidLogBackend(classLabel);
		}
	}
}
