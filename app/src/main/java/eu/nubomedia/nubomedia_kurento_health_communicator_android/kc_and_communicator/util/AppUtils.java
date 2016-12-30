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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.AuthClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.MessagingClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Settings.Secure;
import android.util.Log;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.agenda.services.pojo.UserEdit;

public class AppUtils {
	public static final int ACTION_TAKE_PICTURE = 100;
	public static final int ACTION_RECORD_VIDEO = 101;
	public static final int ACTION_GALLERY = 103;
	public static final int RETURN_SETUP = 104;
	public static final int ACTION_RESPONSE = 105;
	public static final int RETURN_ITEM_SELECTED = 106;
	public static final int GROUP_ADMIN = 107;

	private static final Logger log = LoggerFactory.getLogger(AppUtils.class
			.getSimpleName());

	public static ArrayList<String> listOfDownloads = null;

	public static ArrayList<String> getlistOfDownload() {
		if (listOfDownloads == null) {
			listOfDownloads = new ArrayList<String>();
		}
		return listOfDownloads;
	}

	public static void getUserInfo(Context context) {
		try {
			Account account = AccountUtils.getAccount(
					context.getApplicationContext(), false);
			AccountManager am = (AccountManager) context
					.getSystemService(Context.ACCOUNT_SERVICE);

			AuthClientService cs = new AuthClientService(
					context.getApplicationContext());

			String returned = cs.getUserData(am.getUserData(account,
					JsonKeys.ID_STORED));
			if (returned != null) {
				JSONObject returnJson = new JSONObject(returned);
				am.setUserData(account, JsonKeys.ID_STORED, String.valueOf(Long
						.parseLong(returnJson.getString(JsonKeys.ID))));
				am.setUserData(account, JsonKeys.NAME,
						returnJson.getString(JsonKeys.NAME));
				am.setUserData(account, JsonKeys.SURNAME,
						returnJson.getString(JsonKeys.SURNAME));
				am.setUserData(account, JsonKeys.PICTURE,
						ConstantKeys.STRING_DEFAULT);
				am.setUserData(account, JsonKeys.PHONE, String.valueOf(Long
						.parseLong(returnJson.getString(JsonKeys.PHONE))));
				am.setUserData(account, JsonKeys.EMAIL,
						returnJson.getString(JsonKeys.EMAIL));

				String aux = String.valueOf(Long.parseLong(returnJson
						.getString(JsonKeys.ID)));

				File toDelete = new File(FileUtils.getDir() + "/" + aux
						+ ConstantKeys.AVATAR + ConstantKeys.EXTENSION_JPG);

				if (toDelete.exists()) {
					toDelete.delete();
				}

			}
		} catch (Exception e) {
			return;
		}
	}

	public static JSONArray getMessagesFromServer(final Context ctx,
			final String timeline, final boolean retry, final String lastMessage) {
		MessagingClientService cs = new MessagingClientService(
				ctx.getApplicationContext());

		try {
			return cs.retrieveMessages(String.valueOf(timeline), lastMessage);
		} catch (Exception e) {
			log.error("Error retrieving messages", e);
			if (retry) {
				return getMessagesFromServer(ctx, timeline, false, lastMessage);
			} else {
				return null;
			}
		}
	}

	public static String humanReadableByteCount(long bytes, boolean si) {
		int unit = si ? 1000 : 1024;
		if (bytes < unit)
			return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
				+ (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public static boolean updateUser(final Context ctx, final String path,
			UserEdit user) {
		JSONObject jsonUserInfo = new JSONObject();
		try {
			jsonUserInfo.put(JsonKeys.ID, user.getId());
			jsonUserInfo.put(JsonKeys.NAME, user.getName());
			jsonUserInfo.put(JsonKeys.SURNAME, user.getSurname());
		} catch (JSONException e) {
			log.error("Cannot update user", e);
			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonUserInfo.toString(),
				Command.METHOD_UPDATE_USER, path);
	}

	public static boolean createGroup(final Context ctx, final String path,
			GroupCreate group, AccountId accountId, boolean retry) {
		JSONObject jsonGroupInfo = new JSONObject();
		try {
			JSONObject groupTag = JsonParser.GroupCreateToJson(group);
			JSONObject accountTag = JsonParser.AccountIdToJson(accountId);
			jsonGroupInfo.put(JsonKeys.GROUP, groupTag);
			jsonGroupInfo.put(JsonKeys.ACCOUNT, accountTag);
		} catch (JSONException e) {
			log.error("Cannot create group", e);
			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonGroupInfo.toString(),
				Command.METHOD_CREATE_GROUP, path);
	}

	public static boolean deleteGroup(final Context ctx, final String path,
			final String groupId, boolean retry) {
		JSONObject jsonGroupInfo = new JSONObject();
		try {
			jsonGroupInfo.put(JsonKeys.ID, groupId);
		} catch (JSONException e) {
			log.error("Cannot delete group", e);

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonGroupInfo.toString(),
				Command.METHOD_DELETE_GROUP, path);
	}

	public static GroupObject getGroupByName(final Context ctx, String groupName) {
		ArrayList<GroupObject> groups = DataBasesAccess.getInstance(ctx)
				.GroupsDataBase(DataBasesAccess.READ, null);
		for (GroupObject g : groups) {
			if(g.getName().equals(groupName)) {
				return g;
			}
		}

		return null;
	}

	public static boolean addUserGroup(final Context ctx, final String path,
			String groupId, Long userId, boolean retry) {
		JSONObject jsonComplete = new JSONObject();
		try {
			JSONObject groupTag = new JSONObject();
			groupTag.put(JsonKeys.ID, groupId);
			JSONObject userTag = new JSONObject();
			userTag.put(JsonKeys.ID, userId);
			jsonComplete.put(JsonKeys.GROUP, groupTag);
			jsonComplete.put(JsonKeys.USER, userTag);
		} catch (JSONException e) {
			log.error("Cannot add user to group");

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonComplete.toString(),
				Command.METHOD_ADD_GROUP_MEMBER, path);
	}

	public static boolean deleteUserGroup(final Context ctx, final String path,
			String groupId, Long userId, boolean retry) {
		JSONObject jsonComplete = new JSONObject();
		try {
			JSONObject groupTag = new JSONObject();
			groupTag.put(JsonKeys.ID, groupId);
			JSONObject userTag = new JSONObject();
			userTag.put(JsonKeys.ID, userId);
			jsonComplete.put(JsonKeys.GROUP, groupTag);
			jsonComplete.put(JsonKeys.USER, userTag);
		} catch (JSONException e) {
			log.error("Cannot delete user to group");

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonComplete.toString(),
				Command.METHOD_REMOVE_GROUP_MEMBER, path);
	}

	public static boolean addAdminGroup(final Context ctx, final String path,
			String groupId, Long userId, boolean retry) {
		JSONObject jsonComplete = new JSONObject();
		try {
			JSONObject groupTag = new JSONObject();
			groupTag.put(JsonKeys.ID, groupId);
			JSONObject userTag = new JSONObject();
			userTag.put(JsonKeys.ID, userId);
			jsonComplete.put(JsonKeys.GROUP, groupTag);
			jsonComplete.put(JsonKeys.USER, userTag);
		} catch (JSONException e) {
			log.error("Cannot add admin to group");

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonComplete.toString(),
				Command.METHOD_ADD_GROUP_ADMIN, path);
	}

	public static boolean deleteAdminGroup(final Context ctx,
			final String path, String groupId, Long userId, boolean retry) {
		JSONObject jsonComplete = new JSONObject();
		try {
			JSONObject groupTag = new JSONObject();
			groupTag.put(JsonKeys.ID, groupId);
			JSONObject userTag = new JSONObject();
			userTag.put(JsonKeys.ID, userId);
			jsonComplete.put(JsonKeys.GROUP, groupTag);
			jsonComplete.put(JsonKeys.USER, userTag);
		} catch (JSONException e) {
			log.error("Cannot delete admin to group");

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(jsonComplete.toString(),
				Command.METHOD_REMOVE_GROUP_ADMIN, path);
	}

	public static UserObject getContactByPhone(final Context ctx,
											   String contactPhone) {
		ArrayList<UserObject> allUsers = DataBasesAccess.getInstance(
				ctx.getApplicationContext()).UsersDataBase(
				DataBasesAccess.READ, null);

		for (UserObject user : allUsers) {
			if ((user.getPhone() != null) && (user.getPhone().equals(contactPhone))) {
				return user;
			}
		}

		return null;
	}

	public static boolean sendMessage(final Context ctx, final String path,
			String msg, Long userId, String groupId, boolean retry) {
		JSONObject JsonGroupMsg = new JSONObject();
		try {
			JsonGroupMsg.put(JsonKeys.FROM, userId);
			JsonGroupMsg.put(JsonKeys.TO, groupId);
			JsonGroupMsg.put(JsonKeys.BODY, msg);
		} catch (JSONException e) {
			log.error("Cannot send message", e);

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(JsonGroupMsg.toString(),
				Command.METHOD_SEND_MESSAGE_TO_GROUP, path);
	}

	public static boolean deleteTimeline(final Context ctx, final String path,
			String type, Long userId, Long groupId, boolean retry) {
		JSONObject paramsTag = new JSONObject();
		try {
			paramsTag.put(JsonKeys.OWNER_ID, userId);
			JSONObject partyTag = new JSONObject();
			partyTag.put(JsonKeys.ID, groupId);
			partyTag.put(JsonKeys.TYPE, type);
			paramsTag.put(JsonKeys.PARTY, partyTag);
		} catch (JSONException e) {
			log.error("Cannot delete timeline", e);

			return false;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		return cs.createCommand(paramsTag.toString(),
				Command.METHOD_DELETE_TIMELINE, path);
	}

	public static String actionOverTimeline(final Context ctx, String action,
			Long partyId, String type, String name, boolean retry) {
		Account account = AccountUtils.getAccount(ctx.getApplicationContext(),
				true);
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);
		String ownerId = am.getUserData(account, JsonKeys.ID_STORED);

		JSONObject jsonTimelineInfo = new JSONObject();
		try {
			jsonTimelineInfo.put(JsonKeys.OWNER_ID, ownerId);

			JSONObject partyInfo = new JSONObject();
			partyInfo.put(JsonKeys.ID, partyId);
			partyInfo.put(JsonKeys.TYPE, type);
			if (name != null) {
				partyInfo.put(JsonKeys.NAME, name);
			}
			jsonTimelineInfo.put(JsonKeys.PARTY, partyInfo);

		} catch (JSONException e) {
			return ConstantKeys.SENDING_FAIL;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		boolean result = cs.createCommand(jsonTimelineInfo.toString(), action,
				null);
		if (result) {
			return ConstantKeys.SENDING_OK;
		} else {
			return ConstantKeys.SENDING_OFFLINE;
		}
	}

	public static String editGroup(final Context ctx, final String path,
			String groupId, String name, String phone, boolean retry) {
		JSONObject object = new JSONObject();
		try {
			object.put(JsonKeys.ID, Long.valueOf(groupId));
			object.put(JsonKeys.NAME, name);
			object.put(JsonKeys.PHONE, phone);
		} catch (JSONException e) {
			return ConstantKeys.SENDING_FAIL;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		boolean result = cs.createCommand(object.toString(),
				Command.METHOD_UPDATE_GROUP, path);
		if (result) {
			return ConstantKeys.SENDING_OK;
		} else {
			return ConstantKeys.SENDING_OFFLINE;
		}
	}

	private static ArrayList<String> getNumbers(Context context) {
		ArrayList<String> list = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(Phone.CONTENT_URI,
					null, null, null, null);
			int phoneNumberIdx = cursor.getColumnIndex(Phone.NUMBER);
			cursor.moveToFirst();
			do {
				String phoneNumber = cursor.getString(phoneNumberIdx);
				list.add(phoneNumber);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			Log.d(context.getPackageName(), e.toString());
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static String searchLocalContact(final Context ctx, boolean retry) {
		ArrayList<String> localNumbers = getNumbers(ctx);
		JSONArray array = new JSONArray();
		JSONObject object = new JSONObject();
		try {
			for (int i = 0; i < localNumbers.size(); i++) {
				array.put(localNumbers.get(i));
			}
			object.put(JsonKeys.LOCAL_PHONES, array);
		} catch (JSONException e) {
			return ConstantKeys.SENDING_FAIL;
		}

		CommandStoreService cs = new CommandStoreService(
				ctx.getApplicationContext());

		boolean result = cs.createCommand(object.toString(),
				Command.METHOD_SEARCH_LOCAL_CONTACT, null);
		if (result) {
			return ConstantKeys.SENDING_OK;
		} else {
			return ConstantKeys.SENDING_OFFLINE;
		}
	}

	public static void purgeApp(Context context) {
		DataBasesAccess.getInstance(context.getApplicationContext())
				.MessagesDataBaseDeleteAll();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteAvatarDataBase();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteCommandsToSendDataBase();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteUsersDataBase();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteGroupsDataBase();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteGroupMembersDataBase();
		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteTimelinesDataBase();

		File dir = new File(FileUtils.getDir());
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				new File(dir, children[i]).delete();
			}
		}

		AccountUtils.resetAuthToken(context);
		HttpManager.shutdownHttpClients(context);
	}

	public static void CancelNotification(Context context) {
		NotificationManager nMgr = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nMgr.cancelAll();
	}

	public static void setDefaults(String key, String value, Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		editor.commit();
	}

	public static String getDefaults(String key, Context context) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		return preferences.getString(key, "false");
	}

	public static String getAuthToken(Context context) {
		Context appContext = context.getApplicationContext();
		Account account = AccountUtils.getAccount(appContext, false);
		AccountManager am = (AccountManager) appContext
				.getSystemService(Context.ACCOUNT_SERVICE);

		String authToken = null;
		try {
			authToken = am.blockingGetAuthToken(account,
					appContext.getString(R.string.account_type), true);
		} catch (Exception e) {
			log.error("Cannot get authtoken", e);
		}

		return authToken;
	}

	public static Class<?> getClassByName(Context context, int classNameResource) {
		try {
			String className = context.getResources().getString(
					classNameResource);
			return getClassByName(className);
		} catch (NotFoundException e) {
			log.warn("Cannot get the className.", e);
			return null;
		}
	}

	public static Class<?> getClassByName(String className) {
		log.debug("className: " + className);
		Class<?> activityClass = null;
		try {
			activityClass = Class.forName(className);
		} catch (ClassNotFoundException e) {
			log.warn("Cannot get the class: " + className, e);
		}
		return activityClass;
	}

	public static boolean getResource(Context context, int resId,
			boolean defaultValue) {
		boolean res = defaultValue;
		if (context == null)
			return res;
		try {
			res = context.getResources().getBoolean(resId);
		} catch (Exception e) {
			log.warn("Cannot get the resource.", e);
		}
		return res;
	}

	public static Map<String, Boolean> getCallConfigMap(Context context){
		Map<String,Boolean> map = new HashMap<String, Boolean>();
		if(context != null){
			map.put(ConstantKeys.VIDEOCALL_INIT_SOUND_OFF_KEY,
					getResource(context, R.bool.videocall_init_sound_off, false));
			map.put(ConstantKeys.VIDEOCALL_INIT_VIDEO_OFF_KEY,
					getResource(context, R.bool.videocall_init_video_off, false));
		}		
		return map;		
	}

	public static String generateInstanceId(Context context) {
		String androidId = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		if (androidId == null || androidId.isEmpty())
			androidId = UUID.randomUUID().toString();

		String uuid = null;
		try {
			uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"))
					.toString();
		} catch (UnsupportedEncodingException e) {
			uuid = GeneralUtils.generateUniqueId();
		}

		String appName = context.getString(R.string.app_name);

		return appName + "-" + uuid;
	}
}
