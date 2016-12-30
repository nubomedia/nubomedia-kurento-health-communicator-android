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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class CommandObject {

	public enum SendStatus {
		NONE, PROCESSING;

		public static SendStatus getValueOf(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return NONE;
			}
		}
	}

	private String id;
	private String json;
	private String media = ConstantKeys.STRING_DEFAULT;
	private SendStatus sendStatus = SendStatus.NONE;

	public CommandObject() {
		id = String.valueOf(System.nanoTime());
	}

	public CommandObject(String id) {
		this.id = id;
	}

	public String getID() {
		return id;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public SendStatus getSendStatus() {
		return sendStatus;
	}

	public void setSendStatus(SendStatus sendStatus) {
		this.sendStatus = sendStatus;
	}

}
