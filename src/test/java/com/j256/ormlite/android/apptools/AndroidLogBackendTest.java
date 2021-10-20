package com.j256.ormlite.android.apptools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.j256.ormlite.logger.LogBackendFactory;
import com.j256.ormlite.logger.LogBackendType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.logger.LoggerFactory;

public class AndroidLogBackendTest {

	@Test
	public void testEnowork() {
		LogBackendFactory factory = LoggerFactory.getLogBackendFactory();
		try {
			LoggerFactory.setLogBackendFactory(LogBackendType.ANDROID);
			Logger logger = LoggerFactory.getLogger(getClass());
			logger.trace("hello");
			assertEquals(LogBackendType.ANDROID, LoggerFactory.getLogBackendFactory());
		} finally {
			LoggerFactory.setLogBackendFactory(factory);
		}
	}
}
