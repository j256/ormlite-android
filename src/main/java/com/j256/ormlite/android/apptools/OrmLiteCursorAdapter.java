package com.j256.ormlite.android.apptools;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CursorAdapter;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.stmt.PreparedQuery;

import java.sql.SQLException;

public abstract class OrmLiteCursorAdapter<T,ViewType extends View> extends CursorAdapter {
    protected PreparedQuery<T> query;

    public OrmLiteCursorAdapter(Context context) {
        super(context, null, false);
    }

    // Final to prevent subclasses from accidentally overriding.
    // Intentional overriding can be accomplished by overriding doBindView
    @Override
    final public void bindView(View itemView, Context context, Cursor cursor) {
        doBindView(itemView,context,cursor);
    }

    @SuppressWarnings("unchecked")
    protected void doBindView(View itemView, Context context, Cursor cursor) {
        try {
            bindView((ViewType)itemView, context, query.mapRow(new AndroidDatabaseResults(cursor, null)));

        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    final public void changeCursor(Cursor cursor) {
        throw new UnsupportedOperationException("Please use changeCursor(Cursor,PreparedQuery) instead");
    }

    public void changeCursor(Cursor cursor, PreparedQuery<T> query ) {
        setQuery(query);
        super.changeCursor(cursor);
    }

    public void setQuery(PreparedQuery<T> query) {
        this.query = query;
    }

    abstract public void bindView(ViewType itemView, Context context, T item);
}
