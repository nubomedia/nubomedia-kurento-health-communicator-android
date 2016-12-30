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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncStats;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;

public class ContactManager {

	private static Logger log = LoggerFactory.getLogger(ContactManager.class
			.getSimpleName());

	public static void setAccountContactsVisibility(Context context,
			Account account, boolean visible) {
		ContentValues values = new ContentValues();
		values.put(RawContacts.ACCOUNT_NAME, account.name);
		values.put(RawContacts.ACCOUNT_TYPE,
				context.getString(R.string.account_type));
		values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

		context.getContentResolver().insert(Settings.CONTENT_URI, values);
	}

	public static void updatePhoneAgendaContacts(Context context,
			Account account, SyncStats stats, ArrayList<UserObject> contactList) {
		final HashMap<String, Long> contacts = new HashMap<String, Long>();
		final HashSet<String> noDelete = new HashSet<String>();

		final ContentResolver resolver = context.getContentResolver();
		final Cursor cursor = resolver.query(
				RawContacts.CONTENT_URI, new String[] {
						RawContacts._ID,
						RawContacts.SOURCE_ID },
				RawContacts.ACCOUNT_NAME + "=? AND "
						+ RawContacts.ACCOUNT_TYPE + "=?",
				new String[] { account.name, account.type }, null);

		int count = cursor.getCount();
		if (count > 0) {
			for (int i = 0; i < count; i++) {
				cursor.moveToPosition(i);

				String username = cursor.getString(1);
				Long id = cursor.getLong(0);

				if (contacts.containsKey(username)) {
					deleteContact(context, stats, id);
				} else {
					contacts.put(username, id);
				}
			}
		}

		cursor.close();

		for (int i = 0; i < contactList.size(); i++) {
			UserObject contact = contactList.get(i);
			addOrUpdateContact(context, account, stats, contact);
		}

		HashSet<String> deleteContacts = new HashSet<String>();
		deleteContacts.addAll(contacts.keySet());
		deleteContacts.removeAll(noDelete);

		for (String username : deleteContacts) {
			log.debug("Deleting contact " + username);
			deleteContact(context, stats, contacts.get(username));
		}
	}

	private static void addOrUpdateContact(Context context, Account account,
			SyncStats stats, UserObject contact) {
		if (contact == null)
			return;

		// TODO We need to check for an update or an insert but for now
		// we are going to add the contacts
		addContact(context, account, stats, contact);
	}

	private static void deleteContact(Context context, SyncStats stats,
			long rawcontactid) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		ops.add(ContentProviderOperation
				.newDelete(
						ContactsContract.Data.CONTENT_URI
								.buildUpon()
								.appendQueryParameter(
										ContactsContract.CALLER_IS_SYNCADAPTER,
										"true").build())
				.withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=?",
						new String[] { String.valueOf(rawcontactid) }).build());
		ops.add(ContentProviderOperation
				.newDelete(
						RawContacts.CONTENT_URI
								.buildUpon()
								.appendQueryParameter(
										ContactsContract.CALLER_IS_SYNCADAPTER,
										"true").build())
				.withSelection(RawContacts._ID + "=?",
						new String[] { String.valueOf(rawcontactid) }).build());
		ops.add(ContentProviderOperation
				.newDelete(
						ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI
								.buildUpon()
								.appendQueryParameter(
										ContactsContract.CALLER_IS_SYNCADAPTER,
										"true").build())
				.withSelection(ContactsContract.Profile._ID + "=?",
						new String[] { String.valueOf(rawcontactid) }).build());

		try {
			ContentProviderResult[] results = context.getContentResolver()
					.applyBatch(ContactsContract.AUTHORITY, ops);
			stats.numDeletes++;
			if (results != null) {
				for (ContentProviderResult result : results) {
					stats.numEntries += result.count;
				}
			}
		} catch (Throwable e) {
			log.error("Exception encoutered while deleting contact: " + e, e);
		}
	}

	private static String getContactDisplayName(UserObject contact) {
		String displayName;

		displayName = contact.getName();

		if (displayName == null || displayName.isEmpty())
			displayName = contact.getSurname();
		else if (contact.getSurname() != null || contact.getSurname().isEmpty())
			displayName += " " + contact.getSurname();

		if (displayName == null || displayName.isEmpty())
			displayName = contact.getName();

		return displayName;
	}

	private static void addContact(Context context, Account account,
			SyncStats stats, UserObject contact) {
		String displayName = getContactDisplayName(contact);

		Uri uri = RawContacts.CONTENT_URI;

		if (account.name.equals(contact.getName()))
			uri = ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI;

		uri = uri
				.buildUpon()
				.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
						"true").build();

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ops.add(ContentProviderOperation
				.newInsert(uri)
				.withValue(RawContacts.ACCOUNT_TYPE,
						context.getString(R.string.account_type))
				.withValue(RawContacts.ACCOUNT_NAME,
						account.name)
				.withValue(RawContacts.ACCOUNT_TYPE,
						account.type)
				.withValue(RawContacts.DIRTY, false)
				.withValue(RawContacts.SOURCE_ID,
						contact.getName()).build());
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				.withValue(
						ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
						displayName).build());
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(
						ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,
						contact.getPhone())
				.withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
						ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
				.build());

		Bitmap bitmap = getContactPicture(context, contact);

		if (bitmap != null) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] photoBuffer = stream.toByteArray();

			ops.add(ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI)
					.withValueBackReference(
							ContactsContract.Data.RAW_CONTACT_ID, 0)
					.withValue(
							ContactsContract.Data.MIMETYPE,
							ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
					.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO,
							photoBuffer).build());

			bitmap.recycle();
		}

		try {
			ContentProviderResult[] results = context.getContentResolver()
					.applyBatch(ContactsContract.AUTHORITY, ops);

			stats.numInserts++;
			if (results != null) {
				stats.numEntries += results.length;
			}
		} catch (Throwable e) {
			log.error("Exception encoutered while inserting contact: " + e, e);
		}
	}

	private static Bitmap getContactPicture(Context context, UserObject contact) {
		// TODO here we goint to take the picture from the url now we
		// are going to take a resource example
		return BitmapFactory.decodeResource(context.getResources(),
				R.drawable.ic_contact_picture);
	}
}
