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

import android.graphics.Color;

import com.kurento.agenda.services.pojo.TimelinePartyUpdate;
import com.kurento.agenda.services.pojo.TimelineReadResponse;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class TimelineObject extends TimelineReadResponse {

	private boolean newMessages = false;
	private Long lastMessageId;
	private Long lastMessageTimestamp;
	private String lastMessageBody = ConstantKeys.STRING_DEFAULT;
	private boolean showIt = true;
	private Long timestampDrift = ConstantKeys.LONG_DEFAULT;
	private int backgroundColor = Color.TRANSPARENT;
	private boolean isAnimated = false;
	private String statusToPaint = "";

	private int newIconSize;
	private int tagResourceId;

	public TimelineObject() {
		setParty(new TimelinePartyUpdate());
	}

	public boolean isNewMessages() {
		return newMessages;
	}

	public void setNewMessages(boolean newMessages) {
		this.newMessages = newMessages;
	}

	public boolean isShowIt() {
		return showIt;
	}

	public void setShowIt(boolean showIt) {
		this.showIt = showIt;
	}

	public Long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	public void setLastMessageTimestamp(Long lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	public Long getTimestampDrift() {
		return timestampDrift;
	}

	public void setTimestampDrift(Long timestampDrift) {
		this.timestampDrift = timestampDrift;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public boolean isAnimated() {
		return isAnimated;
	}

	public void setAnimated(boolean isAnimated) {
		this.isAnimated = isAnimated;
	}

	public String getStatusToPaint() {
		return statusToPaint;
	}

	public void setStatusToPaint(String statusToPaint) {
		this.statusToPaint = statusToPaint;
	}

	public int getNewIconSize() {
		return newIconSize;
	}

	public void setNewIconSize(int newIconSize) {
		this.newIconSize = newIconSize;
	}

	public int getTagResourceId() {
		return tagResourceId;
	}

	public void setTagResourceId(int tagResourceId) {
		this.tagResourceId = tagResourceId;
	}

	public Long getLastMessageId() {
		return lastMessageId;
	}

	public void setLastMessageId(Long lastMessageId) {
		this.lastMessageId = lastMessageId;
	}

	public String getLastMessageBody() {
		return lastMessageBody;
	}

	public void setLastMessageBody(String lastMessageBody) {
		this.lastMessageBody = lastMessageBody;
	}

}
