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
import java.util.Date;
import java.util.Iterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.kurento.agenda.services.pojo.ContentReadResponse;
import com.kurento.agenda.services.pojo.MessageApp;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class MessagesDataBaseAdapter {

	private static final String DATABASE_NAME = "Messages.db";
	private static final String DATABASE_TABLE = "messages";
	private static final int DATABASE_VERSION = 12;

	// the index key column name for use in where clauses.
	public static final String KEY_ID = "_id";

	// the name and column index of each column in your database
	public static final String KEY_MESSAGE_ID = "message_id";
	public static final int MESSAGE_ID_COLUMN = 1;

	public static final String KEY_FROM = "from_id";
	public static final int FROM_COLUMN = 2;

	public static final String KEY_BODY = "body";
	public static final int BODY_COLUMN = 3;

	public static final String KEY_CONTENT_ID = "content_id";
	public static final int CONTENT_ID_COLUMN = 4;

	public static final String KEY_TIMESTAMP = "timestamp";
	public static final int TIMESTAMP_COLUMN = 5;

	public static final String KEY_PAYLOAD = "payload";
	public static final int PAYLOAD_COLUMN = 6;

	public static final String KEY_FROM_NAME = "from_name";
	public static final int FROM_NAME_COLUMN = 7;

	public static final String KEY_TIMELINE = "timeline";
	public static final int TIMELINE_COLUMN = 8;

	public static final String KEY_LOCAL_ID = "local_id";
	public static final int LOCAL_ID_COLUMN = 9;

	public static final String KEY_FROM_SURNAME = "from_surname";
	public static final int FROM_SURNAME_COLUMN = 10;

	public static final String KEY_FROM_PICTURE = "from_picture";
	public static final int FROM_PICTURE_COLUMN = 11;

	public static final String KEY_CONTENT_TYPE = "content_type";
	public static final int CONTENT_TYPE_COLUMN = 12;

	public static final String KEY_CONTENT_SIZE = "content_size";
	public static final int CONTENT_SIZE_COLUMN = 13;

	public static final String KEY_PARTYID = "party_id";
	public static final int PARTYID_COLUMN = 14;

	public static final String KEY_TOTAL = "total_value";
	public static final int TOTAL_COLUMN = 15;

	public static final String KEY_MESSAGE_STATUS = "message_status";
	public static final int MESSAGE_STATUS_COLUMN = 16;

	// SQL statemen to create a new dabase.
	private static final String DATABASE_CREATE = "create table "
			+ DATABASE_TABLE + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_MESSAGE_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_FROM + ConstantKeys.NOT_NULL
			+ ", " + KEY_BODY + ConstantKeys.NOT_NULL + ", " + KEY_CONTENT_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_TIMESTAMP
			+ ConstantKeys.NOT_NULL + ", " + KEY_PAYLOAD + ", " + KEY_FROM_NAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_TIMELINE
			+ ConstantKeys.NOT_NULL + ", " + KEY_LOCAL_ID
			+ ConstantKeys.NOT_NULL + ", " + KEY_FROM_SURNAME
			+ ConstantKeys.NOT_NULL + ", " + KEY_FROM_PICTURE
			+ " text not null," + KEY_CONTENT_TYPE + " text not null,"
			+ KEY_CONTENT_SIZE + ConstantKeys.NOT_NULL + ", " + KEY_PARTYID
			+ ConstantKeys.NOT_NULL + ", " + KEY_TOTAL + ConstantKeys.NOT_NULL
			+ ", " + KEY_MESSAGE_STATUS + ConstantKeys.NOT_NULL + ");";

	private SQLiteDatabase db;
	private final Context context;
	public myDbHelper dbHelper;

	public MessagesDataBaseAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public MessagesDataBaseAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		db.close();
	}

	public void FreeUpSpace(Long timeline) {
		ArrayList<MessageObject> list = getSelectedEntriesParsed(timeline, null);
		Long futureSize = Long.valueOf(list.size()) + 1; // the new message;
		Long maxSize = Long.valueOf(Preferences.getMaxMessages(context));
		if (futureSize <= maxSize) {
			return;
		}

		Iterator<MessageObject> iterator = list.iterator();
		while ((futureSize > maxSize) && (iterator.hasNext())) {
			MessageObject item = iterator.next();
			removeEntry(item.getLocalId());
			futureSize = futureSize - 1;
		}
	}

	public int insertEntry(MessageObject message) {
		int index = 0;
		String strFilterLocal = KEY_LOCAL_ID + "=" + "\""
				+ message.getLocalId() + "\"";

		ContentValues newValues = new ContentValues();
		newValues.put(KEY_MESSAGE_ID, message.getId());
		newValues.put(KEY_FROM, message.getFrom().getId());
		newValues.put(KEY_BODY, message.getBody());
		newValues.put(KEY_CONTENT_ID, message.getContent().getId());
		newValues.put(KEY_TIMESTAMP, message.getTimestamp());
		if (message.getPayload() != null)
			newValues.put(KEY_PAYLOAD, message.getPayload());
		newValues.put(KEY_FROM_NAME, message.getFrom().getName());
		newValues.put(KEY_TIMELINE, message.getTimeline().getId());
		newValues.put(KEY_LOCAL_ID, message.getLocalId());
		newValues.put(KEY_FROM_SURNAME, message.getFrom().getSurname());
		newValues.put(KEY_FROM_PICTURE, message.getFrom().getPicture());
		newValues.put(KEY_CONTENT_TYPE, message.getContent().getContentType());
		newValues.put(KEY_CONTENT_SIZE, message.getContent().getContentSize());
		newValues.put(KEY_PARTYID, message.getPartyId());
		newValues.put(KEY_TOTAL, message.getTotal());
		newValues.put(KEY_MESSAGE_STATUS, message.getStatus().name());

		int localRows = db.update(DATABASE_TABLE, newValues, strFilterLocal,
				null);
		index = localRows;
		if (localRows == 0) {
			// Firt we free up space
			FreeUpSpace(message.getTimeline().getId());

			// We don't have a local one with the same data, let's check if we
			// have a message with the same server id
			if (message.getId() != 0) {
				String strFilterServer = KEY_MESSAGE_ID + "=" + "\""
						+ message.getId() + "\"";
				int serverRows = db.update(DATABASE_TABLE, newValues,
						strFilterServer, null);
				if (serverRows == 0) {
					db.insert(DATABASE_TABLE, null, newValues);
				}
			} else {
				db.insert(DATABASE_TABLE, null, newValues);
			}
		}

		return index;
	}

	public boolean setMessageStatus(Long localId, MessageObject.Status status) {
		String strFilter = KEY_LOCAL_ID + "=" + "\"" + localId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_MESSAGE_STATUS, status.name());
		db.update(DATABASE_TABLE, args, strFilter, null);

		return true;
	}

	public boolean setTotal(String localId, int total) {
		String strFilter = KEY_LOCAL_ID + "=" + "\"" + localId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_TOTAL, total);
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean replaceAck(String messageId, boolean ack) {
		String strFilter = KEY_MESSAGE_ID + "=" + "\"" + messageId + "\"";
		ContentValues args = new ContentValues();
		args.put(KEY_PAYLOAD, ack);
		db.update(DATABASE_TABLE, args, strFilter, null);
		return true;
	}

	public boolean removeEntry(Long localId) {
		return db.delete(DATABASE_TABLE, KEY_LOCAL_ID + "=" + localId, null) > 0;
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

	public int getObjectsNumber() {
		return db.query(
				DATABASE_TABLE,
				new String[] { KEY_ID, KEY_MESSAGE_ID, KEY_FROM, KEY_BODY,
						KEY_CONTENT_ID, KEY_TIMESTAMP, KEY_PAYLOAD,
						KEY_FROM_NAME, KEY_TIMELINE, KEY_LOCAL_ID,
						KEY_FROM_SURNAME, KEY_FROM_PICTURE, KEY_CONTENT_TYPE,
						KEY_CONTENT_SIZE, KEY_PARTYID, KEY_TOTAL,
						KEY_MESSAGE_STATUS }, null, null, null, null,
				KEY_TIMESTAMP + " ASC").getCount();
	}

	public Cursor getAllEntries() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_MESSAGE_ID,
				KEY_FROM, KEY_BODY, KEY_CONTENT_ID, KEY_TIMESTAMP, KEY_PAYLOAD,
				KEY_FROM_NAME, KEY_TIMELINE, KEY_LOCAL_ID, KEY_FROM_SURNAME,
				KEY_FROM_PICTURE, KEY_CONTENT_TYPE, KEY_CONTENT_SIZE,
				KEY_PARTYID, KEY_TOTAL, KEY_MESSAGE_STATUS }, null, null, null,
				null, KEY_TIMESTAMP + " ASC");
	}

	public Cursor getEntries(String whereClause, String whereArgs[]) {
		return db.query(DATABASE_TABLE, new String[] { KEY_ID, KEY_MESSAGE_ID,
				KEY_FROM, KEY_BODY, KEY_CONTENT_ID, KEY_TIMESTAMP, KEY_PAYLOAD,
				KEY_FROM_NAME, KEY_TIMELINE, KEY_LOCAL_ID, KEY_FROM_SURNAME,
				KEY_FROM_PICTURE, KEY_CONTENT_TYPE, KEY_CONTENT_SIZE,
				KEY_PARTYID, KEY_TOTAL, KEY_MESSAGE_STATUS }, whereClause,
				whereArgs, null, null, KEY_TIMESTAMP + " ASC");
	}

	public ArrayList<MessageObject> getSelectedEntriesParsed(Long timelineId,
			Long partyId) {
		ArrayList<MessageObject> list = new ArrayList<MessageObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				String searchId;
				String localId;
				if (timelineId != null) {
					searchId = String.valueOf(timelineId);
					localId = cursor.getString(TIMELINE_COLUMN);
				} else {
					searchId = String.valueOf(partyId);
					localId = cursor.getString(PARTYID_COLUMN);
				}

				if (localId.equals(searchId)) {
					list.add(cursorToMessageObject(cursor));
				}
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public ArrayList<MessageObject> getAllEntriesParsed() {
		ArrayList<MessageObject> list = new ArrayList<MessageObject>();

		Cursor cursor = getAllEntries();
		if (cursor.moveToFirst()) {
			do {
				list.add(cursorToMessageObject(cursor));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return list;
	}

	public MessageObject getMessage(String localId) {
		MessageObject message = new MessageObject();
		Cursor cursor = getEntries(KEY_LOCAL_ID + " = ?",
				new String[] { localId });

		if (cursor.moveToFirst()) {
			message = cursorToMessageObject(cursor);
		}
		cursor.close();

		return message;
	}

	private MessageObject cursorToMessageObject(Cursor cursor) {
		MessageObject message = new MessageObject();
		ContentReadResponse content = new ContentReadResponse();
		UserReadAvatarResponse from = new UserReadAvatarResponse();

		message.setId(cursor.getLong(MESSAGE_ID_COLUMN));
		from.setId(cursor.getLong(FROM_COLUMN));
		message.setBody(cursor.getString(BODY_COLUMN));
		content.setId(Long.parseLong(cursor.getString(CONTENT_ID_COLUMN)));
		message.getTimeline().setId(
				Long.parseLong(cursor.getString(TIMELINE_COLUMN)));
		message.setPartyId(Long.parseLong(cursor.getString(PARTYID_COLUMN)));
		from.setName(cursor.getString(FROM_NAME_COLUMN));
		message.setLocalId(cursor.getLong(LOCAL_ID_COLUMN));
		from.setSurname(cursor.getString(FROM_SURNAME_COLUMN));
		from.setPicture(cursor.getLong(FROM_PICTURE_COLUMN));
		content.setContentSize(cursor.getLong(CONTENT_SIZE_COLUMN));
		content.setContentType(cursor.getString(CONTENT_TYPE_COLUMN));

		Date date = new Date(cursor.getLong(TIMESTAMP_COLUMN));
		message.setTimestamp(date.getTime());

		if (message.getId() != 0) {
			message.setTotal(100);
		} else {
			message.setTotal(1);
		}

		String payload = cursor.getString(PAYLOAD_COLUMN);
		if (payload != null) {
			MessageApp msgApp = null;
			try {
				msgApp = JacksonManager.fromJson(payload, MessageApp.class);
				message.setApp(msgApp);
			} catch (Exception e) {
			}
		}

		message.setContent(content);
		message.setFrom(from);

		message.setTotal(cursor.getInt(TOTAL_COLUMN));

		message.incStatusAndUpdate(MessageObject.Status.getValueOf(cursor
				.getString(MESSAGE_STATUS_COLUMN)), context);

		return message;
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
