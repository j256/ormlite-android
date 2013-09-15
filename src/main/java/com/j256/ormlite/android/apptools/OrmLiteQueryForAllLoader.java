package com.j256.ormlite.android.apptools;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * A <code>Loader</code> implementation that queries specified {@link com.j256.ormlite.dao.Dao} for
 * all data, using the <code>Dao.queryForAll()</code> call.
 *
 * @author EgorAnd
 */
public class OrmLiteQueryForAllLoader<T, ID> extends AbstractOrmLiteLoader<T, ID> {

    public OrmLiteQueryForAllLoader(Context context) {
        super(context);
    }

    public OrmLiteQueryForAllLoader(Context context, Dao<T, ID> dao) {
        super(context, dao);
    }

    @Override
    public List<T> loadInBackground() {
        if (getDao() == null) {
            throw new IllegalStateException("Dao is not initialized.");
        }
        try {
            return getDao().queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
