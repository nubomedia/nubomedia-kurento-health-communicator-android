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
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.ContentReadResponse;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;

public class SendMessageAsyncTask extends AsyncTask<Void, Void, String> {

	private static final Logger log = LoggerFactory
			.getLogger(SendMessageAsyncTask.class.getSimpleName());

	private static final Executor EXECUTOR = Executors
			.newSingleThreadExecutor();

	private String messageBody;
	private String realPath = null;
	private boolean mediaFromGallery;
	private MessageObject message;
	private Context ctx;
	private TimelineObject timeline;
	private AccountManager am;
	private Account account;
	private Long actualTimeStamp;
	private Object messageApp;

	public SendMessageAsyncTask(Context ctx, String messageBody,
								Uri mediaFileUri, boolean mediaFromGallery,
								TimelineObject timeline, Long actualTimeStamp, Object messageApp) {
		this.ctx = ctx;
		this.messageBody = messageBody;
		if (mediaFileUri != null) {
			this.realPath = FileUtils.getRealPathFromURI(mediaFileUri, ctx);
		}
		this.mediaFromGallery = mediaFromGallery;
		this.timeline = timeline;
		this.am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);
		this.account = AccountUtils.getAccount(ctx, true);

		this.actualTimeStamp = actualTimeStamp;
		this.messageApp = messageApp;

		PerformanceMonitor.monitor(
				ctx,
				PerformanceMonitor.Type.MSG_USER_CREATE,
				new PerformanceMonitor.Message.Builder()
						.localId(actualTimeStamp.toString())
						.from(am.getUserData(account, JsonKeys.ID_STORED))
						.build());
	}

	private MessageObject createMessage() {
		ContentReadResponse content = new ContentReadResponse();
		if (realPath != null) {
			File auxFile = new File(realPath);
			long fileLenght = auxFile.length();
			auxFile = null;
			System.gc();

			content.setId(Long.valueOf(actualTimeStamp));
			content.setContentSize(fileLenght);
			if ((realPath.contains(ConstantKeys.EXTENSION_JPG))
					|| (realPath.contains(ConstantKeys.EXTENSION_PNG))
					|| (realPath.contains(ConstantKeys.EXTENSION_JPEG))) {
				content.setContentType(ConstantKeys.IMAGE);
			} else {
				content.setContentType(ConstantKeys.VIDEO);
			}
		} else {
			content.setId((long) 0);
			content.setContentSize((long) 0);
			content.setContentType(ConstantKeys.STRING_DEFAULT);
		}

		// we store the message
		MessageObject message = new MessageObject();
		message.setBody(messageBody);

		message.setId((long) 0);
		message.setLocalId(actualTimeStamp);

		message.getTimeline().setId(timeline.getId());
		message.setPartyId(timeline.getParty().getId());
		message.setPartyType(timeline.getParty().getType());

		Date date = new Date(Long.valueOf(actualTimeStamp)
				- timeline.getTimestampDrift());
		message.setTimestamp(date.getTime());

		UserReadAvatarResponse from = new UserReadAvatarResponse();
		from.setId(Long.valueOf(am.getUserData(account, JsonKeys.ID_STORED)));
		from.setName(am.getUserData(account, JsonKeys.NAME));
		from.setPicture(Long.valueOf(am.getUserData(account, JsonKeys.PICTURE)));
		from.setSurname(am.getUserData(account, JsonKeys.SURNAME));
		message.setFrom(from);

		message.setContent(content);
		message.setTotal(1);
		message.incStatusAndUpdate(MessageObject.Status.SENDING, ctx);

		if (messageApp != null) {
			message.setApp(messageApp.toString());
		}

		return message;
	}

	@Override
	protected void onPreExecute() {
		message = createMessage();
		DataBasesAccess.getInstance(ctx).MessagesDataBaseWrite(message);
		// There is a new message on database
		Intent intent = new Intent();
		intent.setAction(ConstantKeys.BROADCAST_MESSAGE_CREATED);
		intent.putExtra(ConstantKeys.MESSAGE_CREATED_EXTRA, message);
		ctx.sendBroadcast(intent);
	}

	@Override
	protected String doInBackground(Void... params) {
		// First we need to send the create timeline is need it
		if (timeline.getId() == 0) {
			String returned = ConstantKeys.SENDING_FAIL;
			if (timeline.getParty().getType().equals(JsonKeys.GROUP)) {
				returned = AppUtils.actionOverTimeline(
						ctx.getApplicationContext(),
						Command.METHOD_CREATE_TIMELINE,
						Long.valueOf(timeline.getParty().getId()),
						JsonKeys.GROUP, timeline.getParty().getName(), true);
			} else if (timeline.getParty().getType().equals(JsonKeys.USER)) {
				returned = AppUtils.actionOverTimeline(
						ctx.getApplicationContext(),
						Command.METHOD_CREATE_TIMELINE,
						Long.valueOf(timeline.getParty().getId()),
						JsonKeys.USER, timeline.getParty().getName(), true);
			}

			// There was an error, we don't send the message
			if (returned.equals(ConstantKeys.SENDING_FAIL)) {
				return ConstantKeys.SENDING_FAIL;
			}
		}

		// here we have a timeline on server or we have send a command
		// to background, so we can set the actual timeline to visible
		DataBasesAccess.getInstance(ctx.getApplicationContext())
				.TimelinesDataBaseIsRecoverTimeline(
						Long.valueOf(timeline.getParty().getId()));

		if (realPath != null) {
			if (mediaFromGallery) {
				try {
					File fileOri = new File(realPath);
					String ext = ConstantKeys.STRING_DEFAULT;
					if ((realPath.contains(ConstantKeys.EXTENSION_JPG))
							|| (realPath.contains(ConstantKeys.EXTENSION_PNG))
							|| (realPath.contains(ConstantKeys.EXTENSION_JPEG))) {
						ext = ConstantKeys.EXTENSION_JPG;
					} else {
						ext = ConstantKeys.EXTENSION_3GP;
					}
					File fileDest = new File(FileUtils.getDir() + "/"
							+ actualTimeStamp + ext);

					FileUtils.copyFile(fileOri, fileDest);
					FileUtils.createThumnail(fileDest);
				} catch (IOException e) {
					log.error("Error tryning to copy the media to app path", e);
				}
			} else {
				FileUtils.createThumnail(new File(realPath));
			}
		}

		return FileUtils.sendMessageToServer(ctx, realPath, timeline.getParty()
				.getId(), timeline.getParty().getType(), timeline.getId(),
				true, actualTimeStamp, messageBody, messageApp);
	}

	public void execute() {
		super.executeOnExecutor(EXECUTOR);
	}

}
