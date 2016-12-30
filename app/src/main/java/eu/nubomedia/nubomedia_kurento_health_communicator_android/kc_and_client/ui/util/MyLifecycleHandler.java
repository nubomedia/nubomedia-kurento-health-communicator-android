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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.util;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.gcm.MyGCMIntentService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.push.WSPushManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.qos.QoScontroller;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.QoSservice;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class MyLifecycleHandler implements ActivityLifecycleCallbacks {
	protected static final Logger log = LoggerFactory
			.getLogger(MyLifecycleHandler.class.getSimpleName());
	protected static WSPushManager wsPushManager;
	protected static Context context;

	protected static int resumed;
	protected static int paused;
	protected static int started;
	protected static int stopped;

	public MyLifecycleHandler(Context ctx) {
		context = ctx;
		context.registerReceiver(mReceiverQoSFailModeChange,
				qosFailModeChangeIntentFilter);

		if (QoSservice.getFailModeIsActive())
			init();
		else
			closeWS();
	}

	public static void close() {
		if (wsPushManager != null) {
			wsPushManager.closeWS();
		}

		QoScontroller.setQosActive(false, context);
	}

	/* Broadcast receivers */
	protected IntentFilter qosFailModeChangeIntentFilter = new IntentFilter(
			QoSservice.BROADCAST_QOS_FAIL_MODE_CHANGE);
	protected BroadcastReceiver mReceiverQoSFailModeChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			boolean failModeActive = intent.getExtras().getBoolean(
					QoSservice.QOS_FAIL_MODE_KEY);
			log.debug("FailModeIsActive: " + failModeActive);
			if (failModeActive)
				init();
			else
				closeWS();
		}
	};

	public static void init() {
		if (wsPushManager == null) {
			wsPushManager = new WSPushManager(context);
		}

		wsPushManager.connectWS();
	}

	public static boolean isApplicationVisible() {
		return started > stopped;
	}

	public static boolean isApplicationInForeground() {
		return resumed > paused;
	}

	@Override
	public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
	}

	@Override
	public void onActivityDestroyed(Activity activity) {
	}

	@Override
	public void onActivityResumed(Activity activity) {
		if (!MyLifecycleHandler.isApplicationInForeground()) {
			MyGCMIntentService.resetNotificationValues();

			Account ac = AccountUtils.getAccount(context, true);
			if (ac != null) {
				if (wsPushManager == null) {
					MyLifecycleHandler.init();
				}
				wsPushManager.connectWS();
			}
		}

		++resumed;
	}

	@Override
	public void onActivityPaused(Activity activity) {
		++paused;
		android.util.Log.w("test", "application is in foreground: "
				+ (resumed > paused));
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
	}

	@Override
	public void onActivityStarted(Activity activity) {
		if (!MyLifecycleHandler.isApplicationVisible()) {
			MyGCMIntentService.resetNotificationValues();

			Account ac = AccountUtils.getAccount(context, true);
			if (ac != null) {
				if (wsPushManager == null) {
					MyLifecycleHandler.init();
				}
				wsPushManager.connectWS();
			}
		}

		++started;
	}

	@Override
	public void onActivityStopped(Activity activity) {
		++stopped;

		if (!QoSservice.getFailModeIsActive()) {
			closeWS();
		}
	}

	protected static void closeWS() {
		if (!MyLifecycleHandler.isApplicationVisible()) {
			Account ac = AccountUtils.getAccount(context, true);
			if ((ac != null) && (wsPushManager != null)) {
				wsPushManager.closeWS();
			}
		}
	}
}