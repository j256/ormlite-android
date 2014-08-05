package com.j256.ormlite.android.apptools.support;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
import com.j256.ormlite.android.AndroidCompiledStatement;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.DatabaseConnection;

import java.sql.SQLException;

import static com.j256.ormlite.stmt.StatementBuilder.StatementType.SELECT;

public class OrmLiteCursorLoader<T> extends AsyncTaskLoader<Cursor> implements Dao.Observer {

    protected Dao<T, ?> dao;
    protected PreparedQuery<T> query;
    protected Cursor cursor;

    public OrmLiteCursorLoader(Context context, Dao<T, ?> dao, PreparedQuery<T> query) {
        super(context);
        this.dao = dao;
        this.query = query;
        dao.registerContentObserver(this);
    }

    @Override
    public Cursor loadInBackground() {
        final Cursor cursor;
        try {
            final DatabaseConnection connection = dao.getConnectionSource().getReadOnlyConnection();
            final AndroidCompiledStatement statement = (AndroidCompiledStatement) query.compile(connection, SELECT);
            cursor = statement.getCursor();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Fill the cursor
        cursor.getCount();

        return cursor;
    }

    @Override
    public void deliverResult(Cursor newCursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (newCursor != null)
                newCursor.close();

            return;
        }

        final Cursor oldCursor = cursor;
        cursor = newCursor;

        if (isStarted())
            super.deliverResult(newCursor);

        if (oldCursor != null && oldCursor != newCursor && !oldCursor.isClosed())
            oldCursor.close();

    }

    @Override
    protected void onStartLoading() {
        if (cursor != null)
            deliverResult(cursor);

        if (takeContentChanged() || cursor == null)
            forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }

    @Override
    protected void onReset() {
        super.onReset();
        
        onStopLoading();

        if (cursor != null && !cursor.isClosed())
            cursor.close();

        cursor = null;
    }

    public void onContentUpdated() {
        onContentChanged();
    }

    public PreparedQuery<T> getQuery()
    {
        return query;
    }

    public void setQuery(PreparedQuery<T> mQuery)
    {
        this.query = mQuery;
    }

}
