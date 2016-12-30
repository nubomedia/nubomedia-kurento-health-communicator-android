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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.push.WSPushManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.qos.QoScontroller;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.qos.QoSstate;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;

public class QoSservice extends Service {

	private static final Logger log = LoggerFactory.getLogger(QoSservice.class
			.getSimpleName());

	public static final String QOS_GCM_TIMEOUT_KEY = "qos_gcm_timeout";
	public static final String BROADCAST_QOS_FAIL_MODE_CHANGE = "qos_fail_mode_change";
	public static final String QOS_FAIL_MODE_KEY = "qos_fail_mode";
	public static final String BROADCAST_QOS_ACTIVE_CHANGE = "qos_active_change";
	public static final String QOS_ACTIVE_KEY = "qos_active";
	public static final String BROADCAST_QOS_STATE_CHANGE = "qos_state_change";
	public static final String QOS_STATE_KEY = "qos_state";

	private static CountDownTimer gcmCountDown = null;
	private static CountDownTimer wsCountDown = null;
	private static boolean failModeIsActive = false;

	private boolean receiversAreRegistered = false;

	private static boolean gcmIsOk = true;
	private static boolean wsIsOk = true;

	private static long gcmTimeout = 30000;
	private static long wsTimeout = 30000;

	private static boolean qosServiceNotification = true;

	@Override
	public void onCreate() {
		qosServiceNotification = AppUtils.getResource(getApplicationContext(),
				R.bool.qos_service_notification, true);

		gcmTimeout = Integer.parseInt(getResources().getString(R.string.qos_gcm_timeout));
		wsTimeout = WSPushManager.WS_PING_PERIOD * 2;

		initGCMCountDownTimer();
		initWSCountDownTimer();

		boolean qosActive = AccountUtils.getQosFlag(getApplicationContext());
		QoScontroller.setQosActive(qosActive, getApplicationContext());
		
		onInit();

		updateState();

		registerReceiver(mReceiverQoSActiveChange, intentFilterQoSActiveChange);
	}


	@Override
	public void onDestroy() {
		unregisterReceiver(mReceiverQoSActiveChange);

		unregisterReceivers();

		gcmCountDown.cancel();
		wsCountDown.cancel();

		deactivateFailMode();

		super.onDestroy();
	}

	public void registerReceivers() {
		if (!receiversAreRegistered) {
			registerReceiver(mReceiverGCM, intentFilter);
			registerReceiver(mReceiverWSPing, intentFilterWSPing);
			receiversAreRegistered = true;
		}
	}

	private void unregisterReceivers() {
		if (receiversAreRegistered) {
			unregisterReceiver(mReceiverGCM);
			unregisterReceiver(mReceiverWSPing);
			receiversAreRegistered = false;
		}
	}

	public static void resetGCMCountDown() {
		gcmCountDown.cancel();
		gcmCountDown.start();
	}

	public static void cancelGCMCountDown() {
		gcmCountDown.cancel();
	}

	public static void startGCMCountDown() {
		gcmCountDown.start();
	}

	public static void resetWSCountDown() {
		wsCountDown.cancel();
		wsCountDown.start();
	}

	public static void cancelWSCountDown() {
		wsCountDown.cancel();
	}

	public static void startWSCountDown() {
		wsCountDown.start();
	}

	private void activateFailMode() {
		if (QoScontroller.isQosActive()) {
			if (!failModeIsActive) {
				resetWSCountDown();
				failModeIsActive = true;
				onFailModeChange();
			}
		}
	}

	private void deactivateFailMode() {
		if (failModeIsActive) {
			cancelWSCountDown();
			failModeIsActive = false;
			onFailModeChange();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/* Broadcast receivers */
	protected IntentFilter intentFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM_QOS_RECEIVED);
	protected BroadcastReceiver mReceiverGCM = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long gcmQoStimeout = intent.getExtras()
					.getLong(QOS_GCM_TIMEOUT_KEY);

			if (gcmQoStimeout > 0L) {
				// increment the timeout for preventing network problems and
				// message building delay.
				gcmQoStimeout = (long) ((double) gcmQoStimeout * 1.2);
				updateGcmTimeout(gcmQoStimeout);
			}

			gcmIsOk = true;
			deactivateFailMode();
			updateState();
			if (QoScontroller.isQosActive()) {
				resetGCMCountDown();
			}
		}
	};

	protected IntentFilter intentFilterWSPing = new IntentFilter(
			ConstantKeys.BROADCAST_WS_MSG_RECEIVED);
	protected BroadcastReceiver mReceiverWSPing = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			wsIsOk = true;
			updateState();
			if (QoScontroller.isQosActive() && failModeIsActive) {
				resetWSCountDown();
			}
		}
	};

	protected IntentFilter intentFilterQoSActiveChange = new IntentFilter(
			BROADCAST_QOS_ACTIVE_CHANGE);
	protected BroadcastReceiver mReceiverQoSActiveChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean qosActive = intent.getExtras().getBoolean(
					QoSservice.QOS_ACTIVE_KEY);
			if (qosActive)
				onActivation();
			else
				onDeactivation();
		}
	};

	private void updateState() {
		if (gcmIsOk) {
			QoScontroller.setQosState(QoSstate.CONN_OK,
					getApplicationContext(), qosServiceNotification);
		} else if (wsIsOk) {
			QoScontroller.setQosState(QoSstate.CONN_DEG,
					getApplicationContext(), qosServiceNotification);
		} else {
			QoScontroller.setQosState(QoSstate.CONN_NOK,
					getApplicationContext(), qosServiceNotification);
		}
	}

	public static boolean getFailModeIsActive() {
		return failModeIsActive;
	}

	public void onDeactivation() {
		unregisterReceivers();
		cancelGCMCountDown();
		cancelWSCountDown();
		deactivateFailMode();
	}

	public void onActivation() {
		unregisterReceivers();
		cancelGCMCountDown();
		cancelWSCountDown();
		deactivateFailMode();
		registerReceivers();
		resetGCMCountDown();
	}

	public void onInit() {
		if (QoScontroller.isQosActive()) {
			onActivation();
		} else {
			onDeactivation();
		}
	}

	private void onFailModeChange() {
		Intent i = new Intent(BROADCAST_QOS_FAIL_MODE_CHANGE);
		i.putExtra(QoSservice.QOS_FAIL_MODE_KEY, failModeIsActive);
		sendBroadcast(i);
	}

	public void updateGcmTimeout(long gcmQoStimeout) {
		if (gcmTimeout != gcmQoStimeout) {
			gcmTimeout = gcmQoStimeout;
			initGCMCountDownTimer();
		}
	}

	private void initGCMCountDownTimer() {
		if (gcmCountDown != null) {
			gcmCountDown.cancel();
			gcmCountDown = null;
		}

		gcmCountDown = new CountDownTimer(gcmTimeout, 1000) {
			@Override
			public void onFinish() {
				gcmIsOk = false;
				activateFailMode();
				updateState();
			}

			@Override
			public void onTick(long millisUntilFinished) {
			}
		};
	}

	private void initWSCountDownTimer() {
		if (wsCountDown != null) {
			wsCountDown.cancel();
			wsCountDown = null;
		}
		wsCountDown = new CountDownTimer(wsTimeout, 1000) {
			@Override
			public void onFinish() {
				wsIsOk = false;
				updateState();
			}

			@Override
			public void onTick(long millisUntilFinished) {
			}
		};
	}
}
