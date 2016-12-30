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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.qos;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.QoSservice;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;

public class QoScontroller {

	private static final Logger log = LoggerFactory
			.getLogger(QoScontroller.class.getSimpleName());

	public static final int NOTIFICATION_LIGHTS_ON_MS = 3000;
	public static final int NOTIFICATION_LIGHTS_OFF_MS = 3000;
	public static final int NOTIFICATION_LIGTHS_CONN_DEG_COLOR = Color.rgb(255,
			153, 0);
	public static final int NOTIFICATION_LIGTHS_CONN_NOK_COLOR = Color.rgb(255,
			0, 0);
	public static final long[] NOTIFICATION_VIBRATE_PATTERN = { 2000, 2000,
			2000 };

	public static final int NOTIFICATION_ID = 12345;

	private static QoSstate qosState = QoSstate.CONN_NOK;
	private static boolean qosActive = false;

	private static NotificationManager mNotificationManager = null;

	public synchronized static QoSstate getQosState() {
		return qosState;
	}

	public synchronized static void setQosState(QoSstate qosState, Context ctx,
			boolean showNotification) {
		if (!qosState.equals(QoScontroller.qosState)) {
			QoScontroller.qosState = qosState;
			onStateChange(ctx);
			if (showNotification && isQosActive())
				showNotification(ctx);
		}
	}

	public synchronized static boolean isQosActive() {
		log.trace("isQosActive():" + qosActive);
		return qosActive;
	}

	public synchronized static void setQosActive(boolean qosActive, Context ctx) {
		if (qosActive != QoScontroller.qosActive) {
			QoScontroller.qosActive = qosActive;
			onActiveChange(ctx);
		}
	}

	private static void onActiveChange(Context ctx) {
		if (ctx != null) {
			Intent i = new Intent(QoSservice.BROADCAST_QOS_ACTIVE_CHANGE);
			i.putExtra(QoSservice.QOS_ACTIVE_KEY, qosActive);
			ctx.sendBroadcast(i);
		}
	}

	private static void onStateChange(Context ctx) {
		if (ctx != null) {
			Intent i = new Intent(QoSservice.BROADCAST_QOS_STATE_CHANGE);
			i.putExtra(QoSservice.QOS_STATE_KEY, qosState);
			ctx.sendBroadcast(i);
		}
	}

	public static void showNotification(Context ctx) {
		if (ctx == null)
			return;

		String appName = ctx.getString(R.string.app_name);

		if (mNotificationManager == null)
			mNotificationManager = (NotificationManager) ctx
					.getSystemService(Context.NOTIFICATION_SERVICE);

		String message = null;
		int ledColor;
		if (getQosState().equals(QoSstate.CONN_DEG)) {
			message = (String) ctx.getText(R.string.qos_connection_deg);
			ledColor = NOTIFICATION_LIGTHS_CONN_DEG_COLOR;
		} else if (getQosState().equals(QoSstate.CONN_NOK)) {
			message = (String) ctx.getText(R.string.qos_connection_nok);
			ledColor = NOTIFICATION_LIGTHS_CONN_NOK_COLOR;
		} else {
			mNotificationManager.cancel(appName, NOTIFICATION_ID);
			return;
		}

		String title = (String) ctx.getText(R.string.qos_dialog_title);
		int drawable_id = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();

		Class<?> className = AppUtils.getClassByName(ctx,
				R.string.main_activity_package);
		if (className != null) {
			Intent notificationIntent = new Intent(ctx, className);
			notificationIntent.setComponent(new ComponentName(ctx, className));
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			PendingIntent intent = PendingIntent.getActivity(ctx, 0,
					notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Notification.Builder mBuilder = new Notification.Builder(ctx);
			mBuilder.setContentTitle(title);
			mBuilder.setContentText(message);
			mBuilder.setSmallIcon(drawable_id);
			mBuilder.setWhen(when);
			mBuilder.setContentIntent(intent);
			mBuilder.setAutoCancel(true);
			if (Preferences
					.isNotificationActivated(ctx.getApplicationContext()))
				mBuilder.setSound(RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
			mBuilder.setVibrate(NOTIFICATION_VIBRATE_PATTERN);
			mBuilder.setLights(ledColor, NOTIFICATION_LIGHTS_ON_MS,
					NOTIFICATION_LIGHTS_OFF_MS);

			mNotificationManager.notify(appName, NOTIFICATION_ID,
					mBuilder.build());
		}
	}
}
