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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.CommandObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.CustomMultiPartEntity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.PerformanceMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CommandService {

	private static final Logger log = LoggerFactory
			.getLogger(CommandService.class.getSimpleName());

	private static final int PERCENT_INC = 5;

	private static final String COMMAND_PART_NAME = "command";
	private static final String CONTENT_PART_NAME = "content";

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

	private static HttpResp<Void> sendCommandWithMedia(final Context ctx,
													   CommandObject cmd) throws KurentoCommandException,
			TransportException, InvalidDataException, NotFoundException {
		String contentPath = cmd.getMedia();

		File content = new File(contentPath);
		String mimeType;
		if (content.exists() && content.isFile()) {
			if (contentPath.contains(ConstantKeys.EXTENSION_JPG)) {
				mimeType = ConstantKeys.TYPE_IMAGE;
			} else {
				mimeType = ConstantKeys.TYPE_VIDEO;
			}
		} else {
			String error = contentPath + " does not exists or is not a file";
			log.error(error);
			throw new KurentoCommandException(error);
		}

		CustomMultiPartEntity mpEntity;
		final String json = cmd.getJson();
		String charset = HTTP.UTF_8;
		long contentSize = 0;
		try {
			contentSize = content.length();
			/* Aprox. total size */
			final long totalSize = content.length()
					+ json.getBytes("UTF-16BE").length;
			mpEntity = new CustomMultiPartEntity(new CustomMultiPartEntity.ProgressListener() {

				private int i;

				@Override
				public void transferred(long num) {
					int totalpercent = Math.min(100,
							(int) ((num * 100) / totalSize));

					if (totalpercent > (1 + PERCENT_INC * i)
							|| totalpercent >= 100) {
						Intent intent = new Intent();
						intent.setAction(ConstantKeys.BROADCAST_PROGRESSBAR);
						intent.putExtra(ConstantKeys.JSON, json);
						intent.putExtra(ConstantKeys.TOTAL, totalpercent);
						ctx.sendBroadcast(intent);
						i++;
					}
				}
			});

			mpEntity.addPart(
					COMMAND_PART_NAME,
					new StringBody(json,
							HttpManager.CONTENT_TYPE_APPLICATION_JSON, Charset
									.forName(charset)));

			FormBodyPart fbp = new FormBodyPart(CONTENT_PART_NAME,
					new FileBody(content, mimeType));
			fbp.addField(HTTP.CONTENT_LEN, String.valueOf(contentSize));
			mpEntity.addPart(fbp);

		} catch (UnsupportedEncodingException e) {
			String msg = "Cannot use " + charset + "as entity";
			log.error(msg, e);
			throw new TransportException(msg);
		}

		return HttpManager.sendPostVoid(ctx,
				ctx.getString(R.string.url_command), mpEntity);
	}

	public static boolean sendCommand(Context ctx, CommandObject cmd)
			throws KurentoCommandException, InvalidDataException,
			NotFoundException {
		PerformanceMonitor.monitor(ctx,
				PerformanceMonitor.Type.CMD_SEND_REQUEST_START, cmd.getJson());

		HttpResp<Void> resp;

		try {
			if (cmd.getMedia().length() > 2) {
				resp = sendCommandWithMedia(ctx, cmd);
			} else {
				resp = HttpManager.sendPostVoid(ctx,
						ctx.getString(R.string.url_command), cmd.getJson());
			}
		} catch (TransportException e) {
			log.error("Cannot send command", e);
			return false;
		}

		PerformanceMonitor.monitor(ctx,
				PerformanceMonitor.Type.CMD_SEND_REQUEST_FINISH, cmd.getJson());

		return resp.getCode() == HttpStatus.SC_CREATED;
	}

	public static boolean sendCommandTransaction(Context ctx,
			List<CommandObject> cmdList) throws InvalidDataException,
			KurentoCommandException, NotFoundException {
		String prefix = "";
		StringBuilder b = new StringBuilder("[");
		for (CommandObject cmd : cmdList) {
			b.append(prefix);
			prefix = ", ";
			b.append("[").append(cmd.getJson()).append("]");
		}
		b.append("]");
		String json = b.toString();

		for (CommandObject cmd : cmdList) {
			PerformanceMonitor.monitor(ctx,
					PerformanceMonitor.Type.CMD_SEND_REQUEST_START,
					cmd.getJson());
		}

		HttpResp<Void> resp;
		try {
			resp = HttpManager.sendPostVoid(ctx,
					ctx.getString(R.string.url_command_transaction), json);
		} catch (TransportException e) {
			log.error("Cannot send command", e);
			return false;
		}

		for (CommandObject cmd : cmdList) {
			PerformanceMonitor.monitor(ctx,
					PerformanceMonitor.Type.CMD_SEND_REQUEST_FINISH,
					cmd.getJson());
		}

		return resp.getCode() == HttpStatus.SC_CREATED;
	}

	public static String getCommands(Context ctx, String lastSeq,
			String channelId) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		if (channelId == null) {
			log.warn("Cannot get commands with channelId: {}", channelId);
			ChannelService.createChannel(ctx);

			return null;
		}

		String internalLastSeq = lastSeq;
		if (internalLastSeq == null || internalLastSeq.isEmpty()) {
			internalLastSeq = "0";
		}

		log.debug("getComnands for lastSeq: {}, channelId: {}",
				internalLastSeq, channelId);

		String resource = Uri.parse(ctx.getString(R.string.url_command))
				.buildUpon()
				.appendQueryParameter(JsonKeys.LAST_SEQUENCE, internalLastSeq)
				.appendQueryParameter(JsonKeys.CHANNEL_ID, channelId).build()
				.toString();

		HttpResp<String> resp = HttpManager.sendGetString(ctx, resource);

		return resp.getBody();
	}

}
