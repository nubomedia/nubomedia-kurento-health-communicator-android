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

public class GroupsMemberDataBaseAdapter {

	private static final String DATABASE_NAME = "Groupsmember.db";
	private static final String DATABASE_TABLE = "groupsmember";
	private static final int DATABASE_VERSION = 5;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_GROUP_ID = "group_id";
	public static final int GROUP_ID_COLUMN = 1;

	public static final String KEY_GROUP_NAME = "group_name";
	public static final int GROUP_NAME_COLUMN = 2;

	public static final String KEY_USER_ID = "user_id";
	public static final int USER_ID_COLUMN = 3;

	public static final String KEY_USER_NAME = "user_name";
	public static final int USER_NAME_COLUMN = 4;

	public static final String KEY_USER_SURNAME = "user_surname";
	public static final int USER_SURNAME_COLUMN = 5;

	public static final String KEY_USER_PICTURE = "user_picture";
	public static final int USER_PICTURE_COLUMN = 6;

	public static final String KEY_ADMIN = "admin";
	public static final int ADMIN_COLUMN = 7;

	public static final String KEY_TO_DELETE = "to_delete";
	public static final int TO_DELETE_COLUMN = 8;

	public static final String KEY_TO_ADD = "to_add";
	public static final int TO_ADD_COLUMN = 9;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_GROUP_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_GROUP_NAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_USER_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_USER_NAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_USER_SURNAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_USER_PICTURE
			+ ConstantKeys.NOT_NULL + ", " + KEY_ADMIN + ConstantKeys.NOT_NULL
			+ ", " + KEY_TO_DELETE + ConstantKeys.NOT_NULL + ", " + KEY_TO_ADD
			+ ConstantKeys.NOT_NULL + ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public GroupsMemberDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public GroupsMemberDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public int insertEntry(GroupMemberObject groupMember) {
		int index = 0;

		String strFilter = KEY_GROUP_ID + "=" + "\""
				+ groupMember.getGroup().getId().toString() + "\"" + " AND "
				+ KEY_USER_ID + "=" + "\""
				+ groupMember.getUser().getId().toString() + "\"";

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_GROUP_ID, groupMember.getGroup().getId());
		newValues.put(KEY_GROUP_NAME, groupMember.getGroup().getName());
		newValues.put(KEY_USER_ID, groupMember.getUser().getId());
		newValues.put(KEY_USER_NAME, groupMember.getUser().getName());
		newValues.put(KEY_USER_SURNAME, groupMember.getUser().getSurname());
		newValues.put(KEY_USER_PICTURE, groupMember.getUser().getPicture());

		if (groupMember.isToDeleted()) {
			newValues.put(KEY_TO_DELETE, "0");
		} else {
			newValues.put(KEY_TO_DELETE, "1");
		}

		if (groupMember.isToAdd()) {
			newValues.put(KEY_TO_ADD, "0");
		} else {
			newValues.put(KEY_TO_ADD, "1");
		}

		if (groupMember.isAdmin()) {
			newValues.put(KEY_ADMIN, "0");
		} else {
			newValues.put(KEY_ADMIN, "1");
		}

		int nRowsEffected = db.update(DATABASE_TABLE, newValues, strFilter,
				null);

		if (nRowsEffected == 0) {
			db.insert(DATABASE_TABLE, null, newValues);
		}

		return index;
	}

	public boolean setToDelete(String groupId, String userId, boolean value) {
		String strFilter = KEY_GROUP_ID + "=" + "\"" + groupId + "\"" + " AND "
				+ KEY_USER_ID + "=" + "\"" + userId + "\"";
		ContentValues args = new ContentValues();
		if (value) {
			args.put(KEY_TO_DELETE, "0");
		} else {
			args.put(KEY_TO_DELETE, "1");
		}

		db.update(DATABASE_TABLE, args, strFilter, null);

		return true;
	}

	public boolean setToAdd(String groupId, String userId, boolean value) {
		String strFilter = KEY_GROUP_ID + "=" + "\"" + groupId + "\"" + " AND "
				+ KEY_USER_ID + "=" + "\"" + userId + "\"";
		ContentValues args = new ContentValues();
		if (value) {
			args.put(KEY_TO_ADD, "0");
		} else {
			args.put(KEY_TO_ADD, "1");
		}
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean removeEntry(GroupMemberObject user) {
		return db.delete(DATABASE_TABLE, KEY_GROUP_ID + "="
				+ user.getGroup().getId() + " AND " + KEY_USER_ID + "="
				+ user.getUser().getId(), null) > 0;
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
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_GROUP_ID,
				KEY_GROUP_NAME, KEY_USER_ID, KEY_USER_NAME, KEY_USER_SURNAME,
				KEY_USER_PICTURE, KEY_ADMIN, KEY_TO_DELETE, KEY_TO_ADD }, null,
				null, null, null, null);
	}

	public ArrayList<GroupMemberObject> getAllEntriesParsed() {
		ArrayList<GroupMemberObject> list = new ArrayList<GroupMemberObject>();
		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				GroupMemberObject groupMember = new GroupMemberObject();
				groupMember.getGroup().setId(cursor.getLong(GROUP_ID_COLUMN));
				groupMember.getGroup().setName(
						cursor.getString(GROUP_NAME_COLUMN));
				groupMember.getUser().setId(cursor.getLong(USER_ID_COLUMN));
				groupMember.getUser().setName(
						cursor.getString(USER_NAME_COLUMN));
				groupMember.getUser().setSurname(
						cursor.getString(USER_SURNAME_COLUMN));
				groupMember.getUser().setPicture(
						cursor.getLong(USER_PICTURE_COLUMN));

				if (cursor.getString(ADMIN_COLUMN).equals("0")) {
					groupMember.setAdmin(true);
				} else {
					groupMember.setAdmin(false);
				}

				if (cursor.getString(TO_DELETE_COLUMN).equals("0")) {
					groupMember.setToDeleted(true);
				} else {
					groupMember.setToDeleted(false);
				}

				if (cursor.getString(TO_ADD_COLUMN).equals("0")) {
					groupMember.setToAdd(true);
				} else {
					groupMember.setToAdd(false);
				}

				list.add(groupMember);
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public ArrayList<GroupMemberObject> getMembers(Long groupId) {
		ArrayList<GroupMemberObject> list = new ArrayList<GroupMemberObject>();
		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				GroupMemberObject groupMember = new GroupMemberObject();
				groupMember.getGroup().setId(cursor.getLong(GROUP_ID_COLUMN));
				groupMember.getGroup().setName(
						cursor.getString(GROUP_NAME_COLUMN));
				groupMember.getUser().setId(cursor.getLong(USER_ID_COLUMN));
				groupMember.getUser().setName(
						cursor.getString(USER_NAME_COLUMN));
				groupMember.getUser().setSurname(
						cursor.getString(USER_SURNAME_COLUMN));
				groupMember.getUser().setPicture(
						cursor.getLong(USER_PICTURE_COLUMN));

				if (cursor.getString(ADMIN_COLUMN).equals("0")) {
					groupMember.setAdmin(true);
				} else {
					groupMember.setAdmin(false);
				}

				if (cursor.getString(TO_DELETE_COLUMN).equals("0")) {
					groupMember.setToDeleted(true);
				} else {
					groupMember.setToDeleted(false);
				}

				if (cursor.getString(TO_ADD_COLUMN).equals("0")) {
					groupMember.setToAdd(true);
				} else {
					groupMember.setToAdd(false);
				}

				if (groupMember.getGroup().getId().toString()
						.equals(groupId.toString())) {
					list.add(groupMember);
				}
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
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
