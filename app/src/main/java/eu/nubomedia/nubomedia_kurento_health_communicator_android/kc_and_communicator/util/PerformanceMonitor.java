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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

import com.google.gson.Gson;

public class PerformanceMonitor {

	private static final Logger log = LoggerFactory
			.getLogger(PerformanceMonitor.class.getSimpleName());

	/* @formatter:off */
	public enum Type {
		MSG_USER_CREATE, CMD_ENQUEUE, CMD_DEQUEUED, CMD_SEND_REQUEST_START, CMD_SEND_REQUEST_FINISH, MSG_RECEIVED
	}

	/* @formatter:on */

	private static final Gson gson = new Gson();

	public static void monitor(Context context, Type type, Message message) {
		monitor(context, type, gson.toJson(message));
	}

	public static void monitor(Context context, Type type, String body) {
		String channelId = ChannelService.getChannelId(context);
		Entry entry = new Entry(type, channelId, body);
		String json = gson.toJson(entry);
		log.trace(json);
	}

	public static class Message {
		private String from;
		private String to;
		private String localId;
		private String id;
		private long attachmentSize;

		private Message(Builder b) {
			this.from = b.from;
			this.to = b.to;
			this.localId = b.localId;
			this.id = b.id;
			this.attachmentSize = b.attachmentSize;
		}

		public static class Builder {
			private String from;
			private String to;
			private String localId;
			private String id;
			private long attachmentSize;

			public Builder() {

			}

			public Builder from(String from) {
				this.from = from;
				return this;
			}

			public Builder to(String to) {
				this.to = to;
				return this;
			}

			public Builder localId(String localId) {
				this.localId = localId;
				return this;
			}

			public Builder id(String id) {
				this.id = id;
				return this;
			}

			public Builder attachmentSize(long attachmentSize) {
				this.attachmentSize = attachmentSize;
				return this;
			}

			public Message build() {
				return new Message(this);
			}
		}
	}

	private static class Entry {
		private Type type;
		private long timestamp;
		private String channelId;
		private String body;

		public Entry(Type type, String channelId, String body) {
			this.type = type;
			this.timestamp = System.currentTimeMillis();
			this.channelId = channelId;
			this.body = body;
		}
	}

}
