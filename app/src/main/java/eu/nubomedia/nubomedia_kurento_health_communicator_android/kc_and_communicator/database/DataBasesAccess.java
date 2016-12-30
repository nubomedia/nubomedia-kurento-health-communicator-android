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

import android.content.Context;

import com.kurento.agenda.datamodel.pojo.Timeline.State;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class DataBasesAccess {

	public static final int READ = 100;
	public static final int WRITE = 101;
	public static final int DELETE = 102;
	public static final int READ_SELECTED = 104;
	public static final int WRITE_NEW_MESSAGE = 105;
	public static final int WRITE_TOTAL = 106;
	public static final int WRITE_NEW_MESSAGE_FALSE = 107;
	public static final int WRITE_DONT_SHOW = 108;
	public static final int WRITE_DRIFT = 109;
	public static final int DELETE_PROCESS = 110;
	public static final int CANCEL_DELETE_PROCESS = 111;
	public static final int DELETE_LOCAL = 112;
	public static final int ADD_PROCESS = 113;
	public static final int CANCEL_ADD_PROCESS = 114;

	private static DataBasesAccess classAccess = null;
	private static AvatarDataBaseAdapter dbAvatar = null;
	private static CommandsToSendDataBaseAdapter dbCommand = null;
	private static UsersDataBaseAdapter dbUser = null;
	private static GroupsDataBaseAdapter dbGroup = null;
	private static GroupsMemberDataBaseAdapter dbGroupMember = null;
	private static MessagesDataBaseAdapter dbMessage = null;
	private static TimelineDataBaseAdapter dbTimeline = null;

	private static String createInstance = ConstantKeys.STRING_DEFAULT;
	private static String accessAvatar = ConstantKeys.STRING_DEFAULT;
	private static String accessCommands = ConstantKeys.STRING_DEFAULT;
	private static String accessUsers = ConstantKeys.STRING_DEFAULT;
	private static String accessGroups = ConstantKeys.STRING_DEFAULT;
	private static String accessInvitations = ConstantKeys.STRING_DEFAULT;
	private static String accessMessages = ConstantKeys.STRING_DEFAULT;
	private static String accessTimelines = ConstantKeys.STRING_DEFAULT;

	public static DataBasesAccess getInstance(Context ctx) {

		if (classAccess == null) {
			synchronized (createInstance) {
				if (classAccess == null) {
					classAccess = new DataBasesAccess();
					dbAvatar = new AvatarDataBaseAdapter(
							ctx.getApplicationContext());
					dbCommand = new CommandsToSendDataBaseAdapter(
							ctx.getApplicationContext());
					dbUser = new UsersDataBaseAdapter(
							ctx.getApplicationContext());
					dbGroup = new GroupsDataBaseAdapter(
							ctx.getApplicationContext());
					dbGroupMember = new GroupsMemberDataBaseAdapter(
							ctx.getApplicationContext());
					dbMessage = new MessagesDataBaseAdapter(
							ctx.getApplicationContext());
					dbTimeline = new TimelineDataBaseAdapter(
							ctx.getApplicationContext());
					dbAvatar.open();
					dbCommand.open();
					dbUser.open();
					dbGroup.open();
					dbGroupMember.open();
					dbMessage.open();
					dbTimeline.open();
				}
			}
		}
		return classAccess;
	}

	public ArrayList<AvatarObject> AvatarDataBase(int type, String userId,
			String avatarId) {
		ArrayList<AvatarObject> list = null;
		synchronized (accessAvatar) {
			if (type == READ) {
				list = dbAvatar.getAllEntriesParsed();
			} else if (type == WRITE) {
				dbAvatar.insertEntry(userId, avatarId);
			}
		}
		return list;
	}

	public void DeleteAvatarDataBase() {
		synchronized (accessAvatar) {
			dbAvatar.removeAll();
		}
	}

	public ArrayList<CommandObject> getNonProcessingCommands() {
		synchronized (accessCommands) {
			return dbCommand
					.getCommandsWithStatus(CommandObject.SendStatus.NONE);
		}
	}

	public ArrayList<CommandObject> CommandsToSendDataBase(int type, String id,
			CommandObject command, String object) {
		ArrayList<CommandObject> list = null;
		synchronized (accessCommands) {
			if (type == DELETE) {
				dbCommand.removeEntry(id);
			} else if (type == READ) {
				list = dbCommand.getAllEntriesParsed();
			} else if (type == WRITE) {
				dbCommand.insertEntry(command, object);
			}
		}
		return list;
	}

	public void DeleteCommandsToSendDataBase() {
		synchronized (accessCommands) {
			dbCommand.removeAll();
		}
	}

	public UserObject getUserDataBase(String userId) {
		synchronized (accessUsers) {
			return dbUser.getUser(userId);
		}
	}

	public ArrayList<UserObject> UsersDataBase(int type, UserObject user) {
		ArrayList<UserObject> list = null;
		synchronized (accessUsers) {
			if (type == READ) {
				list = dbUser.getAllEntriesParsed();
			} else if (type == WRITE) {
				dbUser.insertEntry(user);
			}
		}
		return list;
	}

	public void DeleteUsersDataBase() {
		synchronized (accessUsers) {
			dbUser.removeAll();
		}
	}

	public ArrayList<GroupObject> GroupsDataBase(int type, GroupObject group) {
		ArrayList<GroupObject> list = null;
		synchronized (accessGroups) {
			if (type == READ) {
				list = dbGroup.getAllEntriesParsed();
			} else if (type == WRITE) {
				dbGroup.insertEntry(group);
			} else if (type == DELETE) {
				dbGroup.removeEntry(group.getGroupId());
			} else if (type == DELETE_PROCESS) {
				dbGroup.setToDelete(group.getGroupId(), true);
			} else if (type == CANCEL_DELETE_PROCESS) {
				dbGroup.setToDelete(group.getGroupId(), false);
			} else if (type == DELETE_LOCAL) {
				dbGroup.deleteLocal(group.getLocalId());
			}
		}
		return list;
	}

	public GroupObject getGroupDataBase(String groupId) {
		synchronized (accessGroups) {
			return dbGroup.getGroup(groupId);
		}
	}

	public void DeleteGroupsDataBase() {
		synchronized (accessGroups) {
			dbGroup.removeAll();
		}
	}

	public ArrayList<GroupMemberObject> GroupMembersDataBase(int type,
			GroupMemberObject groupMember, Long groupId) {
		ArrayList<GroupMemberObject> list = null;
		synchronized (accessGroups) {
			if (type == READ) {
				list = dbGroupMember.getAllEntriesParsed();
			} else if (type == READ_SELECTED) {
				list = dbGroupMember.getMembers(groupId);
			} else if (type == WRITE) {
				dbGroupMember.insertEntry(groupMember);
			} else if (type == DELETE) {
				dbGroupMember.removeEntry(groupMember);
			} else if (type == DELETE_PROCESS) {
				dbGroupMember.setToDelete(groupMember.getGroup().getId()
						.toString(), groupMember.getUser().getId().toString(),
						true);
			} else if (type == CANCEL_DELETE_PROCESS) {
				dbGroupMember.setToDelete(groupMember.getGroup().getId()
						.toString(), groupMember.getUser().getId().toString(),
						false);
			} else if (type == ADD_PROCESS) {
				dbGroupMember.setToAdd(groupMember.getGroup().getId()
						.toString(), groupMember.getUser().getId().toString(),
						true);
			} else if (type == CANCEL_ADD_PROCESS) {
				dbGroupMember.setToAdd(groupMember.getGroup().getId()
						.toString(), groupMember.getUser().getId().toString(),
						false);
			}
		}
		return list;
	}

	public void DeleteGroupMembersDataBase() {
		synchronized (accessGroups) {
			dbGroupMember.removeAll();
		}
	}

	// Messages access

	public ArrayList<MessageObject> MessagesDataBaseRead() {
		synchronized (accessMessages) {
			return dbMessage.getAllEntriesParsed();
		}
	}

	public void MessagesDataBaseWrite(MessageObject message) {
		synchronized (accessMessages) {
			dbMessage.insertEntry(message);
		}
	}

	public void MessagesDataBaseDeleteAll() {
		synchronized (accessMessages) {
			dbMessage.removeAll();
		}
	}

	public void MessagesDataBaseSetMessageStatus(Long localId, MessageObject.Status status) {
		synchronized (accessMessages) {
			dbMessage.setMessageStatus(localId, status);
		}
	}

	public void MessagesDataBaseWriteTotal(String localId, int total) {
		synchronized (accessMessages) {
			dbMessage.setTotal(localId, total);
		}
	}

	public void MessagesDataBaseDelete(Long localId) {
		synchronized (accessMessages) {
			dbMessage.removeEntry(localId);
		}
	}

	public ArrayList<MessageObject> MessagesDataBaseReadSelected(
			Long timelineId, Long partyId) {
		synchronized (accessMessages) {
			return dbMessage.getSelectedEntriesParsed(timelineId, partyId);
		}
	}

	public int MessagesDataBaseInsertReturnRow(MessageObject message) {
		synchronized (accessMessages) {
			return dbMessage.insertEntry(message);
		}
	}

	public int MessagesDataBaseCountTotalMessages() {
		synchronized (accessMessages) {
			return dbMessage.getObjectsNumber();
		}
	}

	// Timeline access

	public ArrayList<TimelineObject> TimelinesDataBaseRead() {
		synchronized (accessTimelines) {
			return dbTimeline.getAllEntriesParsed();
		}
	}

	public TimelineObject TimelinesDataBaseReadPartyIdSelected(Long id) {
		synchronized (accessTimelines) {
			return dbTimeline.getEntrieParsed(id,
					TimelineDataBaseAdapter.PARTY_ID_COLUMN);
		}
	}

	public TimelineObject TimelinesDataBaseReadTimelineIdSelected(Long id) {
		synchronized (accessTimelines) {
			return dbTimeline.getEntrieParsed(id,
					TimelineDataBaseAdapter.TIMELINE_ID_COLUMN);
		}
	}

	public void TimelinesDataBaseWrite(TimelineObject timeline) {
		synchronized (accessTimelines) {
			dbTimeline.insertEntry(timeline);
		}
	}

	public void TimelinesDataBaseWriteNewMessageFalse(TimelineObject timeline) {
		synchronized (accessTimelines) {
			dbTimeline.setNewMessageFalse(timeline.getId());
		}
	}

	public void TimelinesDataBaseWriteDontShow(Long id) {
		synchronized (accessTimelines) {
			dbTimeline.dontShow(id);
		}
	}

	public void TimelinesDataBaseSetState(Long id, State state) {
		synchronized (accessTimelines) {
			dbTimeline.setState(id, state);
		}
	}

	public void TimelinesDataBaseWriteNewMessage(Long id, Long messageId,
			Long messageTime, boolean fromUser, String messageBody) {
		synchronized (accessTimelines) {
			dbTimeline.setNewMessage(id, messageId, fromUser, messageTime,
					messageBody);

		}
	}

	public void TimelinesDataBaseDrift(Long id, Long drift) {
		synchronized (accessTimelines) {
			dbTimeline.setDrift(id, drift);
		}
	}

	public boolean TimelinesDataBaseIsRecoverTimeline(Long id) {
		synchronized (accessTimelines) {
			return dbTimeline.RecoverTimeline(id);
		}
	}

	public void DeleteTimelinesDataBase(Long PartyId) {
		synchronized (accessTimelines) {
			dbTimeline.removeEntry(PartyId.toString());
		}
	}

	public void DeleteTimelinesDataBase() {
		synchronized (accessTimelines) {
			dbTimeline.removeAll();
		}
	}

}
