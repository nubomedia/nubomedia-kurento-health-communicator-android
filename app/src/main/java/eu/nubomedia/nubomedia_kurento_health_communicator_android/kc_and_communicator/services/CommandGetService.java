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
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GettingCommandsStatusAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.gcm.MyGCMIntentService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.push.WSPushManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo.Code;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.CommandReadResponse;

public class CommandGetService extends Service {

	private static final Logger log = LoggerFactory
			.getLogger(CommandGetService.class.getSimpleName());

	private static boolean updatedMessageNotification = true;

	private ServiceHandler mServiceHandler;
	private static final int MSG_WHAT = 0;

	@Override
	public void onCreate() {
		HandlerThread thread = new HandlerThread("GetMessages");
		thread.start();

		Looper mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		updatedMessageNotification = AppUtils.getResource(
				getApplicationContext(), R.bool.updated_messages_notification,
				true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mServiceHandler.hasMessages(MSG_WHAT)) {
			mServiceHandler.sendEmptyMessage(MSG_WHAT);
		}

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			getCommands();
		}
	}

	private void getCommands() {
		Account account = AccountUtils.getAccount(getApplicationContext(),
				false);
		if (account == null) {
			log.warn("There is not any account. Messages can no be retrieved");
			return;
		}

		AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
		log.debug("account: {}, am: {}", account, am);

		String lastSeq = am.getUserData(account, JsonKeys.LAST_SEQUENCE);
		String channelId = am.getUserData(account, JsonKeys.CHANNEL_ID);

		String commandsJson;
		try {
			commandsJson = CommandService.getCommands(CommandGetService.this,
					lastSeq, channelId);
		} catch (NotFoundException e) {
			log.warn("Cannot get commands", e);
			KhcNotFoundInfo info = e.getInfo();
			if (info == null) {
				return;
			}

			if (Code.CHANNEL_NOT_FOUND.equals(info.getCode())) {
				log.warn("Channel {} not found. A new one will be created.",
						channelId);
				ChannelService.createChannel(this);

				try {
					channelId = am.getUserData(account, JsonKeys.CHANNEL_ID);
					commandsJson = CommandService.getCommands(
							CommandGetService.this, lastSeq, channelId);
				} catch (Exception e1) {
					log.warn("Cannot get commands before creating channel", e);
				}
			}

			return;
		} catch (Exception e) {
			log.warn("Cannot get commands", e);
			return;
		}

		if (commandsJson == null) {
			log.debug("Any command retrieve");
			return;
		}

		try {
			String newLastSeq = processCommandsJson(commandsJson);
			if ((lastSeq == null)
					|| ((newLastSeq != null) && (Integer.parseInt(newLastSeq) > Integer
							.parseInt(lastSeq)))) {
				am.setUserData(account, JsonKeys.LAST_SEQUENCE, newLastSeq);
			}
		} catch (JSONException e) {
			log.warn("Cannot process retrieved commnands: {}", commandsJson);

			GettingCommandsStatusAccess.getInstance(getApplicationContext())
					.setCommandsStatus(getApplicationContext(), true);

			return;
		}

		Intent iFinish = new Intent(ConstantKeys.BROADCAST_GET_COMMANDS_FINISH);
		sendBroadcast(iFinish);
	}

	private String processCommandsJson(String commandsJson)
			throws JSONException {
		List<CommandReadResponse> cmds;
		try {
			cmds = (List<CommandReadResponse>) JacksonManager.fromJson(
					commandsJson,
					new TypeReference<List<CommandReadResponse>>() {
					});
		} catch (Exception e) {
			log.error("Cannot process cmds", e);
			return null;
		}

		boolean sync = false;
		boolean updateMessages = false;
		boolean updateTimeline = false;
		boolean updateGroup = false;
		boolean deleteGroup = false;
		boolean updateMemberGroup = false;
		boolean updateUser = false;
		boolean updateContact = false;
		boolean callMute = false;
		boolean callAccept = false;
		String lastSeq = null;
		List<String> timelinesChanged = new ArrayList<String>();
		ArrayList<GroupObject> groupsChanged = new ArrayList<GroupObject>();
		ArrayList<GroupObject> groupsDeleted = new ArrayList<GroupObject>();
		CallReceive callReceived = null;

		for (CommandReadResponse cmd : cmds) {
			boolean ret = false;
			String m = cmd.getMethod();
			ObjectMapper mapper = JacksonManager.getMapper();
			String p = mapper.convertValue(cmd.getParams(), ObjectNode.class)
					.toString();

			log.debug("Received command " + m + ": "+ p);

			if (Command.METHOD_UPDATE_USER.equals(m)) {
				ret = CommandRunService.runUpdateUser(getApplicationContext(),
						p);

				updateUser = true;
			} else if (Command.METHOD_UPDATE_GROUP.equals(m)) {
				ret = CommandRunService.runUpdateGroup(getApplicationContext(),
						p);

				GroupObject group = JsonParser.jsonToGroup(p);
				groupsChanged.add(group);

				if (groupCreatedListener != null) {
					groupCreatedListener.groupCreated(group);
				}

				updateGroup = true;
			} else if (Command.METHOD_DELETE_GROUP.equals(m)) {
				ret = CommandRunService.runDeleteGroup(getApplicationContext(),
						p);

				GroupObject group = JsonParser.jsonToGroup(p);
				groupsDeleted.add(group);

				deleteGroup = true;
			} else if (Command.METHOD_ADD_GROUP_MEMBER.equals(m)) {
				ret = CommandRunService.runUpdateGroupMember(
						getApplicationContext(), p, false);
				updateMemberGroup = true;
			} else if (Command.METHOD_REMOVE_GROUP_ADMIN.equals(m)) {
				ret = CommandRunService.runUpdateGroupMember(
						getApplicationContext(), p, false);
				updateMemberGroup = true;
			} else if (Command.METHOD_REMOVE_GROUP_MEMBER.equals(m)) {
				ret = CommandRunService.runDeleteGroupMember(
						getApplicationContext(), p);
				updateMemberGroup = true;
			} else if (Command.METHOD_ADD_GROUP_ADMIN.equals(m)) {
				ret = CommandRunService.runUpdateGroupMember(
						getApplicationContext(), p, true);
				updateMemberGroup = true;
			} else if (Command.METHOD_UPDATE_CONTACT.equals(m)) {
				sync = true;
				ret = CommandRunService.runUpdateContact(
						getApplicationContext(), p);
				updateContact = true;
			} else if (Command.METHOD_UPDATE_MESSAGE.equals(m)) {
				ret = CommandRunService.runUpdateMessage(this, p, false, null);

				MessageObject message = JsonParser.jsonToMessageRead(p, false);
				timelinesChanged.add(String.valueOf(message.getTimeline()
						.getId()));

				updateMessages = true;
				updateTimeline = true;

				MyGCMIntentService.processNewMessageToNotify(message);
			} else if (Command.METHOD_FACTORY_RESET.equals(m)) {
				ret = CommandRunService
						.runFactoryReset(getApplicationContext());
			} else if (Command.METHOD_UPDATE_TIMELINE.equals(m)) {
				ret = CommandRunService.runUpdateTimeline(
						getApplicationContext(), p);
				updateTimeline = true;
			} else if (Command.METHOD_DELETE_TIMELINE.equals(m)) {
				ret = CommandRunService.runDeleteTimeline(
						getApplicationContext(), p);
				updateTimeline = true;
			} else {
				log.warn("Command with method {} not processed", m);
			}

			if (ret) {
				lastSeq = cmd.getSequenceNumber().toString();
			}
		}

		if (sync) {
			startService(new Intent(getApplicationContext(), SyncService.class));
		}

		// sending notifications and broadcasts
		if (updateMessages) {
			if ((!WSPushManager.isWebSocketOpen())
					&& (!Boolean.parseBoolean(AppUtils.getDefaults(
							ConstantKeys.FROMLOGIN, getApplicationContext())))) {
				if (updatedMessageNotification) {
					MyGCMIntentService
							.generateNotification(getApplicationContext());
				}
			} else {
				MyGCMIntentService.resetNotificationValues();
			}
		}

		GettingCommandsStatusAccess.getInstance(getApplicationContext())
				.setCommandsStatus(getApplicationContext(), false);

		Intent i = new Intent(ConstantKeys.BROADCAST_GCM);
		i.putExtra(Command.METHOD_ADD_GROUP_MEMBER, updateMemberGroup);
		i.putExtra(JsonKeys.COMMAND_TYPE_UPDATE_MESSAGE_ARRAY,
				timelinesChanged.toArray(new String[0]));
		i.putExtra(Command.METHOD_UPDATE_GROUP, updateGroup);
		i.putExtra(JsonKeys.COMMAND_TYPE_UPDATE_GROUP_ARRAY, groupsChanged);
		i.putExtra(Command.METHOD_UPDATE_MESSAGE, updateMessages);
		i.putExtra(Command.METHOD_UPDATE_TIMELINE, updateTimeline);
		i.putExtra(Command.METHOD_DELETE_GROUP, deleteGroup);
		i.putExtra(JsonKeys.COMMAND_TYPE_DELETE_GROUP_ARRAY, groupsDeleted);
		i.putExtra(Command.METHOD_UPDATE_USER, updateUser);
		i.putExtra(Command.METHOD_UPDATE_CONTACT, updateContact);
		i.putExtra(Command.METHOD_CALL_MUTE, callMute);
		i.putExtra(Command.METHOD_CALL_ACCEPT, callAccept);
		i.putExtra(JsonKeys.COMMAND_TYPE_CALL_RECEIVED, callReceived);
		sendBroadcast(i);

		return lastSeq;
	}

	private <T> T parseParam(CommandReadResponse cmd, Class<T> clazz)
			throws Exception {
		ObjectMapper mapper = JacksonManager.getMapper();
		ObjectNode params = mapper.convertValue(cmd.getParams(),
				ObjectNode.class);

		return mapper.readValue(params, clazz);
	}

	/* Test Utilities */

	private GroupCreatedListener groupCreatedListener;

	public interface GroupCreatedListener {
		void groupCreated(GroupObject group);
	}

	public void setGroupCreatedListener(GroupCreatedListener l) {
		this.groupCreatedListener = l;
	}

}
