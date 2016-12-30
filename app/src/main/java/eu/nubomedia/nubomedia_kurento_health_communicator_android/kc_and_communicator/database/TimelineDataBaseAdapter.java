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

import com.kurento.agenda.datamodel.pojo.Timeline.State;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class TimelineDataBaseAdapter {

	private static final String DATABASE_NAME = "Timeline.db";
	private static final String DATABASE_TABLE = "timelines";
	private static final int DATABASE_VERSION = 7;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_TIMELINE_ID = "timeline_id";
	public static final int TIMELINE_ID_COLUMN = 1;

	public static final String KEY_OWNER = "owner";
	public static final int OWNER_COLUMN = 2;

	public static final String KEY_PARTY_ID = "party_id";
	public static final int PARTY_ID_COLUMN = 3;

	public static final String KEY_NEW_MESSAGES = "new_messages";
	public static final int NEW_MESSAGES_COLUMN = 4;

	public static final String KEY_LAST_MESSAGE_ID = "last_messages_id";
	public static final int LAST_MESSAGE_ID_COLUMN = 5;

	public static final String KEY_SHOWIT = "show_it";
	public static final int SHOWIT_COLUMN = 6;

	public static final String KEY_PARTY_NAME = "party_name";
	public static final int PARTY_NAME_COLUMN = 7;

	public static final String KEY_PARTY_TYPE = "party_type";
	public static final int PARTY_TYPE_COLUMN = 8;

	public static final String KEY_LAST_MESSAGE_TIMESTAMP = "last_message_timestamp";
	public static final int LAST_MESSAGE_TIMESTAMP_COLUMN = 9;

	public static final String KEY_TIMESTAMP_DRIFT = "timestamp_drift";
	public static final int TIMESTAMP_DRIFT_COLUMN = 10;

	public static final String KEY_STATE = "state";
	public static final int STATE_COLUMN = 11;

	public static final String KEY_LAST_MESSAGE_BODY = "last_message_body";
	public static final int LAST_MESSAGE_BODY_COLUMN = 12;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_TIMELINE_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_OWNER + ConstantKeys.NOT_NULL
			+ ", " + KEY_PARTY_ID + ConstantKeys.NOT_NULL + ", "
			+ KEY_NEW_MESSAGES + ConstantKeys.NOT_NULL + ", "
			+ KEY_LAST_MESSAGE_ID + " text not null," + KEY_SHOWIT
			+ ConstantKeys.NOT_NULL + ", " + KEY_PARTY_NAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_PARTY_TYPE
			+ ConstantKeys.NOT_NULL + ", " + KEY_LAST_MESSAGE_TIMESTAMP
			+ ConstantKeys.NOT_NULL + ", " + KEY_TIMESTAMP_DRIFT
			+ ConstantKeys.NOT_NULL + ", " + KEY_STATE
			+ ConstantKeys.NOT_NULL + ", " + KEY_LAST_MESSAGE_BODY
			+ ConstantKeys.NOT_NULL + ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public TimelineDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public TimelineDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public boolean setNewMessage(Long timelineId, Long messageId,
			boolean fromUser, Long timestamp, String messageBody) {
		TimelineObject timeline = getTimeline(String.valueOf(timelineId));
		if (timeline == null) {
			return false;
		}

		String strFilter = KEY_TIMELINE_ID + "=" + "\"" + timelineId + "\"";
		ContentValues args = new ContentValues();
		if (fromUser) {
			args.put(KEY_NEW_MESSAGES, "true");
		}

		Long ts = timeline.getLastMessageTimestamp();
		if (ts != null && ts > timestamp) {
			args.put(KEY_LAST_MESSAGE_TIMESTAMP, ts);
			args.put(KEY_LAST_MESSAGE_BODY, timeline.getLastMessageBody());
			args.put(KEY_LAST_MESSAGE_ID, timeline.getLastMessageId());
		} else {
			args.put(KEY_LAST_MESSAGE_TIMESTAMP, timestamp);
			args.put(KEY_LAST_MESSAGE_BODY, messageBody);
			args.put(KEY_LAST_MESSAGE_ID, messageId);
		}

		args.put(KEY_SHOWIT, "0");
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean setNewMessageFalse(Long timelineId) {
		String strFilter = KEY_TIMELINE_ID + "=" + "\"" + timelineId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_NEW_MESSAGES, "false");
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean dontShow(Long partyId) {
		String strFilter = KEY_PARTY_ID + "=" + "\"" + partyId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_STATE, State.HIDDEN.toString());
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean setState(Long partyId, State state) {
		String strFilter = KEY_PARTY_ID + "=" + "\"" + partyId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_STATE, state.toString());
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean setDrift(Long timelineId, Long drift) {
		String strFilter = KEY_TIMELINE_ID + "=" + "\"" + timelineId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_TIMESTAMP_DRIFT, drift);
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean RecoverTimeline(Long partyId) {
		String strFilter = KEY_PARTY_ID + "=" + "\"" + partyId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_STATE, State.ENABLED.toString());
		int nRowsEffected = db.update(DATABASE_TABLE, args, strFilter, null);
		if (nRowsEffected == 0) {
			return false;
		}
		return true;
	}

	public int insertEntry(TimelineObject timeline) {
		int index = 0;

		String strFilter = KEY_PARTY_ID + "=" + "\""
				+ timeline.getParty().getId() + "\"";

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_TIMELINE_ID, timeline.getId());
		newValues.put(KEY_OWNER, timeline.getOwnerId());
		newValues.put(KEY_PARTY_ID, timeline.getParty().getId());
		newValues.put(KEY_PARTY_NAME, timeline.getParty().getName());
		newValues.put(KEY_PARTY_TYPE, timeline.getParty().getType().toString()
				.toLowerCase());
		newValues.put(KEY_STATE, timeline.getState().toString());

		if (timeline.isShowIt()) {
			newValues.put(KEY_SHOWIT, "0");
		} else {
			newValues.put(KEY_SHOWIT, "1");
		}

		int nRowsEffected = db.update(DATABASE_TABLE, newValues, strFilter,
				null);
		if (nRowsEffected == 0) {
			newValues.put(KEY_NEW_MESSAGES, "false");
			newValues.put(KEY_LAST_MESSAGE_ID, "0");
			newValues
					.put(KEY_LAST_MESSAGE_TIMESTAMP, ConstantKeys.LONG_DEFAULT);

			newValues.put(KEY_TIMESTAMP_DRIFT, timeline.getTimestampDrift());
			newValues.put(KEY_LAST_MESSAGE_BODY, timeline.getLastMessageBody());

			db.insert(DATABASE_TABLE, null, newValues);
		}

		return index;
	}

	public boolean removeEntry(String id) {
		return db.delete(DATABASE_TABLE, KEY_PARTY_ID + "=" + id, null) > 0;
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
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_TIMELINE_ID,
				KEY_OWNER, KEY_PARTY_ID, KEY_NEW_MESSAGES, KEY_LAST_MESSAGE_ID,
				KEY_SHOWIT, KEY_PARTY_NAME, KEY_PARTY_TYPE,
				KEY_LAST_MESSAGE_TIMESTAMP, KEY_TIMESTAMP_DRIFT, KEY_STATE,
				KEY_LAST_MESSAGE_BODY }, whereClause, whereArgs, null, null,
				KEY_LAST_MESSAGE_TIMESTAMP + " DESC");
	}

	public Cursor getAllEntries() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_TIMELINE_ID,
				KEY_OWNER, KEY_PARTY_ID, KEY_NEW_MESSAGES, KEY_LAST_MESSAGE_ID,
				KEY_SHOWIT, KEY_PARTY_NAME, KEY_PARTY_TYPE,
				KEY_LAST_MESSAGE_TIMESTAMP, KEY_TIMESTAMP_DRIFT, KEY_STATE,
				KEY_LAST_MESSAGE_BODY }, null, null, null, null,
				KEY_LAST_MESSAGE_TIMESTAMP + " DESC");
	}

	public ArrayList<TimelineObject> getAllEntriesParsed() {
		ArrayList<TimelineObject> list = new ArrayList<TimelineObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				TimelineObject timeline = cursorToTimelineObject(cursor);

				if (timeline.isShowIt()) {
					list.add(timeline);
				}

			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public TimelineObject getTimeline(String timelineId) {
		TimelineObject timeline = new TimelineObject();
		Cursor cursor = getEntries(KEY_TIMELINE_ID + " = ?",
				new String[] { timelineId });
		if (cursor.moveToFirst()) {
			timeline = cursorToTimelineObject(cursor);
		}
		cursor.close();

		return timeline;
	}

	public TimelineObject getEntrieParsed(Long id, int column) {
		TimelineObject toReturn = null;
		Cursor cursor = getAllEntries();
		if (!cursor.moveToFirst()) {
			return null;
		}

		do {
			Long rowId = cursor.getLong(column);
			if (rowId.equals(id)) {
				return cursorToTimelineObject(cursor);
			}
		} while (cursor.moveToNext());

		cursor.close();

		return toReturn;
	}

	private TimelineObject cursorToTimelineObject(Cursor cursor) {
		TimelineObject timeline = new TimelineObject();
		timeline.setId(cursor.getLong(TIMELINE_ID_COLUMN));
		timeline.setOwnerId(cursor.getLong(OWNER_COLUMN));
		timeline.getParty().setId(cursor.getLong(PARTY_ID_COLUMN));
		timeline.setLastMessageId(cursor.getLong(LAST_MESSAGE_ID_COLUMN));
		timeline.setLastMessageTimestamp(cursor
				.getLong(LAST_MESSAGE_TIMESTAMP_COLUMN));
		timeline.getParty().setName(cursor.getString(PARTY_NAME_COLUMN));
		timeline.getParty().setType(cursor.getString(PARTY_TYPE_COLUMN));

		if (cursor.getString(SHOWIT_COLUMN).equals("0")) {
			timeline.setShowIt(true);
		} else {
			timeline.setShowIt(false);
		}

		if (cursor.getString(NEW_MESSAGES_COLUMN).equalsIgnoreCase("true")) {
			timeline.setNewMessages(true);
		} else {
			timeline.setNewMessages(false);
		}

		timeline.setState(State.valueOf(cursor
				.getString(STATE_COLUMN)));

		timeline.setTimestampDrift(cursor.getLong(TIMESTAMP_DRIFT_COLUMN));
		timeline.setLastMessageBody(cursor.getString(LAST_MESSAGE_BODY_COLUMN));

		return timeline;
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
