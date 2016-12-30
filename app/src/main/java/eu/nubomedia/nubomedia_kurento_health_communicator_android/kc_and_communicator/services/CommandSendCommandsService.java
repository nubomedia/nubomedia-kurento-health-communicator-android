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
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.CommandObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.IntentService;
import android.content.Intent;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;

public class CommandSendCommandsService extends IntentService {

	private static final Logger log = LoggerFactory
			.getLogger(CommandSendCommandsService.class.getSimpleName());

	private static final Executor SEND_COMMAND_WITH_MEDIA_EXECUTOR = Executors
			.newScheduledThreadPool(HttpManager.HTTP_CLIENT_MULTIPART_CONNECTIONS);

	public CommandSendCommandsService() {
		super("CommandSendCommandsService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		List<CommandObject> list = DataBasesAccess.getInstance(
				getApplicationContext()).getNonProcessingCommands();
		List<CommandObject> plainCmds = new ArrayList<CommandObject>();

		for (final CommandObject cmd : list) {
			PerformanceMonitor.monitor(getApplicationContext(),
					PerformanceMonitor.Type.CMD_DEQUEUED, cmd.getJson());

			restoreCommand(cmd, CommandObject.SendStatus.PROCESSING);

			if (cmd.getMedia().length() > 2) {
				processCommands(plainCmds, false);
				plainCmds.clear();

				final List<CommandObject> l = new ArrayList<CommandObject>();
				l.add(cmd);
				SEND_COMMAND_WITH_MEDIA_EXECUTOR.execute(new Runnable() {
					@Override
					public void run() {
						processCommands(l, true);
					}
				});
			} else {
				plainCmds.add(cmd);
			}
		}

		processCommands(plainCmds, false);
	}

	private void processCommands(List<CommandObject> cmdList,
			boolean withAttachment) {
		if (cmdList.isEmpty()) {
			return;
		}

		if (withAttachment && cmdList.size() > 1) {
			log.error("Only one command with attachment can be sent at the same time.");
			return;
		}

		try {
			boolean status;

			if (withAttachment) {
				status = CommandService
						.sendCommand(CommandSendCommandsService.this,
								cmdList.get(0));
			} else {
				status = CommandService
						.sendCommandTransaction(
								CommandSendCommandsService.this, cmdList);
			}

			if (status) {
				for (CommandObject cmd : cmdList) {
					deleteCommand(cmd);
					notifyOnMsgSent(cmd.getJson());
				}
			} else {
				for (CommandObject cmd : cmdList) {
					log.warn("Command could not be sent " + cmd.getJson());
					restoreCommand(cmd, CommandObject.SendStatus.NONE);
				}
			}
		} catch (Exception e) {
			log.error("Cannot send command. It will be deleted", e);
			for (CommandObject cmd : cmdList) {
				deleteCommand(cmd);
				recoverStatus(cmd);
			}
		}
	}

	private void notifyOnMsgSent(String json) {
		String localId = null;
		try {
			JSONObject object = new JSONObject(json);
			if (object.getString(JsonKeys.METHOD).equals(
					Command.METHOD_SEND_MESSAGE_TO_GROUP)) {
				JSONObject params = object.getJSONObject(JsonKeys.PARAMS);
				localId = params.getString(JsonKeys.LOCALID);

				if (localId == null) {
					log.error("Error getting localId");
					return;
				}

				Intent intent = new Intent();
				intent.setAction(ConstantKeys.BROADCAST_MESSAGE_SENT);
				intent.putExtra(ConstantKeys.LOCALID, localId);
				getApplicationContext().sendBroadcast(intent);
			}
		} catch (JSONException e) {
			log.error("Error getting localId", e);
		}
	}

	private void storeCommand(CommandObject command, String object) {
		DataBasesAccess.getInstance(getApplicationContext())
				.CommandsToSendDataBase(DataBasesAccess.WRITE, null, command,
						object);
	}

	private void deleteCommand(CommandObject cmd) {
		DataBasesAccess.getInstance(getApplicationContext())
				.CommandsToSendDataBase(DataBasesAccess.DELETE, cmd.getID(),
						null, null);
	}

	private void restoreCommand(CommandObject cmd, CommandObject.SendStatus status) {
		deleteCommand(cmd);
		cmd.setSendStatus(status);
		storeCommand(cmd, cmd.getMedia());
	}

	private void recoverStatus(CommandObject cmd) {
		String json = cmd.getJson();
		log.warn("Command was rejected: {}", json);

		try {
			JSONObject jsonObject = new JSONObject(json);
			JSONObject params = jsonObject.getJSONObject(JsonKeys.PARAMS);

			String method = jsonObject.getString(JsonKeys.METHOD);

			if (method.equals(Command.METHOD_SEND_MESSAGE_TO_GROUP)) {
				restoreSendMessage(params, cmd.getMedia());

			} else if (method.equals(Command.METHOD_DELETE_TIMELINE)) {
				restoreDeleteTimeline(params);

			} else if (method.equals(Command.METHOD_CREATE_GROUP)) {
				restoreCreateGroup(params);

			} else if (method.equals(Command.METHOD_DELETE_GROUP)) {
				restoreDeleteGroup(params);

			} else if (method.equals(Command.METHOD_REMOVE_GROUP_MEMBER)) {
				restoreRemoveGroupMember(params);

			} else if (method.equals(Command.METHOD_ADD_GROUP_MEMBER)) {
				restoreAddGroupMember(params);

			} else if (method.equals(Command.METHOD_UPDATE_USER)) {
				retoreUpdateUser(params);

			} else if (method.equals(Command.METHOD_UPDATE_GROUP)) {
				// TODO
			}

		} catch (JSONException e) {
		}
	}

	private ArrayList<CommandObject> getDBCommand() {
		ArrayList<CommandObject> list = DataBasesAccess.getInstance(
				getApplicationContext()).CommandsToSendDataBase(
				DataBasesAccess.READ, null, null, null);
		return list;
	}

	// Methods to retrieve databases to the last status
	private void restoreSendMessage(JSONObject params, String media)
			throws JSONException {
		Long localId = params.getLong(JsonKeys.LOCALID);
		DataBasesAccess.getInstance(getApplicationContext())
				.MessagesDataBaseDelete(localId);

		if (!media.equals(ConstantKeys.STRING_CERO)) {
			File file = new File(media);
			file.delete();
		}
	}

	private void restoreDeleteTimeline(JSONObject params) throws JSONException {
		JSONObject party = params.getJSONObject(JsonKeys.PARTY);
		Long id = Long.parseLong(party.getString(JsonKeys.ID));
		DataBasesAccess.getInstance(getApplicationContext())
				.TimelinesDataBaseIsRecoverTimeline(id);
	}

	private void restoreCreateGroup(JSONObject params) throws JSONException {
		JSONObject group = params.getJSONObject(JsonKeys.GROUP);
		Long localId = group.getLong(JsonKeys.LOCALID);
		GroupObject aux = new GroupObject();
		aux.setLocalId(localId);
		DataBasesAccess.getInstance(getApplicationContext()).GroupsDataBase(
				DataBasesAccess.DELETE_LOCAL, aux);
	}

	private void restoreDeleteGroup(JSONObject params) throws JSONException {
		Long id = Long.parseLong(params.getString(JsonKeys.ID));
		GroupObject aux = new GroupObject();
		aux.setGroupId(id.toString());
		DataBasesAccess.getInstance(getApplicationContext()).GroupsDataBase(
				DataBasesAccess.CANCEL_DELETE_PROCESS, aux);
		DataBasesAccess.getInstance(getApplicationContext())
				.TimelinesDataBaseSetState(id, State.ENABLED);
	}

	private void restoreRemoveGroupMember(JSONObject params)
			throws JSONException {
		// Restore user status, is was set to delete
		GroupMemberObject groupMember = new GroupMemberObject();
		JSONObject group = params.getJSONObject(JsonKeys.GROUP);
		JSONObject user = params.getJSONObject(JsonKeys.USER);
		groupMember.getGroup().setId(
				Long.parseLong(group.getString(JsonKeys.ID)));
		groupMember.getUser()
				.setId(Long.parseLong(user.getString(JsonKeys.ID)));

		DataBasesAccess.getInstance(getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.CANCEL_DELETE_PROCESS,
						groupMember, null);

		// Restore the group and timeline status it is was the localuser
		// the one to be deleted
		// cos this method leave the user from a group too
		GroupObject aux = new GroupObject();
		aux.setGroupId(group.getString(JsonKeys.ID));
		DataBasesAccess.getInstance(getApplicationContext()).GroupsDataBase(
				DataBasesAccess.CANCEL_DELETE_PROCESS, aux);
		DataBasesAccess.getInstance(getApplicationContext())
				.TimelinesDataBaseSetState(
						Long.parseLong(group.getString(JsonKeys.ID)), State.ENABLED);
	}

	private void restoreAddGroupMember(JSONObject params) throws JSONException {
		GroupMemberObject groupMember = new GroupMemberObject();
		JSONObject group = params.getJSONObject(JsonKeys.GROUP);
		JSONObject user = params.getJSONObject(JsonKeys.USER);
		groupMember.getGroup().setId(
				Long.parseLong(group.getString(JsonKeys.ID)));
		groupMember.getUser()
				.setId(Long.parseLong(user.getString(JsonKeys.ID)));

		DataBasesAccess
				.getInstance(getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.DELETE, groupMember, null);
	}

	private void retoreUpdateUser(JSONObject params) throws JSONException {
		AppUtils.getUserInfo(this.getApplicationContext());
	}

}
