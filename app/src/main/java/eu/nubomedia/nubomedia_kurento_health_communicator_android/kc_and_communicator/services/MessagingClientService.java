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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

public class MessagingClientService {

	private static final Logger log = LoggerFactory
			.getLogger(MessagingClientService.class.getSimpleName());

	private final Context context;

	public MessagingClientService(Context context) {
		this.context = context;
	}

	public Bitmap getContent(String authToken, String contentId,
			String messageId, String timelineId, String localId,
			Boolean avatar, Boolean small, Long contentSize, HttpGet myJob)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		boolean isBig = false;

		String url = context.getString(R.string.url_get_content);

		if (avatar) {
			url = Uri.parse(url).buildUpon().build().toString() + timelineId
					+ "/" + messageId + "/avatar/small";
			isBig = false;
		} else {
			if (small) {
				url = Uri.parse(url).buildUpon().build().toString()
						+ timelineId + "/" + messageId
						+ ConstantKeys.THUMBNAIL_CONTENT;
				isBig = false;
			} else {
				url = Uri.parse(url).buildUpon().build().toString()
						+ timelineId + "/" + messageId + "/" + "content";
				isBig = true;
			}
		}

		HttpResponse response = HttpManager.sendGet(context, url);

		String type = response.getFirstHeader("Content-Type").getValue();
		String name;

		if (localId == null) {
			if (type.equalsIgnoreCase(ConstantKeys.TYPE_IMAGE)) {
				if (avatar) {
					name = contentId + ConstantKeys.AVATAR
							+ ConstantKeys.EXTENSION_JPG;
				} else {
					name = contentId + ConstantKeys.EXTENSION_JPG;
				}
			} else {
				name = contentId + ConstantKeys.EXTENSION_3GP;
			}
		} else {
			if (type.equalsIgnoreCase(ConstantKeys.TYPE_IMAGE)) {
				if (avatar) {
					name = localId + ConstantKeys.AVATAR
							+ ConstantKeys.EXTENSION_JPG;
				} else {
					name = localId + ConstantKeys.EXTENSION_JPG;
				}
			} else {
				name = localId + ConstantKeys.EXTENSION_3GP;
			}
		}

		HttpEntity entity = response.getEntity();
		Bitmap ret = FileUtils.entityToBitmap(context, entity, name, isBig,
				contentSize);

		try {
			entity.consumeContent();
		} catch (IOException e) {
			log.error("Cannot consume content", e);
		}

		return ret;
	}

	public JSONArray retrieveMessages(String timelineId, String lastMessage)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		String url = Uri
				.parse(context.getString(R.string.url_retrieve_message_1))
				.buildUpon().build().toString()
				+ timelineId
				+ context.getString(R.string.url_retrieve_message_2);

		if (lastMessage != ConstantKeys.STRING_DEFAULT) {
			url = Uri
					.parse(url)
					.buildUpon()
					.appendQueryParameter(JsonKeys.MAX_MESSAGE,
							Preferences.getMaxMessages(context))
					.appendQueryParameter(JsonKeys.LAST_MESSAGE, lastMessage)
					.build().toString();
		} else {
			url = Uri
					.parse(url)
					.buildUpon()
					.appendQueryParameter(JsonKeys.MAX_MESSAGE,
							Preferences.getMaxMessages(context)).build()
					.toString();
		}

		HttpResp<String> resp = HttpManager.sendGetString(context, url);

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK) {
			String msg = "Cannot get user data (" + code + ")";
			log.warn(msg);
			throw new TransportException(msg);
		}

		String ret = resp.getBody();

		if (ret == null) {
			log.error("Error retrieving messages");
			return null;
		}

		try {
			JSONArray jsonObject = new JSONArray(ret);
			return jsonObject;
		} catch (JSONException e) {
			log.error("Error parsing JSON: ", e);
			return null;
		}
	}

}
