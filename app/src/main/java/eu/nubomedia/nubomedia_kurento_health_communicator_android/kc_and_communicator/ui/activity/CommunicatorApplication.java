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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStartServiceReceiver;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.QoSservice;

public class CommunicatorApplication extends Application {

	private static final long REPEAT_TIME = 1000 * 30; // * 60 * 24;
	private static Context context;

	@Override
	public void onCreate() {
		CommunicatorApplication.context = getApplicationContext();

		startService(new Intent(getApplicationContext(), QoSservice.class));

		launchAlarm(getApplicationContext());
	}

	public static void launchAlarm(Context context) {
		AlarmManager service = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context.getApplicationContext(),
				CommandStartServiceReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(
				context.getApplicationContext(), 0, i,
				PendingIntent.FLAG_CANCEL_CURRENT);
		service.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + REPEAT_TIME, REPEAT_TIME, pending);

	}

	public static Context getAppContext() {
		return CommunicatorApplication.context;
	}
}
