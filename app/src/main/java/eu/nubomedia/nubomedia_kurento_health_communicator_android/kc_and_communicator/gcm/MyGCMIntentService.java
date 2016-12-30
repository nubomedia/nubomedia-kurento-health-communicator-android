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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.gcm;

import android.graphics.BitmapFactory;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandGetService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.QoSservice;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.media.RingtoneManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kurento.agenda.services.pojo.UserCreate;

import java.util.ArrayList;
import java.util.List;

public class MyGCMIntentService extends IntentService {

	public MyGCMIntentService() {
		super("MyGCMIntentService");
	}

	private static final Logger log = LoggerFactory
			.getLogger(MyGCMIntentService.class.getSimpleName());

	public static String NOTIFICATION_IMAGE = "ic_launcher";
	public static String NOTIFICATION_PACKAGE = "package_to_open";
	public static String NOTIFICATION_CLASS = "class_to_open";

	private static int newMessages = 0;
	private static String newMessagePartyName = "";
	private static Long newMessageTimelineId = 0l;
	private static List<String> newMessageContent = new ArrayList<String>();
	private static Boolean newMessagesFromDifferentUsers = false;

	public static void generateNotification(Context context) {
        String message = (String) context.getText(R.string.notification_body);
        String titleSingle = (String) context.getText(R.string.notification_title_single);
		String titlePlural = (String) context.getText(R.string.notification_title_plural);
		String titleFrom = (String) context.getText(R.string.notification_title_from);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_launcher_light)
				.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
				.setVibrate(new long[]{0, 1000, 0, 0, 0}) // {delay, vibrate, sleep, vibrate, sleep}
				.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

		if (newMessagesFromDifferentUsers) {
			mBuilder.setContentTitle(newMessages + " " + context.getText(R.string.notification_new_messages)
					+ " " + titlePlural).setContentText(message);
		} else {
			NotificationCompat.InboxStyle inboxStyle =
					new NotificationCompat.InboxStyle();

			if (newMessages == 1) {
				mBuilder.setContentTitle(newMessages + " " + titleSingle + " " + titleFrom + " "
						+ newMessagePartyName).setContentText(newMessageContent.get(0));
				inboxStyle.setBigContentTitle(newMessages + " " + titleSingle + " " + titleFrom + " "
						+ newMessagePartyName);
				inboxStyle.addLine(newMessageContent.get(0));
			} else {
				mBuilder.setContentTitle(newMessages + " " + titlePlural + " " + titleFrom + " "
						+ newMessagePartyName).setContentText(message);
				inboxStyle.setBigContentTitle(newMessages + " " + titlePlural + " " + titleFrom + " "
						+ newMessagePartyName);

				for (int i=0; i < newMessageContent.size(); i++) {
					inboxStyle.addLine(newMessageContent.get(i));
				}
				if (newMessages > 2) {
					inboxStyle.addLine((String)context.getText(R.string.notification_dots) + (newMessages-2) + " "
							+ context.getText(R.string.notification_more_messages)
							+ context.getText(R.string.notification_dots));
				}
			}

			mBuilder.setStyle(inboxStyle);
		}

		int myPackage = context.getResources().getIdentifier(
				NOTIFICATION_PACKAGE, ConstantKeys.STRING,
				context.getPackageName());

		int myClass = context.getResources().getIdentifier(NOTIFICATION_CLASS,
				ConstantKeys.STRING, context.getPackageName());

		ComponentName c = new ComponentName(context.getString(myPackage),
                context.getString(myClass));
        Intent notificationIntent = new Intent();
		notificationIntent.setComponent(c);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notificationIntent.putExtra(ConstantKeys.NOTIFICATION, newMessageTimelineId);
		PendingIntent intent = PendingIntent.getActivity(context, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mBuilder.setContentIntent(intent);

		NotificationManager mNotifyMgr =
				(NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		mNotifyMgr.notify(0, mBuilder.build());
	}

	public static void processNewMessageToNotify(MessageObject newMessage) {
		newMessages++;

		if (newMessagesFromDifferentUsers) {
			return;
		}

		if ((newMessages > 1) && (!newMessage.getPartyId().equals(newMessageTimelineId))) {
			newMessagesFromDifferentUsers = true;
			newMessageTimelineId = null;
		} else {
			newMessagePartyName = newMessage.getPartyName();
			newMessageTimelineId = newMessage.getPartyId();

			if (newMessages <= 2) {
				newMessageContent.add(newMessage.getBody());
			}
		}
	}

	public static void resetNotificationValues() {
		newMessages = 0;
		newMessagePartyName = "";
		newMessageTimelineId = 0l;
		newMessageContent = new ArrayList<String>();
		newMessagesFromDifferentUsers = false;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */
			if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
					.equals(messageType)) {
				log.warn("Gcm send error: " + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
					.equals(messageType)) {
				log.warn("Gcm deleted: " + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
					.equals(messageType)) {
				log.warn("Gcm message: " + extras.toString());
				onMessage(intent);
			}

			MyGCMBroadcastReceiver.completeWakefulIntent(intent);
		}
	}

	public void onMessage(Intent intent) {
		boolean isQoSmsg = intent.getExtras().containsKey(
				UserCreate.QOS_GCM_DATA_KEY);
		if (isQoSmsg) {
			String gcmQoStimeoutStr = intent.getExtras().getString(
					UserCreate.QOS_GCM_DATA_KEY);
			long gcmQoStimeout = 0L;
			try {
				gcmQoStimeout = Long.parseLong(gcmQoStimeoutStr);
			} catch (NumberFormatException e) {
				log.warn("Error parsing gcmQoStimeout", e);
			}

			log.trace("GCM onMessage: qos timeout: " + gcmQoStimeout);

			Intent i = new Intent(ConstantKeys.BROADCAST_GCM_QOS_RECEIVED);
			i.putExtra(QoSservice.QOS_GCM_TIMEOUT_KEY, gcmQoStimeout);
			sendBroadcast(i);
		} else
			startService(new Intent(getApplicationContext(),
					CommandGetService.class));
	}

}
