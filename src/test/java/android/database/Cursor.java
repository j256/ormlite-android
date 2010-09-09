package android.database;

/**
 * Stub implementation of the Android cursor object to stop compilation errors.
 */
public interface Cursor {

	public boolean moveToFirst();

	public int getColumnCount();

	public String getColumnName(int column);

	public void close();

	public boolean moveToNext();

	public int getColumnIndex(String columnName);

	public String getString(int columnIndex);

	public boolean isNull(int columnIndex);

	public short getShort(int columnIndex);

	public byte[] getBlob(int columnIndex);

	public int getInt(int columnIndex);

	public long getLong(int columnIndex);

	public float getFloat(int columnIndex);

	public double getDouble(int columnIndex);
}
