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
import java.util.ArrayList;

import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.AvatarObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;

import com.kurento.agenda.services.pojo.UserReadResponse;

public class CommandRunService {

	private static final Logger log = LoggerFactory
			.getLogger(CommandRunService.class.getSimpleName());

	public static boolean runUpdateUser(Context context, String jsonCommand) {
		UserReadResponse user = null;
		try {
			user = JsonParser.jsonToUserRead(jsonCommand);
		} catch (JSONException e) {
			log.error("Cannot update user.", e);
			return false;
		}

		// We can delete the picture to refresh it
		File toDelete = new File(FileUtils.getDir() + "/"
				+ user.getPicture().toString() + ConstantKeys.AVATAR
				+ ConstantKeys.EXTENSION_JPG);

		if (toDelete.exists()) {
			toDelete.delete();
		}

		Account account = AccountUtils.getAccount(context, false);

		if (account == null) {
			log.warn("Cannot update user. There is not any account.");
			return false;
		}

		AccountManager am = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);
		am.setUserData(account, JsonKeys.ID, user.getId().toString());
		am.setUserData(account, JsonKeys.NAME, user.getName());
		am.setUserData(account, JsonKeys.SURNAME, user.getSurname());

		String oldPicture = am.getUserData(account, JsonKeys.PICTURE);
		if (!oldPicture.equals(user.getPicture().toString())) {
			// Delete avatar to force downloading it again
			ImageDownloader downloader = new ImageDownloader();
			downloader.eraseUserAvatar(user.getId().toString());
		}

		am.setUserData(account, JsonKeys.PICTURE, user.getPicture().toString());
		am.setUserData(account, JsonKeys.PHONE, user.getPhone());
		am.setUserData(account, JsonKeys.EMAIL, user.getEmail());
		am.setUserData(account, JsonKeys.QOS_FLAG,
				String.valueOf(user.getQos()));

		return true;
	}

	public static boolean runUpdateGroup(Context context, String jsonCommand) {

		GroupObject group = null;
		try {
			group = JsonParser.jsonToGroup(jsonCommand);
		} catch (JSONException e) {
			return false;
		}

		// Here we can delete the group avatar, to assure the refresh
		File toDelete = new File(FileUtils.getDir() + "/" + group.getGroupId()
				+ ConstantKeys.AVATAR + ConstantKeys.EXTENSION_JPG);

		if (toDelete.exists()) {
			toDelete.delete();
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.GroupsDataBase(DataBasesAccess.WRITE, group);

		return true;
	}

	public static boolean runDeleteGroup(Context context, String jsonCommand) {

		GroupObject group = null;
		try {
			group = JsonParser.jsonToGroup(jsonCommand);
		} catch (JSONException e) {
			return false;
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.GroupsDataBase(DataBasesAccess.DELETE, group);

		return true;
	}

	public static boolean runUpdateGroupMember(Context context,
			String jsonCommand, Boolean admin) {

		GroupMemberObject groupMember = null;
		try {
			groupMember = JsonParser.jsonToGroupMember(jsonCommand);
		} catch (JSONException e) {
			return false;
		}

		groupMember.setAdmin(admin);
		groupMember.setToAdd(true);

		DataBasesAccess.getInstance(context.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.WRITE, groupMember, null);

		return true;
	}

	public static boolean runDeleteGroupMember(Context context,
			String jsonCommand) {

		GroupMemberObject groupMember = null;
		try {
			groupMember = JsonParser.jsonToGroupMember(jsonCommand);
		} catch (JSONException e) {
			return false;
		}

		DataBasesAccess
				.getInstance(context.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.DELETE, groupMember, null);

		if (deleteUserCRListener != null) {
			deleteUserCRListener.deleteUserCR();
		}
		return true;
	}

	public static boolean runUpdateContact(Context context, String jsonCommand) {

		UserObject user = null;
		try {
			user = JsonParser.jsonToUserObject(jsonCommand);
		} catch (JSONException e) {
			return false;
		}

		UserObject oldUser = DataBasesAccess.getInstance(
				context.getApplicationContext()).getUserDataBase(
				user.getId().toString());
		if ((oldUser != null)
				&& (oldUser.getPicture() != null)
				&& (!oldUser.getPicture().toString()
						.equals(user.getPicture().toString()))) {
			// Delete avatar to force downloading it again
			ImageDownloader downloader = new ImageDownloader();
			downloader.eraseUserAvatar(user.getId().toString());
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.UsersDataBase(DataBasesAccess.WRITE, user);

		return true;
	}

	public static boolean runUpdateMessage(Context context, String jsonCommand,
			boolean isRest, Long timeline) {
		MessageObject message;
		try {
			message = JsonParser.jsonToMessageRead(jsonCommand, isRest);
			if (timeline != null) {
				message.getTimeline().setId(timeline);
			}
		} catch (JSONException e) {
			log.error("Error parsing message JSON", e);
			return false;
		}

		TimelineObject time = DataBasesAccess.getInstance(
				context.getApplicationContext())
				.TimelinesDataBaseReadTimelineIdSelected(
						message.getTimeline().getId());
		message.setPartyId(time.getParty().getId());

		PerformanceMonitor.monitor(
				context,
				PerformanceMonitor.Type.MSG_RECEIVED,
				(new PerformanceMonitor.Message.Builder())
						.from(Long.toString(message.getFrom().getId()))
						.localId(message.getLocalId().toString())
						.id(Long.toString(message.getId())).build());

		message.setTotal(100);

		Account account = AccountUtils.getAccount(context, false);
		AccountManager am = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);

		String userId = am.getUserData(account, JsonKeys.ID_STORED);
		String fromId = String.valueOf(message.getFrom().getId());

		if (userId.equals(fromId)) {
			message.incStatusAndUpdate(MessageObject.Status.ACK, context);
		} else {
			message.incStatusAndUpdate(MessageObject.Status.NEW, context);
		}

		int row = DataBasesAccess.getInstance(context.getApplicationContext())
				.MessagesDataBaseInsertReturnRow(message);

		// update avatar
		ArrayList<AvatarObject> avatarList = DataBasesAccess.getInstance(
				context).AvatarDataBase(DataBasesAccess.READ, null, null);
		for (int i = 0; i < avatarList.size(); i++) {
			if (avatarList.get(i).getUserId()
					.equals(String.valueOf(message.getFrom().getId()))) {
				if (!avatarList.get(i).getAvatarId()
						.equals(String.valueOf(message.getFrom().getPicture()))) {
					// The avatar was changed so we delete the file
					File file = new File(FileUtils.getDir() + "/"
							+ message.getFrom().getId() + ConstantKeys.AVATAR
							+ ConstantKeys.EXTENSION_JPG);
					if (file.exists()) {
						file.delete();
					}
				}
			}
		}

		DataBasesAccess.getInstance(context).AvatarDataBase(
				DataBasesAccess.WRITE,
				String.valueOf(message.getFrom().getId()),
				String.valueOf(message.getFrom().getPicture()));

		// update timeline with this last message

		if (row != 0) {
			Long server = message.getTimestamp();
			Long local = System.currentTimeMillis();
			DataBasesAccess.getInstance(context.getApplicationContext())
					.TimelinesDataBaseDrift(message.getTimeline().getId(),
							local - server);
			// timeline was changed, we broadcast to activities to upload it
			Intent intent = new Intent();
			intent.setAction(ConstantKeys.BROADCAST_TIMELINE_UPDATE);
			intent.putExtra(ConstantKeys.TIMELINE,
					String.valueOf(message.getTimeline().getId()));
			intent.putExtra(ConstantKeys.DRIFT, String.valueOf(local - server));
			context.sendBroadcast(intent);
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.TimelinesDataBaseWriteNewMessage(
						message.getTimeline().getId(), message.getId(),
						message.getTimestamp(), true, message.getBody());
		if (receiveMsgComRunListener != null) {
			receiveMsgComRunListener.receiveMsgComRun();
		}

		return true;
	}

	public static boolean runUpdateTimeline(Context context, String jsonCommand) {

		TimelineObject timeline = null;

		try {
			timeline = JsonParser.jsonToTimelineObject(jsonCommand);
			timeline.setShowIt(true);
		} catch (JSONException e) {
			return false;
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.TimelinesDataBaseWrite(timeline);

		return true;
	}

	public static boolean runDeleteTimeline(Context context, String jsonCommand) {

		TimelineObject timeline = null;

		try {
			timeline = JsonParser.jsonToTimelineObject(jsonCommand);
			timeline.setShowIt(true);
		} catch (JSONException e) {
			return false;
		}

		DataBasesAccess.getInstance(context.getApplicationContext())
				.DeleteTimelinesDataBase(timeline.getParty().getId());
		if (deleteTimelineListener != null) {
			deleteTimelineListener.deleteTimeline();
		}

		if (timeline.getId() > 0) {
			FileUtils.deleteMediaFromTimeline(context, timeline.getId());
		}

		return true;
	}

	public static boolean runFactoryReset(Context context) {
		AppUtils.purgeApp(context);
		return true;
	}

	/* Test Utilities */

	private static DeleteTimelineListener deleteTimelineListener;

	public interface DeleteTimelineListener {
		void deleteTimeline();
	}

	public static void setDeleteTimelineListener(DeleteTimelineListener l) {
		deleteTimelineListener = l;
	}

	private static ReceiveMsgComRunListener receiveMsgComRunListener;

	public interface ReceiveMsgComRunListener {
		void receiveMsgComRun();
	}

	public static void setReceiveMsgComRunListener(ReceiveMsgComRunListener l) {
		receiveMsgComRunListener = l;
	}

	private static DeleteUserCRListener deleteUserCRListener;

	public interface DeleteUserCRListener {
		void deleteUserCR();
	}

	public static void setDeleteUserCRListener(DeleteUserCRListener l) {
		deleteUserCRListener = l;
	}

}
