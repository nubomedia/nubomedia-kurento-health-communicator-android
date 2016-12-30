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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GettingCommandsStatusAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CommandStartServiceReceiver extends BroadcastReceiver {

	private static final Logger log = LoggerFactory
			.getLogger(CommandStartServiceReceiver.class.getSimpleName());

	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null
				&& activeNetwork.isConnectedOrConnecting();

		if (!isConnected) {
			log.warn("No connection actived");
			return;
		}

		Intent service = new Intent(context.getApplicationContext(),
				CommandSendCommandsService.class);
		context.startService(service);

		boolean isGettingCommandsError = GettingCommandsStatusAccess
				.getInstance(context.getApplicationContext())
				.getCommandsStatus(context.getApplicationContext());

		if (isGettingCommandsError) {
			context.startService(new Intent(context.getApplicationContext(),
					CommandGetService.class));
		}
	}
}
