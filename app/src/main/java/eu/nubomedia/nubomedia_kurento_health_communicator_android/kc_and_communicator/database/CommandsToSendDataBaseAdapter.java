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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class CommandsToSendDataBaseAdapter {

	private static final Logger log = LoggerFactory
			.getLogger(CommandsToSendDataBaseAdapter.class.getSimpleName());

	private static final String DATABASE_NAME = "CommandToSend.db";
	private static final String DATABASE_TABLE = "commands";
	private static final int DATABASE_VERSION = 3;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_UID = "uid";
	public static final int UID_COLUMN = 1;

	public static final String KEY_JSON = "json";
	public static final int JSON_COLUMN = 2;

	public static final String KEY_MEDIA = "media";
	public static final int MEDIA_COLUMN = 3;

	public static final String KEY_SEND_STATUS = "send_status";
	public static final int SEND_STATUS_COLUMN = 4;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_UID
			+ " text not null, " + KEY_JSON + " text not null, " + KEY_MEDIA
			+ " text not null, " + KEY_SEND_STATUS + " text not null);";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public CommandsToSendDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public CommandsToSendDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public long insertEntry(CommandObject command, String media) {
		ContentValues newValues = new ContentValues();
		newValues.put(KEY_UID, command.getID());
		newValues.put(KEY_JSON, command.getJson());
		newValues.put(KEY_SEND_STATUS, command.getSendStatus().name());

		if (media == null) {
			newValues.put(KEY_MEDIA, Long.valueOf(0));
		} else {
			newValues.put(KEY_MEDIA, media);
		}

		long index = db.insert(DATABASE_TABLE, null, newValues);
		if (index == -1) {
			log.error("Error inserting command {}", command.getJson());
		}

		return index;
	}

	public boolean removeEntry(String timestamp) {
		int nDeleted = db.delete(DATABASE_TABLE, KEY_UID + " = ?",
				new String[] { timestamp });

		return nDeleted > 0;
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
		return getEntries(null, null);
	}

	public Cursor getEntries(String whereClause, String whereArgs[]) {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_UID,
				KEY_JSON, KEY_MEDIA, KEY_SEND_STATUS }, whereClause, whereArgs,
				null, null, KEY_UID + " ASC");
	}

	public ArrayList<CommandObject> getAllEntriesParsed() {
		ArrayList<CommandObject> list = new ArrayList<CommandObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				list.add(cursorToCommandObject(cursor));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public ArrayList<CommandObject> getCommandsWithStatus(
			CommandObject.SendStatus status) {
		ArrayList<CommandObject> list = new ArrayList<CommandObject>();

		Cursor cursor = getEntries(KEY_SEND_STATUS + " = ?",
				new String[] { status.name() });
		if (cursor.moveToFirst()) {
			do {
				list.add(cursorToCommandObject(cursor));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public ArrayList<CommandObject> getAllMessages() {
		ArrayList<CommandObject> list = new ArrayList<CommandObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				if (cursor.getString(MEDIA_COLUMN) != " ") {
					list.add(cursorToCommandObject(cursor));
				}
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	private CommandObject cursorToCommandObject(Cursor cursor) {
		CommandObject command = new CommandObject(cursor.getString(UID_COLUMN));
		command.setJson(cursor.getString(JSON_COLUMN));
		command.setSendStatus(CommandObject.SendStatus.getValueOf(cursor
				.getString(SEND_STATUS_COLUMN)));
		command.setMedia(cursor.getString(MEDIA_COLUMN));

		return command;
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
