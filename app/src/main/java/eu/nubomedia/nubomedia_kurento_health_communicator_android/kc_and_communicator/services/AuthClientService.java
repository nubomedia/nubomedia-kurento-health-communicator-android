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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.LoginValues;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.kurento.agenda.services.pojo.AccountReadInfoResponse;
import com.kurento.agenda.services.pojo.UserCreate;
import com.kurento.agenda.services.pojo.UserReadResponse;

public class AuthClientService {

	private static final Logger log = LoggerFactory
			.getLogger(AuthClientService.class.getSimpleName());

	private static final String USER_PART_NAME = "user";
	private static final String PICTURE_PART_NAME = "picture";

	private final Context context;

	public AuthClientService(Context context) {
		this.context = context;
	}

	public LoginValues login(String username, String password) {
		HttpManager.setCredentials(context, username, password);

		HttpResp<String> resp;
		try {
			resp = HttpManager.sendGetString(context,
					context.getString(R.string.url_get_user_login), false);
		} catch (Exception e) {
			log.error("Cannot login", e);
			return null;
		}

		int code = resp.getCode();

		if (code != HttpStatus.SC_OK) {
			log.error("Error login ({})", code);
			return null;
		}

		String token = ConstantKeys.STRING_DEFAULT;
		for (Header header : resp.getHeaders(HttpManager.SET_COOKIE_HEADER)) {
			int index = header.getValue().indexOf(HttpManager.COOKIE_NAME);
			if (index != -1) {
				String value = header.getValue().substring(
						index + HttpManager.COOKIE_NAME.length());
				token = value.split(";")[0];
			}
		}

		UserReadResponse user;
		try {
			user = new UserReadResponse();
			user = JsonParser.jsonToUserRead(resp.getBody());
		} catch (JSONException e) {
			log.error("Cannot generate user from JSON", e);
			e.printStackTrace();
			return null;
		}

		log.debug("Login success with token: {}", token);

		LoginValues loginValues = new LoginValues();
		loginValues.setToken(token);
		loginValues.setUser(user);

		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "true", context);

		return loginValues;
	}

	public void checkAccountOnResume() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				AccountReadInfoResponse result;
				try {
					result = new AuthClientService(context)
							.checkAccount(Preferences.getAccountId(context
									.getApplicationContext()));
				} catch (Exception e) {
					log.error("Cannot check account", e);
					return null;
				}

				Preferences.setAccountName(context.getApplicationContext(),
						result.getName());
				Preferences.setUserAutoRegister(
						context.getApplicationContext(),
						result.getUserAutoregister());
				Preferences.setGroupAutoRegister(
						context.getApplicationContext(),
						result.getGroupAutoregister());

				return null;
			}
		}.execute();
	}

	public AccountReadInfoResponse checkAccount(String accountId)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		HttpResp<String> resp = HttpManager.sendGetString(context,
				context.getString(R.string.url_account, accountId));

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK) {
			String msg = "Cannot get user data (" + code + ")";
			log.warn(msg);
			throw new TransportException(msg);
		}

		AccountReadInfoResponse account = null;
		try {
			account = JsonParser.jsonToAccountInfo(resp.getBody());
		} catch (JSONException e) {
			log.error("Some error parsing json from account", e);
		}

		return account;
	}

	public boolean createUser(String accountId, UserCreate user)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(JsonKeys.PASSWORD, user.getPassword());
			jsonObject.put(JsonKeys.NAME, user.getName());
			jsonObject.put(JsonKeys.SURNAME, user.getSurname());
			jsonObject.put(JsonKeys.PHONE, user.getPhone());
			jsonObject.put(JsonKeys.EMAIL, user.getEmail());
			jsonObject.put(JsonKeys.PHONE_REGION, ConstantKeys.ES);

		} catch (JSONException e) {
			log.error("Cannot create JSON", e);
			return false;
		}

		HttpResp<String> resp = HttpManager.sendPostString(context,
				context.getString(R.string.url_create_user, accountId),
				jsonObject.toString());

		return resp.getCode() == HttpStatus.SC_CREATED;
	}

	public boolean createUser(String accountId, UserCreate user,
			String contentPath) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		log.debug("Create user {}", user.getName());
		JSONObject object = new JSONObject();
		try {
			object.put(JsonKeys.PASSWORD, user.getPassword());
			object.put(JsonKeys.NAME, user.getName());
			object.put(JsonKeys.SURNAME, user.getSurname());
			object.put(JsonKeys.PHONE, user.getPhone());
			object.put(JsonKeys.EMAIL, user.getEmail());
			object.put(JsonKeys.PHONE_REGION, ConstantKeys.ES);

		} catch (JSONException e) {
			log.debug("Error creating object");
			return false;
		}
		String json = object.toString();

		String charset = HTTP.UTF_8;

		MultipartEntity mpEntity = new MultipartEntity();
		try {
			mpEntity.addPart(
					USER_PART_NAME,
					new StringBody(json,
							HttpManager.CONTENT_TYPE_APPLICATION_JSON, Charset
									.forName(charset)));
		} catch (UnsupportedEncodingException e) {
			String msg = "Cannot use " + charset + "as entity";
			log.error(msg, e);
			throw new TransportException(msg);
		}

		File content = new File(contentPath);
		mpEntity.addPart(PICTURE_PART_NAME, new FileBody(content,
				ConstantKeys.TYPE_IMAGE));

		HttpResp<Void> resp = HttpManager.sendPostVoid(context,
				context.getString(R.string.url_create_user, accountId),
				mpEntity);

		return resp.getCode() == HttpStatus.SC_CREATED;
	}

	public boolean getRecoveryCode(String userId) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(JsonKeys.USER_IDENTITY, userId);
		} catch (JSONException e) {
			log.error("Cannot create JSON", e);
			return false;
		}

		HttpResp<Void> resp = HttpManager.sendPostVoid(context,
				context.getString(R.string.url_get_recovery_code),
				jsonObject.toString());

		return resp.getCode() == HttpStatus.SC_NO_CONTENT;
	}

	public boolean sendRecoveryCode(String password, String recoveryCode)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(JsonKeys.NEW_PASSWORD, password);
			jsonObject.put(JsonKeys.SECURITY_CODE, recoveryCode);
		} catch (JSONException e) {
			log.error("Cannot create JSON", e);
			return false;
		}

		HttpResp<Void> resp = HttpManager.sendPostVoid(context,
				context.getString(R.string.url_send_recovery_code),
				jsonObject.toString());

		return resp.getCode() == HttpStatus.SC_NO_CONTENT;
	}

	public String getUserData(String userId) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		HttpResp<String> resp = HttpManager.sendGetString(context,
				context.getString(R.string.url_get_user_data) + "/" + userId);

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK) {
			String msg = "Cannot get user data (" + code + ")";
			log.warn(msg);
			throw new TransportException(msg);
		}

		return resp.getBody();
	}

	public Bitmap getAvatar(String contentId) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		HttpResp<Bitmap> resp = HttpManager.sendGetBimap(context,
				context.getString(R.string.url_user_avatar, contentId),
				contentId);

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK) {
			String msg = "Cannot get avatar (" + code + ")";
			log.warn(msg);
			throw new TransportException(msg);
		}

		return resp.getBody();
	}

}
