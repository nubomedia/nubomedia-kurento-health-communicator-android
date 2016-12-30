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

public class UsersDataBaseAdapter {

	private static final String DATABASE_NAME = "Contacts.db";
	private static final String DATABASE_TABLE = "contacts";
	private static final int DATABASE_VERSION = 2;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_CONTACT_ID = "contact_id";
	public static final int CONTACT_ID_COLUMN = 1;

	public static final String KEY_NAME = "name";
	public static final int NAME_COLUMN = 2;

	public static final String KEY_SURNAME = "surname";
	public static final int SURNAME_COLUMN = 3;

	public static final String KEY_PICTURE = "picture";
	public static final int PICTURE_COLUMN = 4;

	public static final String KEY_PHONE = "phone";
	public static final int PHONE_COLUMN = 5;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_CONTACT_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_NAME + ConstantKeys.NOT_NULL
			+ ", " + KEY_SURNAME + ConstantKeys.NOT_NULL + ", " + KEY_PICTURE
			+ ConstantKeys.NOT_NULL + ", " + KEY_PHONE + ConstantKeys.NOT_NULL
			+ ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public UsersDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public UsersDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public int insertEntry(UserObject user) {
		int index = 0;

		String strFilter = KEY_CONTACT_ID + "=" + "\"" + user.getId() + "\"";

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_CONTACT_ID, user.getId());
		newValues.put(KEY_NAME, user.getName());
		newValues.put(KEY_SURNAME, user.getSurname());
		newValues.put(KEY_PICTURE, user.getPicture());
		newValues.put(KEY_PHONE, user.getPhone());

		int nRowsEffected = db.update(DATABASE_TABLE, newValues, strFilter,
				null);
		if (nRowsEffected == 0) {
			db.insert(DATABASE_TABLE, null, newValues);
		}

		return index;
	}

	public boolean removeEntry(String contact) {
		return db.delete(DATABASE_TABLE, KEY_CONTACT_ID + "=" + contact, null) > 0;
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

	public Cursor getEntries(String whereClause, String whereArgs[]) {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_CONTACT_ID,
				KEY_NAME, KEY_SURNAME, KEY_PICTURE, KEY_PHONE }, whereClause,
				whereArgs, null, null, KEY_NAME + " COLLATE NOCASE ASC");
	}

	public UserObject getUser(String userId) {
		UserObject group = new UserObject();
		Cursor cursor = getEntries(KEY_CONTACT_ID + " = ?",
				new String[] { userId });
		if (cursor.moveToFirst()) {
			group = cursorToUserObject(cursor);
		}
		cursor.close();

		return group;
	}

	public Cursor getAllEntries() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_CONTACT_ID,
				KEY_NAME, KEY_SURNAME, KEY_PICTURE, KEY_PHONE }, null, null,
				null, null, KEY_NAME + " COLLATE NOCASE ASC");
	}

	public ArrayList<UserObject> getAllEntriesParsed() {
		ArrayList<UserObject> list = new ArrayList<UserObject>();
		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst())
			do {
				UserObject contact = new UserObject();
				contact.setId(cursor.getLong(CONTACT_ID_COLUMN));
				contact.setName(cursor.getString(NAME_COLUMN));
				contact.setSurname(cursor.getString(SURNAME_COLUMN));
				contact.setPicture(cursor.getLong(PICTURE_COLUMN));
				contact.setPhone(cursor.getString(PHONE_COLUMN));

				list.add(contact);
			} while (cursor.moveToNext());
		cursor.close();

		return list;
	}

	private UserObject cursorToUserObject(Cursor cursor) {
		UserObject user = new UserObject();

		user.setId(cursor.getLong(CONTACT_ID_COLUMN));
		user.setName(cursor.getString(NAME_COLUMN));
		user.setSurname(cursor.getString(SURNAME_COLUMN));
		user.setPicture(cursor.getLong(PICTURE_COLUMN));
		user.setPhone(cursor.getString(PHONE_COLUMN));

		return user;
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
