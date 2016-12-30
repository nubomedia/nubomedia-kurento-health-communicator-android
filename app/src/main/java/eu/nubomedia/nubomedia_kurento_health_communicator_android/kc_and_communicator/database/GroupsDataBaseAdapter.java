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

public class GroupsDataBaseAdapter {

	private static final String DATABASE_NAME = "Groups.db";
	private static final String DATABASE_TABLE = "groups";
	private static final int DATABASE_VERSION = 5;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_GROUP_ID = "group_id";
	public static final int GROUP_ID_COLUMN = 1;

	public static final String KEY_NAME = "name";
	public static final int NAME_COLUMN = 2;

	public static final String KEY_CANREAD = "can_read";
	public static final int CANREAD_COLUMN = 3;

	public static final String KEY_CANSEND = "can_send";
	public static final int CANSEND_COLUMN = 4;

	public static final String KEY_CANLEAVE = "can_leave";
	public static final int CANLEAVE_COLUMN = 5;

	public static final String KEY_PICTURE = "picture";
	public static final int PICTURE_COLUMN = 6;

	public static final String KEY_ISADMIN = "is_admin";
	public static final int ISADMIN_COLUMN = 7;

	public static final String KEY_TO_DELETE = "to_delete";
	public static final int TO_DELETE_COLUMN = 8;

	public static final String KEY_LOCAL_ID = "local_id";
	public static final int LOCAL_ID_COLUMN = 9;

	public static final String KEY_PHONE = "phone";
	public static final int PHONE_COLUMN = 10;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_GROUP_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_NAME + ConstantKeys.NOT_NULL
			+ ", " + KEY_CANREAD + ConstantKeys.NOT_NULL + ", " + KEY_CANSEND
			+ ConstantKeys.NOT_NULL + ", " + KEY_CANLEAVE
			+ ConstantKeys.NOT_NULL + ", " + KEY_PICTURE
			+ ConstantKeys.NOT_NULL + ", " + KEY_ISADMIN
			+ ConstantKeys.NOT_NULL + ", " + KEY_TO_DELETE
			+ ConstantKeys.NOT_NULL + ", " + KEY_LOCAL_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_PHONE + ConstantKeys.NOT_NULL
			+ ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public GroupsDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public GroupsDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public int insertEntry(GroupObject group) {
		int index = 0;

		String strFilter;
		if (!group.getLocalId().toString().equals(ConstantKeys.STRING_CERO)) {
			strFilter = KEY_LOCAL_ID + "=" + "\"" + group.getLocalId() + "\"";
		} else {
			strFilter = KEY_LOCAL_ID + "=" + "\"" + group.getGroupId() + "\"";
		}

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_GROUP_ID, group.getGroupId());
		newValues.put(KEY_NAME, group.getName());
		newValues.put(KEY_PICTURE, group.getPicture());

		if (group.getCanRead()) {
			newValues.put(KEY_CANREAD, "0");
		} else {
			newValues.put(KEY_CANREAD, "1");
		}

		if (group.getCanSend()) {
			newValues.put(KEY_CANSEND, "0");
		} else {
			newValues.put(KEY_CANSEND, "1");
		}

		if (group.getCanLeave()) {
			newValues.put(KEY_CANLEAVE, "0");
		} else {
			newValues.put(KEY_CANLEAVE, "1");
		}

		if (group.isAdmin()) {
			newValues.put(KEY_ISADMIN, "0");
		} else {
			newValues.put(KEY_ISADMIN, "1");
		}

		if (group.isToDelete()) {
			newValues.put(KEY_TO_DELETE, "0");
		} else {
			newValues.put(KEY_TO_DELETE, "1");
		}

		newValues.put(KEY_LOCAL_ID, group.getLocalId());
		newValues.put(KEY_PHONE, group.getPhone());

		int nRowsEffected = db.update(DATABASE_TABLE, newValues, strFilter,
				null);
		if (nRowsEffected == 0) {
			db.insert(DATABASE_TABLE, null, newValues);
		}

		return index;
	}

	public boolean setToDelete(String groupId, boolean value) {
		String strFilter = KEY_GROUP_ID + "=" + "\"" + groupId + "\"";
		ContentValues args = new ContentValues();
		if (value) {
			args.put(KEY_TO_DELETE, "0");
		} else {
			args.put(KEY_TO_DELETE, "1");
		}
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean deleteLocal(Long id) {
		return db.delete(DATABASE_TABLE, KEY_LOCAL_ID + "=" + id, null) > 0;
	}

	public boolean removeEntry(String id) {
		return db.delete(DATABASE_TABLE, KEY_GROUP_ID + "=" + id, null) > 0;
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
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_GROUP_ID,
				KEY_NAME, KEY_CANREAD, KEY_CANSEND, KEY_CANLEAVE, KEY_PICTURE,
				KEY_ISADMIN, KEY_TO_DELETE, KEY_LOCAL_ID, KEY_PHONE },
				whereClause, whereArgs, null, null, KEY_NAME
						+ " COLLATE NOCASE ASC");
	}

	public Cursor getAllEntries() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_GROUP_ID,
				KEY_NAME, KEY_CANREAD, KEY_CANSEND, KEY_CANLEAVE, KEY_PICTURE,
				KEY_ISADMIN, KEY_TO_DELETE, KEY_LOCAL_ID, KEY_PHONE }, null,
				null, null, null, KEY_NAME + " COLLATE NOCASE ASC");
	}

	public GroupObject getGroup(String groupId) {
		GroupObject group = new GroupObject();
		Cursor cursor = getEntries(KEY_GROUP_ID + " = ?",
				new String[] { groupId });
		if (cursor.moveToFirst()) {
			group = cursorToGroupObject(cursor);
		}
		cursor.close();

		return group;
	}

	public ArrayList<GroupObject> getAllEntriesParsed() {
		ArrayList<GroupObject> list = new ArrayList<GroupObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst())
			do {
				GroupObject group = cursorToGroupObject(cursor);
				list.add(group);
			} while (cursor.moveToNext());

		cursor.close();

		return list;
	}

	private GroupObject cursorToGroupObject(Cursor cursor) {
		GroupObject group = new GroupObject();
		group.setGroupId(cursor.getString(GROUP_ID_COLUMN));
		group.setName(cursor.getString(NAME_COLUMN));

		if (cursor.getString(CANREAD_COLUMN).equals("0")) {
			group.setCanRead(true);
		} else {
			group.setCanRead(false);
		}

		if (cursor.getString(CANSEND_COLUMN).equals("0")) {
			group.setCanSend(true);
		} else {
			group.setCanSend(false);
		}

		if (cursor.getString(CANLEAVE_COLUMN).equals("0")) {
			group.setCanLeave(true);
		} else {
			group.setCanLeave(false);
		}

		if (cursor.getString(ISADMIN_COLUMN).equals("0")) {
			group.setIsAdmin(true);
		} else {
			group.setIsAdmin(false);
		}

		if (cursor.getString(TO_DELETE_COLUMN).equals("0")) {
			group.setToDelete(true);
		} else {
			group.setToDelete(false);
		}

		group.setPicture(cursor.getLong(PICTURE_COLUMN));
		group.setLocalId(cursor.getLong(LOCAL_ID_COLUMN));
		group.setPhone(cursor.getString(PHONE_COLUMN));

		return group;
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
