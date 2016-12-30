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

public class JsonKeys {

	// Commands type
	public static final String COMMAND_TYPE_UPDATE_MESSAGE_ARRAY = "updateMessageArray";
	public static final String COMMAND_TYPE_UPDATE_GROUP_ARRAY = "updateGroupArray";
	public static final String COMMAND_TYPE_DELETE_GROUP_ARRAY = "deleteGroupArray";
	public static final String COMMAND_TYPE_CALL_RECEIVED = "callReceived";
	public static final String COMMAND_MESSAGE_DATACHANNEL_MUTE_AUDIO = "datachannelMessageMuteAudio";
	public static final String COMMAND_MESSAGE_DATACHANNEL_MUTE_VIDEO = "datachannelMessageMuteVideo";
	public static final String COMMAND_MESSAGE_DATACHANNEL_CALL_WAIT = "datachannelMessageCallWait";
	public static final String COMMAND_MESSAGE_DATACHANNEL_CALL_WAKEUP = "datachannelMessageCallWakeUp";
	public static final String COMMAND_MESSAGE_DATACHANNEL_CALL_WAIT_CALLFWD_ACCEPT = "datachannelMessageCallWaitCallFwdAccept";
	public static final String COMMAND_MESSAGE_DATACHANNEL_CALLFWD_INIT_CALL = "datachannelMessageCallFwdInitCall";
	public static final String COMMAND_MESSAGE_DATACHANNEL_CALLFWD_TERMINATE_CALL = "datachannelMessageCallFwdTerminateCall";

	// Invitation Actions
	public static final String SEND = "send";
	public static final String ACCEPT = "accept";
	public static final String REJECT = "reject";
	public static final String CANCEL = "cancel";

	// Commands
	public static final String COMMAND_LIST = "commandList";
	public static final String METHOD = "method";
	public static final String PARAMS = "params";
	public static final String COMMAND = "command";
	public static final String LAST_SEQUENCE = "lastSequence";
	public static final String SEQUENCE_NUMBER = "sequenceNumber";
	public static final String CHANNEL_ID = "channelId";
	public static final String REGISTER_ID = "registerId";
	public static final String REGISTER_TYPE = "registerType";
	public static final String INSTANCE_ID = "instanceId";
	public static final String REGISTER_USER_ID = "userId";
	public static final String OWNER = "owner";
	public static final String OWNER_ID = "ownerId";
	public static final String PARTY_ID = "partyId";
	public static final String PARTY = "party";
	public static final String TIMELINE = "timeline";
	public static final String LOCALID = "localId";
	public static final String PARTY_NAME = "partyName";
	public static final String PARTY_TYPE = "partyType";
	public static final String GROUP = "group";
	public static final String ACCOUNT = "account";
	public static final String TYPE = "type";
	public static final String USER = "user";
	public static final String ACTION = "action";
	public static final String ENABLED = "enabled";
	public static final String LOCAL_PHONES = "localPhones";
	public static final String PHONE_REGION = "phoneRegion";
	public static final String LOCAL = "locale";

	// Message
	public final static String FROM = "from";
	public final static String TO = "to";
	public final static String BODY = "body";
	public final static String MEDIA = "media";
	public final static String ID_STORED = "id_stored";
	public final static String ID = "id";
	public final static String TIMESTAMP = "timestamp";
	public final static String APP = "app";
	public final static String FROM_NAME = "fromName";
	public final static String FROM_SURNAME = "fromSurname";
	public final static String FROM_PICTURE = "fromPicture";
	public static final String CONTENT_SIZE = "contentSize";
	public static final String CONTENT_ID = "contentId";
	public static final String CONTENT_TYPE = "contentType";
	public static final String MAX_MESSAGE = "maxMessage";
	public static final String CONTENT = "content";
	public final static String LAST_MESSAGE = "lastMessage";
	public static final String PAYLOAD = "payload";

	// User
	public final static String VERSION = "version";
	public final static String NAME = "name";
	public final static String SURNAME = "surname";
	public final static String PICTURE = "picture";
	public final static String PHONE = "phone";
	public final static String EMAIL = "email";
	public final static String PASSWORD = "password";
	public final static String AUTH_TOKEN = "authtoken";
	public final static String QOS_FLAG = "qos";

	// Recovery
	public final static String USER_IDENTITY = "userIdentity";
	public final static String SECURITY_CODE = "securityCode";
	public final static String NEW_PASSWORD = "newPassword";

	// Groups
	public final static String CAN_READ = "canRead";
	public final static String CAN_SEND = "canSend";
	public final static String CAN_LEAVE = "canLeave";
	public final static String IS_ADMIN = "isAdmin";
	public final static String ADMIN = "admin";

	// Account
	public final static String USER_AUTO_REGISTER = "userAutoregister";
	public final static String GROUP_AUTO_REGISTER = "groupAutoregister";
	public final static String ACCOUNT_NAME = "name";

	// Search users
	public final static String PATTERN = "pattern";
	public final static String FIRST_RESULT = "firstResult";
	public final static String MAX_RESULT = "maxResult";

	// Timeline
	public final static String STATE = "state";

	// GCM
	public final static String GCM_ID = "gcm_id";

	// Call
	public static final String AUDIO_STATE = "audioState";
	public static final String VIDEO_STATE = "videoState";

}
