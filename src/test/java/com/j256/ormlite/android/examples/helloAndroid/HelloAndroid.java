package com.j256.ormlite.android.examples.helloAndroid;

import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager.SqliteOpenHelperFactory;
import com.j256.ormlite.dao.Dao;

public class HelloAndroid extends OrmLiteBaseActivity<DatabaseHelper> {

	private final String LOG_TAG = getClass().getSimpleName();

	static {
		OpenHelperManager.setOpenHelperFactory(new SqliteOpenHelperFactory() {
			public OrmLiteSqliteOpenHelper getHelper(Context context) {
				return new DatabaseHelper(context);
			}
		});
	}

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "creating " + getClass());
		TextView tv = new TextView(this);
		doSampleDatabaseStuff("onCreate", tv);
		setContentView(tv);
	}

	/**
	 * Do our sample database stuff.
	 */
	private void doSampleDatabaseStuff(String action, TextView tv) {
		try {
			// get our dao
			Dao<SimpleData, Object> simpleDao = getHelper().getSimpleDataDao();
			// query for all of the data objects in the database
			List<SimpleData> list = simpleDao.queryForAll();
			// our string builder for building the content-view
			StringBuilder sb = new StringBuilder();
			sb.append("got ").append(list.size()).append(" entries in ").append(action).append("\n");

			// if we already have items in the database
			if (list.size() > 0) {
				// output the first one
				SimpleData simple = list.get(0);
				sb.append("--------------------------------\n");
				sb.append("[0] = ").append(simple).append("\n");
				sb.append("--------------------------------\n");
				// delete it
				int ret = simpleDao.delete(simple);
				sb.append("deleted entry = ").append(ret).append("\n");
				Log.i(LOG_TAG, "deleting simple(" + simple.millis + ") returned " + ret);
			}

			// create a new simple object
			long millis = System.currentTimeMillis();
			SimpleData simple = new SimpleData(millis);
			// store it in the database
			int ret = simpleDao.create(simple);
			Log.i(LOG_TAG, "creating simple(" + millis + ") returned " + ret);

			// output it
			sb.append("created new entry = ").append(ret).append("\n");
			sb.append("--------------------------------\n");
			sb.append("new entry = ").append(simple).append("\n");
			sb.append("--------------------------------\n");
			tv.setText(sb.toString());
		} catch (SQLException e) {
			Log.e(LOG_TAG, "Database exception", e);
			return;
		}
	}
}