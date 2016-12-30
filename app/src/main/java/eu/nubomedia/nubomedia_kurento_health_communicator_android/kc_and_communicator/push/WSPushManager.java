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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.push;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.WebSocket;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.jsonrpc.Message;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.jsonrpc.PingRequest;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.jsonrpc.RegisterRequest;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.jsonrpc.Response;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.ChannelService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandGetService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;

import com.kurento.khc.jsonrpc.JsonRpcRequest;
import com.kurento.khc.jsonrpc.JsonRpcRequest.Method;
import com.kurento.khc.jsonrpc.JsonRpcResult;
import com.kurento.khc.jsonrpc.JsonRpcResult.Code;

import javax.net.ssl.SSLContext;

public class WSPushManager {

	private static final Logger log = LoggerFactory
			.getLogger(WSPushManager.class.getSimpleName());

	private static final long RETRY_CONNECTION_INTERVAL = 15000;
	public static final long WS_PING_PERIOD = 15000L;

	private final Context ctx;
	private static WebSocketClient client;
	private boolean reconnect;
	private Date connectionDate = null;
	private static Timer timer = null;

	public WSPushManager(Context ctx) {
		this.ctx = ctx;
	}

	private synchronized boolean getReconnect() {
		return reconnect;
	}

	private synchronized void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

	public synchronized void connectWS() {
		setReconnect(true);

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

		timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				sendPing();
			}
		}, 1000l, WS_PING_PERIOD);
	}

	public synchronized void closeWS() {
		setReconnect(false);
		if (client != null) {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}

			client.close();
			connectionDate = null;
		}
	}

	private synchronized boolean canReconnect() {
		Date actual = new Date();
		if (connectionDate == null
				|| (actual.getTime() - connectionDate.getTime() > RETRY_CONNECTION_INTERVAL)) {
			connectionDate = actual;
			return true;
		}
		return false;
	}

	private synchronized void reconnectWS() {
		if (!getReconnect()) {
			return;
		}

		if ((client != null && ((client.getReadyState() == WebSocket.READYSTATE.OPEN) || (client.getReadyState() == WebSocket.READYSTATE.CONNECTING)))
				|| !canReconnect()) {
			if (client != null) {
				log.warn("WS cannot reconnect. Client is open: " + (client.getReadyState() == WebSocket.READYSTATE.OPEN)
				+ ". Client is connecting: " + (client.getReadyState() == WebSocket.READYSTATE.CONNECTING)
				+ ". CanRecconect: " + !canReconnect());
			} else {
				log.warn("WS cannot reconnect. Client is null");
			}

			return;
		}

		final String channelId = ChannelService.getChannelId(ctx);
		log.trace("channelId: {}", channelId);

		if (channelId == null) {
			log.warn("There is not channelId");

			if (client != null) {
				client.close();
			}

			return;
		}

		URL urlObj = null;
		try {
			urlObj = GeneralUtils.buildURL(ctx,
					ctx.getString(R.string.url_sync));
		} catch (Exception e) {
			String msg = "Cannot create URL";
			log.error(msg, e);

			return;
		}

		URI uri;
		try {
			uri = urlObj.toURI();
		} catch (URISyntaxException e) {
			log.error("Cannot create URI", e);

			return;
		}

		if (client != null) {
			client.close();
		}

		client = new WebSocketClient(uri, new Draft_17()) {
			@Override
			public void onOpen(ServerHandshake serverHandshake) {
				long chId = Long.parseLong(channelId);
				RegisterRequest regReq = new RegisterRequest(chId);
				sendMessage(regReq);

			}

			@Override
			public void onMessage(String message) {
				ctx.sendBroadcast(new Intent(
						ConstantKeys.BROADCAST_WS_MSG_RECEIVED));

				processMessage(message);

			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				log.trace("WebSocket onClose ({}, {}, {})", new Object[] {
						code, reason, remote });

				reconnectWS();
			}

			@Override
			public void onError(Exception e) {
				log.warn("WebSocket onError", e);

				ctx.sendBroadcast(new Intent(ConstantKeys.BROADCAST_WS_ON_ERROR));

				reconnectWS();
			}
		};

		String protocol = Preferences.getServerProtocol(ctx);
		if (HttpManager.PROTOCOL_HTTPS.equalsIgnoreCase(protocol)) {
			try {
				SSLContext sslContext = null;
				try {
					sslContext = SSLContext.getDefault();//.getInstance( "TLS" );
					sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (KeyManagementException e) {
					e.printStackTrace();
				}

				client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
			} catch (Exception e) {
				log.warn("Cannot create WSS support", e);
			}
		}

		log.trace("WS connecting to {}", uri);

		client.connect();
	}

	private void sendMessage(Message msg) {
		String json;
		try {
			json = msg.toJson();
		} catch (Exception e) {
			log.error("Cannot send msg", e);
			return;
		}

		if ((client == null) || (client.getReadyState() != WebSocket.READYSTATE.OPEN)) {
			log.error("Cannot send WS message. WS not connected.");
			connectionDate = null;
			reconnectWS();

			return;
		}
		client.send(json);
	}

	private void sendPing() {
		PingRequest req = new PingRequest();
		sendMessage(req);
	}

	private void processMessage(String msgStr) {
		try {
			JsonRpcRequest req = JacksonManager.fromJson(msgStr,
					JsonRpcRequest.class);

			if (Method.SYNC == req.getMethod()) {
				JsonRpcResult result = new JsonRpcResult();
				result.setCode(Code.OK);
				Response resp = new Response(req, result);
				sendMessage(resp);

				ctx.startService(new Intent(ctx, CommandGetService.class));
			}
			return;
		} catch (Exception e) {
			log.trace("Message received is not a request");
		}
	}

	public static boolean isWebSocketOpen() {
		if (client != null) {
			return (client.getReadyState() == WebSocket.READYSTATE.OPEN);
		}

		return false;
	}
}
