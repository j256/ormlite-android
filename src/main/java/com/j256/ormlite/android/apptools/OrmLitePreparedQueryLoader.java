package com.j256.ormlite.android.apptools;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * A <code>Loader</code> implementation that queries specified {@link com.j256.ormlite.dao.Dao} using
 * a {@link com.j256.ormlite.stmt.PreparedQuery}.
 *
 * @author Egorand
 */
public class OrmLitePreparedQueryLoader<T, ID> extends AbstractOrmLiteLoader<T, ID> {

    private PreparedQuery<T> preparedQuery;

    public OrmLitePreparedQueryLoader(Context context) {
        super(context);
    }

    public OrmLitePreparedQueryLoader(Context context, Dao<T, ID> dao, PreparedQuery<T> preparedQuery) {
        super(context, dao);
        this.preparedQuery = preparedQuery;
    }

    @Override
    public List<T> loadInBackground() {
        if (getDao() == null) {
            throw new IllegalStateException("Dao is not initialized.");
        }
        if (preparedQuery == null) {
            throw new IllegalStateException("PreparedQuery is not initialized.");
        }
        try {
            return getDao().query(preparedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public void setPreparedQuery(PreparedQuery<T> preparedQuery) {
        this.preparedQuery = preparedQuery;
    }

    public PreparedQuery<T> getPreparedQuery() {
        return preparedQuery;
    }
}
