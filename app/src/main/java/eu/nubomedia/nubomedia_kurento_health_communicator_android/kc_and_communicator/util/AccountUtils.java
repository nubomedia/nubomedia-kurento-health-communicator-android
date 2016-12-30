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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

public class AccountUtils {

	private static final Logger log = LoggerFactory
			.getLogger(AccountUtils.class.getSimpleName());

	public static void resetAuthToken(Context ctx) {
		Account account = AccountUtils.getAccount(ctx, false);
		if (account == null) {
			log.warn("Cannot reset authtoken. Account is null.");
			return;
		}

		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);

		String authToken = am.getUserData(account, JsonKeys.AUTH_TOKEN);

		am.setUserData(account, JsonKeys.AUTH_TOKEN,
				ConstantKeys.STRING_DEFAULT);
		am.invalidateAuthToken(account.type, authToken);
	}

	public static void requestContacts(final Context ctx) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return AppUtils.searchLocalContact(ctx.getApplicationContext(),
						true);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void storeUserData(Account account, AccountManager am,
			LoginValues loginValues, String authToken) {
		am.setUserData(account, JsonKeys.ID_STORED, loginValues.getUser()
				.getId().toString());
		am.setUserData(account, JsonKeys.NAME, loginValues.getUser().getName());
		am.setUserData(account, JsonKeys.SURNAME, loginValues.getUser()
				.getSurname());
		am.setUserData(account, JsonKeys.PICTURE, loginValues.getUser()
				.getPicture().toString());
		am.setUserData(account, JsonKeys.PHONE, loginValues.getUser()
				.getPhone());
		am.setUserData(account, JsonKeys.EMAIL, loginValues.getUser()
				.getEmail());
		am.setUserData(account, JsonKeys.QOS_FLAG,
				String.valueOf(loginValues.getUser().getQos()));
		am.setUserData(account, JsonKeys.AUTH_TOKEN, authToken);
	}

	public static Account getAccount(Context ctx, boolean launchRequest) {
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account[] accounts = am.getAccountsByType(ctx
				.getString(R.string.account_type));

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(ctx.getApplicationContext());
		String accountName = pref.getString(
				ctx.getResources().getString(R.string.account_name),
				ConstantKeys.STRING_DEFAULT);
		if (accounts == null || accounts.length == 0) {
			if (launchRequest) {

			}

			return null;
		} else if (accounts.length == 1) {
			Account account = accounts[0];
			if (accountName.equals(account.name)) {
				return account;
			} else {
				storeAccountIntoPreferences(ctx.getApplicationContext(),
						account);
				return account;
			}
		} else {
			for (Account a : accounts) {
				if (accountName.equals(a.name)) {
					return a;
				}
			}
			showAccountSelector(ctx, accounts);
			return null;
		}
	}

	private static void showAccountSelector(final Context context,
			final Account[] accounts) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.main_select_account);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// finish();
			}
		});

		CharSequence[] options = new CharSequence[accounts.length];

		for (int i = 0; i < accounts.length; i++) {
			options[i] = accounts[i].name;
		}

		builder.setSingleChoiceItems(options, 0,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int position) {
						int selectedPosition = ((AlertDialog) dialog)
								.getListView().getCheckedItemPosition();
						storeAccountIntoPreferences(context,
								accounts[selectedPosition]);
						dialog.dismiss();
					}
				});

		builder.show();
	}

	protected static void storeAccountIntoPreferences(Context context,
			Account account) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(context.getResources()
				.getString(R.string.account_name), account.name);
		editor.commit();
	}

	public static void removeAllAccounts(Context ctx) {
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account[] accounts = am.getAccountsByType(ctx
				.getString(R.string.account_type));

		for (Account ac : accounts) {
			am.removeAccount(ac, null, null);
		}
	}

	public static String getAuthtoken(Context context) {
		Account ac = AccountUtils.getAccount(context, false);

		if (ac == null) {
			log.warn("Cannot get any account");
			return null;
		}

		AccountManager am = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);

		String authToken = null;
		try {
			authToken = am.blockingGetAuthToken(ac,
					context.getString(R.string.account_type), true);
		} catch (Exception e) {
			log.warn("Authtoken not stored.");
		}

		return authToken;
	}

	public static String computeMD5(String str) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("Error creating MD5", e);
			return null;
		}

		byte[] array = md.digest(str.getBytes());
		StringBuffer sb = new StringBuffer();

		for (byte b : array) {
			sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
		}

		return sb.toString().toLowerCase();
	}

	public static boolean getQosFlag(Context ctx) {
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account ac = getAccount(ctx, false);
		if (am != null && ac != null) {
			String qosFlag = am.getUserData(ac, JsonKeys.QOS_FLAG);
			log.trace("QoSservice: getting QosFlag->" + qosFlag);
			return Boolean.parseBoolean(qosFlag);
		} else {
			log.trace("QoSservice: QosFlag not exists for user-> false");
			return false;
		}
	}
}
