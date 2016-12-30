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

import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.CommandObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;

public class CommandStoreService {

	private static final Logger log = LoggerFactory
			.getLogger(CommandStoreService.class.getSimpleName());

	private final Context mContext;

	public CommandStoreService(Context context) {
		this.mContext = context;
	}

	public boolean createCommandReset(String json, String method)
			throws KurentoCommandException, InvalidDataException,
			NotFoundException {
		Long ts = System.currentTimeMillis() / 1000;

		String channelId = ChannelService.getChannelId(mContext);
		String id = ConstantKeys.STRING_DEFAULT + ts;

		String jsonCreated;
		try {
			jsonCreated = JsonParser.createCommandJson(id, channelId, method,
					json);
		} catch (JSONException e) {
			log.error("error: " + e.getMessage(), e);
			return false;
		}

		CommandObject command = new CommandObject();
		command.setJson(jsonCreated);

		return CommandService
				.sendCommand(mContext, command);
	}

	public boolean createCommand(String json, String method, String object) {
		CommandObject command = newCommand(mContext, json, method, object);
		if (command == null) {
			return false;
		}

		PerformanceMonitor.monitor(mContext,
				PerformanceMonitor.Type.CMD_ENQUEUE, json);
		storeCommand(command, object);

		Intent service = new Intent(mContext.getApplicationContext(),
				CommandSendCommandsService.class);
		mContext.startService(service);

		return true;
	}

	public static CommandObject newCommand(Context ctx, String json,
			String method, String object) {
		Long tsLong = System.currentTimeMillis() / 1000;
		String ts = tsLong.toString();

		String channelId = ChannelService.getChannelId(ctx);
		String id = ConstantKeys.STRING_DEFAULT + ts;

		String jsonCreated;
		try {
			jsonCreated = JsonParser.createCommandJson(id, channelId, method,
					json);
		} catch (JSONException e) {
			log.error("Error creating command", e);
			return null;
		}

		CommandObject command = new CommandObject();
		command.setJson(jsonCreated);
		command.setMedia(object);

		return command;
	}

	private void storeCommand(CommandObject command, String object) {
		DataBasesAccess.getInstance(mContext.getApplicationContext())
				.CommandsToSendDataBase(DataBasesAccess.WRITE, null, command,
						object);
	}

}
