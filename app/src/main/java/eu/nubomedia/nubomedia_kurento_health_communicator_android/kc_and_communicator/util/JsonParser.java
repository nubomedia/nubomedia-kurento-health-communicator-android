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

import java.util.Date;

import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.AccountReadInfoResponse;
import com.kurento.agenda.services.pojo.ContentReadResponse;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.agenda.services.pojo.UserEdit;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.agenda.services.pojo.UserReadResponse;

public class JsonParser {

	private static final Logger log = LoggerFactory
			.getLogger(CommandStoreService.class.getSimpleName());

	/* Parser to create the json of a command */
	public static String createCommandJson(String commandId, String channelId,
			String method, String innerJson) throws JSONException {
		JSONObject interObject = new JSONObject(innerJson);
		JSONObject json = new JSONObject();
		json.put(JsonKeys.CHANNEL_ID, channelId);
		json.put(JsonKeys.METHOD, method);
		json.put(JsonKeys.PARAMS, interObject);
		json.put(JsonKeys.SEQUENCE_NUMBER, commandId);

		return json.toString();
	}

	/* Parsers to create json with pojos */
	public static String MessageSendToJson(MessageSend message)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.FROM, message.getFrom());
		json.put(JsonKeys.TO, message.getTo());
		json.put(JsonKeys.BODY, message.getBody());
		json.put(JsonKeys.LOCALID, message.getLocalId());
		if (message.getApp() != null)
			json.put(JsonKeys.APP, message.getApp());

		return json.toString();
	}

	public static JSONObject AccountIdToJson(AccountId account)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, account.getId());
		return json;
	}

	public static JSONObject GroupCreateToJson(GroupCreate group)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.NAME, group.getName());
		json.put(JsonKeys.LOCALID, group.getLocalId());
		json.put(JsonKeys.PHONE, group.getPhone());

		return json;
	}

	public static String MessageReadToJson(MessageReadResponse message)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, message.getId());
		json.put(JsonKeys.BODY, message.getBody());
		json.put(JsonKeys.CONTENT_ID, message.getContent().getId());
		json.put(JsonKeys.CONTENT_SIZE, message.getContent().getContentSize());
		json.put(JsonKeys.CONTENT_TYPE, message.getContent().getContentType());
		json.put(JsonKeys.TIMESTAMP, message.getTimestamp());
		String appSerialized = null;
		if (message.getApp() != null)
			try {
				appSerialized = JacksonManager.getMapper().writeValueAsString(
						JacksonManager.getMapper().convertValue(
								message.getApp(), ObjectNode.class));
			} catch (Exception e) {
				//Log.w("Exception serializing message app: " + e.getMessage());
			}
		json.put(JsonKeys.APP, appSerialized);
		json.put(JsonKeys.FROM, message.getFrom().getId());
		json.put(JsonKeys.FROM_NAME, message.getFrom().getName());
		json.put(JsonKeys.FROM_SURNAME, message.getFrom().getSurname());
		json.put(JsonKeys.FROM_PICTURE, message.getFrom().getPicture());
		json.put(JsonKeys.TIMELINE, message.getTimeline());
		json.put(JsonKeys.LOCALID, message.getLocalId());

		return json.toString();
	}

	public static String userReadToJson(UserReadResponse user)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, user.getId());
		json.put(JsonKeys.NAME, user.getName());
		json.put(JsonKeys.SURNAME, user.getSurname());
		json.put(JsonKeys.PICTURE, user.getPicture());
		json.put(JsonKeys.PHONE, user.getPhone());
		json.put(JsonKeys.EMAIL, user.getEmail());
		return json.toString();
	}

	public static String userEditToJson(UserEdit user) throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, user.getId());
		json.put(JsonKeys.NAME, user.getName());
		json.put(JsonKeys.SURNAME, user.getSurname());
		json.put(JsonKeys.PHONE, user.getPhone());
		json.put(JsonKeys.PASSWORD, user.getPassword());
		json.put(JsonKeys.PHONE_REGION, ConstantKeys.ES);
		return json.toString();
	}

	public static String userObjectToJson(UserObject user) throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, user.getId());
		json.put(JsonKeys.NAME, user.getName());
		json.put(JsonKeys.SURNAME, user.getSurname());
		json.put(JsonKeys.PICTURE, user.getPicture());
		json.put(JsonKeys.PHONE, user.getPhone());
		return json.toString();
	}

	public static String groupToJson(GroupObject group) throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, group.getGroupId());
		json.put(JsonKeys.NAME, group.getName());
		json.put(JsonKeys.CAN_READ, group.getCanRead());
		json.put(JsonKeys.CAN_SEND, group.getCanSend());
		json.put(JsonKeys.CAN_LEAVE, group.getCanLeave());
		json.put(JsonKeys.PICTURE, group.getPicture());
		return json.toString();
	}

	public static String timelineToJson(TimelineObject timeline)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JsonKeys.ID, timeline.getId());
		json.put(JsonKeys.OWNER_ID, timeline.getOwnerId());

		JSONObject partyObject = new JSONObject();
		partyObject.put(JsonKeys.ID, timeline.getParty().getId());
		partyObject.put(JsonKeys.TYPE, timeline.getParty().getType().toString()
				.toLowerCase());
		partyObject.put(JsonKeys.NAME, timeline.getParty().getName());
		json.put(JsonKeys.PARTY, partyObject);

		return json.toString();
	}

	/* Parsers to create Pojos with Json */

	public static MessageSend jsonToMessageSend(String json)
			throws JSONException {
		MessageSend message = new MessageSend();
		JSONObject object = new JSONObject(json);
		if (object.has(JsonKeys.BODY)) {
			message.setBody(object.getString(JsonKeys.BODY));

		}
		if (object.has(JsonKeys.FROM)) {
			message.setFrom(Long.parseLong(object.getString(JsonKeys.FROM)));

		}
		if (object.has(JsonKeys.TO)) {
			message.setTo(Long.parseLong(object.getString(JsonKeys.TO)));

		}
		if (object.has(JsonKeys.LOCALID)) {
			message.setLocalId(object.getLong(JsonKeys.LOCALID));
		} else {
			message.setLocalId(Long.valueOf(System.currentTimeMillis()));
		}
		return message;

	}

	public static MessageObject jsonToMessageRead(String json, boolean isRest)
			throws JSONException {
		MessageObject message = new MessageObject();
		JSONObject object = new JSONObject(json);
		if (object.has(JsonKeys.BODY)) {
			message.setBody(object.getString(JsonKeys.BODY));
		} else {
			message.setBody(ConstantKeys.STRING_DEFAULT);
		}

		ContentReadResponse content = new ContentReadResponse();
		UserReadAvatarResponse from = new UserReadAvatarResponse();

		// Contents
		if (object.has(JsonKeys.CONTENT)) {
			JSONObject contentObject = object.getJSONObject(JsonKeys.CONTENT);

			if (contentObject.has(JsonKeys.ID)) {
				content.setId(Long.parseLong(contentObject
						.getString(JsonKeys.ID)));
			} else {
				content.setId(ConstantKeys.LONG_DEFAULT);
			}
			if (contentObject.has(JsonKeys.CONTENT_SIZE)) {
				try {
					content.setContentSize(Long.parseLong(contentObject
							.getString(JsonKeys.CONTENT_SIZE)));
				} catch (Exception e) {
					content.setContentSize(ConstantKeys.LONG_DEFAULT);
				}
			} else {
				content.setContentSize(ConstantKeys.LONG_DEFAULT);
			}
			if (contentObject.has(JsonKeys.CONTENT_TYPE)) {
				content.setContentType(contentObject
						.getString(JsonKeys.CONTENT_TYPE));
			} else {
				content.setContentType(ConstantKeys.STRING_DEFAULT);
			}
		} else {
			content.setId(ConstantKeys.LONG_DEFAULT);
			content.setContentSize(ConstantKeys.LONG_DEFAULT);
			content.setContentType(ConstantKeys.STRING_DEFAULT);
		}

		message.setContent(content);

		// From
		if (object.has(JsonKeys.FROM)) {
			JSONObject fromObject = object.getJSONObject(JsonKeys.FROM);

			if (fromObject.has(JsonKeys.ID)) {
				from.setId(Long.parseLong(fromObject.getString(JsonKeys.ID)));
			}
			if (fromObject.has(JsonKeys.NAME)) {
				from.setName(fromObject.getString(JsonKeys.NAME));
			} else {
				from.setName(ConstantKeys.STRING_DEFAULT);
			}
			if (fromObject.has(JsonKeys.SURNAME)) {
				from.setSurname(fromObject.getString(JsonKeys.SURNAME));
			} else {
				from.setSurname(ConstantKeys.STRING_DEFAULT);
			}
			if (fromObject.has(JsonKeys.PICTURE)) {
				from.setPicture(Long.parseLong(fromObject
						.getString(JsonKeys.PICTURE)));
			} else {
				from.setPicture(ConstantKeys.LONG_DEFAULT);
			}
		} else {
			from.setId(ConstantKeys.LONG_DEFAULT);
			from.setName(ConstantKeys.STRING_DEFAULT);
			from.setSurname(ConstantKeys.STRING_DEFAULT);
			from.setPicture(ConstantKeys.LONG_DEFAULT);
		}

		message.setFrom(from);

		if (object.has(JsonKeys.ID)) {
			message.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		}

		if (object.has(JsonKeys.APP)) {
			String payload = object.getString(JsonKeys.APP);
			if (payload != null) {
				message.setPayload(payload);
			}
		}

		if (object.has(JsonKeys.TIMESTAMP)) {
			Date date = new Date(Long.valueOf(object
					.getString(JsonKeys.TIMESTAMP)));
			message.setTimestamp(date.getTime());
		}

		if (object.has(JsonKeys.TIMELINE)) {
			JSONObject timelineObject = object.getJSONObject(JsonKeys.TIMELINE);
			if (timelineObject.has(JsonKeys.ID)) {
				message.getTimeline().setId(
						Long.parseLong(timelineObject.getString(JsonKeys.ID)));
			} else {
				message.getTimeline().setId(ConstantKeys.LONG_DEFAULT);
			}

			if (timelineObject.has(JsonKeys.NAME)) {
				message.getTimeline().setName(
						timelineObject.getString(JsonKeys.NAME));
			} else {
				message.getTimeline().setName(ConstantKeys.STRING_DEFAULT);
			}

			if (timelineObject.has(JsonKeys.TYPE)) {
				message.getTimeline().setType(
						timelineObject.getString(JsonKeys.TYPE));
			} else {
				message.getTimeline().setType(ConstantKeys.STRING_DEFAULT);
			}
		} else {
			message.getTimeline().setId(ConstantKeys.LONG_DEFAULT);
		}

		if (object.has(JsonKeys.LOCALID)) {
			message.setLocalId(object.getLong(JsonKeys.LOCALID));
		} else {
			message.setLocalId(Long.valueOf(System.currentTimeMillis()));
		}

		return message;
	}

	public static UserReadResponse jsonToUserRead(String json)
			throws JSONException {
		UserReadResponse user = new UserReadResponse();
		JSONObject object = new JSONObject(json);

		// optionals values
		if (object.has(JsonKeys.SURNAME)) {
			user.setSurname(object.getString(JsonKeys.SURNAME));
		} else {
			user.setSurname(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.PICTURE)) {
			user.setPicture(Long.parseLong(object.getString(JsonKeys.PICTURE)));
		} else {
			user.setPicture(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.PHONE)) {
			user.setPhone(object.getString(JsonKeys.PHONE));
		}
		if (object.has(JsonKeys.ID)) {
			user.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		} else {
			user.setId(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.NAME)) {
			user.setName(object.getString(JsonKeys.NAME));
		} else {
			user.setName(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.EMAIL)) {
			user.setEmail(object.getString(JsonKeys.EMAIL));
		}
		if (object.has(JsonKeys.QOS_FLAG)) {
			Boolean qos = Boolean.parseBoolean(object
					.getString(JsonKeys.QOS_FLAG));
			user.setQos(qos);
		}

		return user;
	}

	public static UserEdit jsonToUserEdit(String json) throws JSONException {
		UserEdit user = new UserEdit();
		JSONObject object = new JSONObject(json);
		if (object.has(JsonKeys.ID)) {
			user.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		} else {
			user.setId(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.NAME)) {
			user.setName(object.getString(JsonKeys.NAME));
		} else {
			user.setName(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.SURNAME)) {
			user.setSurname(object.getString(JsonKeys.SURNAME));
		} else {
			user.setSurname(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.PASSWORD)) {
			user.setPassword(object.getString(JsonKeys.PASSWORD));
		}
		if (object.has(JsonKeys.PHONE)) {
			user.setPhone(object.getString(JsonKeys.PHONE));
		}
		return user;
	}

	public static UserObject jsonToUserObject(String json) throws JSONException {
		UserObject user = new UserObject();
		JSONObject object = new JSONObject(json);

		// optionals values
		if (object.has(JsonKeys.SURNAME)) {
			user.setSurname(object.getString(JsonKeys.SURNAME));
		} else {
			user.setSurname(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.PICTURE)) {
			user.setPicture(Long.parseLong(object.getString(JsonKeys.PICTURE)));
		} else {
			user.setPicture(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.PHONE)) {
			user.setPhone(object.getString(JsonKeys.PHONE));
		} else {
			user.setPhone(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.ID)) {
			user.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		} else {
			user.setId(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.NAME)) {
			user.setName(object.getString(JsonKeys.NAME));
		} else {
			user.setPhone(ConstantKeys.STRING_DEFAULT);
		}
		return user;
	}

	public static GroupMemberObject jsonToGroupMember(String json)
			throws JSONException {
		GroupMemberObject groupMember = new GroupMemberObject();
		JSONObject object = new JSONObject(json);

		if (object.has(JsonKeys.GROUP)) {
			JSONObject groupObject = object.getJSONObject(JsonKeys.GROUP);

			if (groupObject.has(JsonKeys.ID)) {
				groupMember.getGroup().setId(
						Long.parseLong(groupObject.getString(JsonKeys.ID)));
			} else {
				groupMember.getGroup().setId(ConstantKeys.LONG_DEFAULT);
			}
			if (groupObject.has(JsonKeys.NAME)) {
				groupMember.getGroup().setName(
						groupObject.getString(JsonKeys.NAME));
			} else {
				groupMember.getGroup().setName(ConstantKeys.STRING_DEFAULT);
			}
		} else {
			groupMember.getGroup().setId(ConstantKeys.LONG_DEFAULT);
			groupMember.getGroup().setName(ConstantKeys.STRING_DEFAULT);
		}

		if (object.has(JsonKeys.USER)) {
			JSONObject userObject = object.getJSONObject(JsonKeys.USER);

			if (userObject.has(JsonKeys.ID)) {
				groupMember.getUser().setId(
						Long.parseLong(userObject.getString(JsonKeys.ID)));
			} else {
				groupMember.getUser().setId(ConstantKeys.LONG_DEFAULT);
			}
			if (userObject.has(JsonKeys.NAME)) {
				groupMember.getUser().setName(
						userObject.getString(JsonKeys.NAME));
			} else {
				groupMember.getUser().setName(ConstantKeys.STRING_DEFAULT);
			}
			if (userObject.has(JsonKeys.SURNAME)) {
				groupMember.getUser().setSurname(
						userObject.getString(JsonKeys.SURNAME));
			} else {
				groupMember.getUser().setSurname(ConstantKeys.STRING_DEFAULT);
			}
			if (userObject.has(JsonKeys.PICTURE)) {
				groupMember.getUser().setPicture(
						Long.parseLong(userObject.getString(JsonKeys.PICTURE)));
			} else {
				groupMember.getUser().setPicture(ConstantKeys.LONG_DEFAULT);
			}
		} else {
			groupMember.getUser().setId(ConstantKeys.LONG_DEFAULT);
			groupMember.getUser().setName(ConstantKeys.STRING_DEFAULT);
			groupMember.getUser().setPicture(ConstantKeys.LONG_DEFAULT);

		}

		return groupMember;
	}

	public static GroupObject jsonToGroup(String json) throws JSONException {
		GroupObject group = new GroupObject();
		JSONObject object = new JSONObject(json);

		if (object.has(JsonKeys.ID)) {
			group.setGroupId(object.getString(JsonKeys.ID));
		} else {
			group.setGroupId("0");
		}
		if (object.has(JsonKeys.NAME)) {
			group.setName(object.getString(JsonKeys.NAME));
		} else {
			group.setName(ConstantKeys.STRING_DEFAULT);
		}
		if (object.has(JsonKeys.CAN_READ)) {
			group.setCanRead(object.getBoolean(JsonKeys.CAN_READ));
		} else {
			group.setCanRead(false);
		}
		if (object.has(JsonKeys.CAN_SEND)) {
			group.setCanSend(object.getBoolean(JsonKeys.CAN_SEND));
		} else {
			group.setCanSend(false);
		}
		if (object.has(JsonKeys.CAN_LEAVE)) {
			group.setCanLeave(object.getBoolean(JsonKeys.CAN_LEAVE));
		} else {
			group.setCanLeave(false);
		}
		if (object.has(JsonKeys.ADMIN)) {
			group.setIsAdmin(object.getBoolean(JsonKeys.ADMIN));
		} else {
			group.setIsAdmin(false);
		}
		if (object.has(JsonKeys.PICTURE)) {
			group.setPicture(Long.parseLong(object.getString(JsonKeys.PICTURE)));
		} else {
			group.setPicture(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.LOCALID)) {
			group.setLocalId(object.getLong(JsonKeys.LOCALID));
		} else {
			group.setLocalId(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.PHONE)) {
			group.setPhone(object.getString(JsonKeys.PHONE));
		} else {
			group.setPhone(ConstantKeys.STRING_DEFAULT);
		}
		return group;
	}

	public static TimelineObject jsonToTimelineObject(String json)
			throws JSONException {
		TimelineObject timeline = new TimelineObject();
		JSONObject object = new JSONObject(json);

		if (object.has(JsonKeys.ID)) {
			timeline.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		} else {
			timeline.setId(ConstantKeys.LONG_DEFAULT);
		}
		if (object.has(JsonKeys.OWNER_ID)) {
			timeline.setOwnerId(Long.parseLong(object
					.getString(JsonKeys.OWNER_ID)));
		} else {
			timeline.setOwnerId(ConstantKeys.LONG_DEFAULT);
		}

		if (object.has(JsonKeys.STATE)) {
			timeline.setState(State.valueOf(object.getString(JsonKeys.STATE)));
		}

		if (object.has(JsonKeys.PARTY)) {
			JSONObject partyObject = object.getJSONObject(JsonKeys.PARTY);

			if (partyObject.has(JsonKeys.ID)) {
				timeline.getParty().setId(
						Long.parseLong(partyObject.getString(JsonKeys.ID)));
			} else {
				timeline.getParty().setId(ConstantKeys.LONG_DEFAULT);
			}
			if (partyObject.has(JsonKeys.NAME)) {
				timeline.getParty().setName(
						partyObject.getString(JsonKeys.NAME));
			} else {
				timeline.getParty().setName(ConstantKeys.STRING_DEFAULT);
			}
			if (partyObject.has(JsonKeys.TYPE)) {
				timeline.getParty().setType(
						partyObject.getString(JsonKeys.TYPE));
			} else {
				// we don't want a null
				timeline.getParty().setType(ConstantKeys.GROUP);
			}

		} else {
			timeline.getParty().setId(ConstantKeys.LONG_DEFAULT);
			timeline.getParty().setName(ConstantKeys.STRING_DEFAULT);
			timeline.getParty().setType(ConstantKeys.GROUP);
		}

		return timeline;
	}

	public static AccountReadInfoResponse jsonToAccountInfo(String json)
			throws JSONException {
		AccountReadInfoResponse info = new AccountReadInfoResponse();
		JSONObject object = new JSONObject(json);

		if (object.has(JsonKeys.ID)) {
			info.setId(Long.parseLong(object.getString(JsonKeys.ID)));
		} else {
			info.setId(ConstantKeys.LONG_DEFAULT);
		}

		if (object.has(JsonKeys.ACCOUNT_NAME)) {
			info.setName(object.getString(JsonKeys.ACCOUNT_NAME));
		} else {
			info.setName(ConstantKeys.STRING_DEFAULT);
		}

		if (object.has(JsonKeys.USER_AUTO_REGISTER)) {
			info.setUserAutoregister((object
					.getBoolean(JsonKeys.USER_AUTO_REGISTER)));
		} else {
			info.setUserAutoregister(false);
		}

		if (object.has(JsonKeys.GROUP_AUTO_REGISTER)) {
			info.setGroupAutoregister((object
					.getBoolean(JsonKeys.GROUP_AUTO_REGISTER)));
		} else {
			info.setGroupAutoregister(false);
		}

		return info;
	}
}
