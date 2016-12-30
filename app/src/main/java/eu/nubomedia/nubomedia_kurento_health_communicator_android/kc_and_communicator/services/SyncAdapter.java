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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services;

import java.util.ArrayList;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.ContactManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.text.TextUtils;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	private final static Logger log = LoggerFactory.getLogger(SyncAdapter.class
			.getSimpleName());

	private final static String SYNC_MARK = "com.kurento.kas.agenda.syncadapter.SYNC_MARK";
	private final static String PROFILE_SYNC_MARK = "com.kurento.kas.agenda.syncadapter.PROFILE_SYNC_MARK";

	private final AccountManager am;

	public SyncAdapter(Context context) {
		super(context, true, true);

		am = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		long mark = getSyncMark(account);

		if (mark == 0) {
			ContactManager.setAccountContactsVisibility(getContext(), account,
					true);
		}

		if (syncResult.stats.numAuthExceptions != 0) {
			syncResult.stats.numAuthExceptions = 0;
		}

		ArrayList<UserObject> contactList = new ArrayList<UserObject>();

		contactList = DataBasesAccess.getInstance(
				getContext().getApplicationContext()).UsersDataBase(
				DataBasesAccess.READ, null);

		ContactManager.updatePhoneAgendaContacts(getContext(), account,
				syncResult.stats, contactList);

		log.debug(ConstantKeys.STRING_DEFAULT + syncResult.stats);

		synchronizeProfile(account, provider, syncResult);
	}

	private void synchronizeProfile(Account account,
			ContentProviderClient provider, SyncResult syncResult) {
		long mark = getProfileSyncMark(account);
	}

	private long getSyncMark(Account account) {
		String mark = am.getUserData(account, SYNC_MARK);
		if (!TextUtils.isEmpty(mark)) {
			try {
				return Long.parseLong(mark);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private long getProfileSyncMark(Account account) {
		String mark = am.getUserData(account, PROFILE_SYNC_MARK);
		if (!TextUtils.isEmpty(mark)) {
			try {
				return Long.parseLong(mark);
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}
}
