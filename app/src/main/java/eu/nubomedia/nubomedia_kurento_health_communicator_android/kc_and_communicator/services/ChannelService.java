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

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ChannelService {

	private static final Logger log = LoggerFactory
			.getLogger(ChannelService.class.getSimpleName());

	private static GoogleCloudMessaging gcm;

	public static void createChannel(Context ctx) {
		String regId = getGCMRegId(ctx);
		if (regId.isEmpty()) {
			log.warn("regId is empty. It will get from GCM register");
			return;
		}

		Account ac = AccountUtils.getAccount(ctx, false);
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);

		String userId;
		try {
			userId = am.getUserData(ac, JsonKeys.ID_STORED);
		} catch (Exception e) {
			log.error("Cannot get userId", e);
			return;
		}

		String json;
		try {
			json = registerInServer(ctx, regId, userId);
		} catch (Exception e) {
			log.error("Cannot send regId to server", e);
			return;
		}

		if (json == null) {
			log.error("Cannot get channelId");
			return;
		}

		try {
			JSONObject obj = new JSONObject(json);

			String channelId = obj.getString(JsonKeys.CHANNEL_ID);
			storeRegistrationId(ctx, regId, channelId);

			Intent i = new Intent(ConstantKeys.BROADCAST_SERVER_REGISTER);
			ctx.sendBroadcast(i);
		} catch (JSONException e) {
			log.error("Error processing register result", e);
		}

		ctx.startService(new Intent(ctx.getApplicationContext(),
				CommandGetService.class));
	}

	private static String getGCMRegId(Context ctx) {
		if (!checkPlayServices(ctx)) {
			return "";
		}

		gcm = GoogleCloudMessaging.getInstance(ctx);
		String regId = getRegistrationId(ctx);

		if (regId.isEmpty()) {
			String senderId = ctx.getResources().getString(
					R.string.gcm_sender_id);
			try {
				regId = gcm.register(senderId);
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}

			// Store gcm register id
			Account ac = AccountUtils.getAccount(ctx, false);
			if (ac == null) {
				log.warn("Cannot get account");
			}
			AccountManager am = AccountManager.get(ctx);
			am.setUserData(ac, JsonKeys.GCM_ID, regId);
		}

		return regId;
	}

	private static boolean checkPlayServices(Context ctx) {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(ctx);
		if (resultCode != ConnectionResult.SUCCESS) {
			log.error("This device not support gcm.");

			return false;
		}

		return true;
	}

	private static String getRegistrationId(Context context) {
		Account ac = AccountUtils.getAccount(context, false);
		if (ac == null) {
			log.warn("Cannot get account");
			return "";
		}

		AccountManager am = AccountManager.get(context);
		String regID = am.getUserData(ac, JsonKeys.GCM_ID);
		if (regID == null) {
			regID = "";
		}

		return regID;
	}

	private static String registerInServer(Context ctx, String regId,
			String userId) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(JsonKeys.REGISTER_ID, regId);
			jsonObject.put(JsonKeys.REGISTER_USER_ID, userId);
			jsonObject.put(JsonKeys.REGISTER_TYPE, "gcm");
			jsonObject
					.put(JsonKeys.INSTANCE_ID, Preferences.getInstanceId(ctx));
			jsonObject.put(JsonKeys.LOCAL, Locale.getDefault().getLanguage());
		} catch (JSONException e) {
			log.error("Cannot create JSON", e);
			return null;
		}

		HttpResp<String> resp = HttpManager.sendPostString(ctx,
				ctx.getString(R.string.url_get_channel), jsonObject.toString());

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK && code != HttpStatus.SC_CREATED) {
			return null;
		}

		return resp.getBody();
	}

	private static void storeRegistrationId(Context context, String regId,
			String channelId) {
		AccountManager am = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account ac = AccountUtils.getAccount(context, true);
		am.setUserData(ac, JsonKeys.REGISTER_ID, regId);
		am.setUserData(ac, JsonKeys.CHANNEL_ID, channelId);
	}

	public static String getChannelId(Context context) {
		AccountManager am = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account ac = AccountUtils.getAccount(context, false);

		if (ac == null) {
			log.warn("Account is null. Channel id can not be get");
			return null;
		}

		String channelId = am.getUserData(ac, JsonKeys.CHANNEL_ID);
		if ((channelId == null || channelId.length() == 0)) {
			return null;
		} else {
			return channelId;
		}
	}
}
