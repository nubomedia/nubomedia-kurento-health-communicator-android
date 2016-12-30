// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;


public class AvatarDataBaseAdapter {

	private static final String DATABASE_NAME = "Avatar.db";
	private static final String DATABASE_TABLE = "avatar";
	private static final int DATABASE_VERSION = 1;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_USER_ID = "user_id";
	public static final int USER_ID_COLUMN = 1;

	public static final String KEY_AVATAR_ID = "avatar_id";
	public static final int AVATAR_ID_COLUMN = 2;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_USER_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_AVATAR_ID
			+ ConstantKeys.NOT_NULL + ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public AvatarDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public AvatarDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public ArrayList<AvatarObject> getAllEntriesParsed() {
		ArrayList<AvatarObject> list = new ArrayList<AvatarObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst())
			do {
				AvatarObject avatar = new AvatarObject();
				avatar.setUserId(cursor.getString(USER_ID_COLUMN));
				avatar.setAvatarId(cursor.getString(AVATAR_ID_COLUMN));
				list.add(avatar);

			} while (cursor.moveToNext());

		cursor.close();
		return list;
	}

	public int insertEntry(String userId, String avatarId) {
		int index = 0;

		String strFilter = KEY_USER_ID + "=" + "\"" + userId + "\"";

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_USER_ID, userId);
		newValues.put(KEY_AVATAR_ID, avatarId);

		int nRowsEffected = db.update(DATABASE_TABLE, newValues, strFilter,
				null);
		if (nRowsEffected == 0) {
			db.insert(DATABASE_TABLE, null, newValues);
		}

		return index;
	}

	public boolean removeEntry(String userId) {
		return db.delete(DATABASE_TABLE, KEY_USER_ID + "=" + userId, null) > 0;
	}

	public boolean removeAll() {
		return db.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean isEmpty() {
		Cursor cursor = getAllEntries();
		boolean ret = !cursor.moveToFirst();
		cursor.close();

		return ret;
	}

	public Cursor getAllEntries() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_USER_ID,
				KEY_AVATAR_ID }, null, null, null, null, KEY_ID + " DESC");
	}

	private static class myDbHelper extends SQLiteOpenHelper {
		public myDbHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		// Called when no database exists in disk and the helper class needs to
		// create a new one.
		// this means that you need to take the row of users, so we need to make
		// an insert
		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE);
		}

		// called when there is a dabase vewrsion mismatch meaning that the
		// version of the database on disk need to be upgraded to the curren
		// version
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
				int _newVersion) {
			// Upgrade the existing database to conform to the new version.
			// multiple
			// previous version can be handle by comparing _oldVersion and
			// _newVersion values.
			// the simplest case is to drop the old table and create a new one.
			_db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(_db);
		}
	}
}
