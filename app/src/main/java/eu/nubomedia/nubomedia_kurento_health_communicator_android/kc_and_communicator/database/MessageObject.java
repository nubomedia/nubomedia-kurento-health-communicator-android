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

import android.content.Context;
import android.graphics.Color;

import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.TimelinePartyUpdate;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class MessageObject extends MessageReadResponse {

	/* @formatter:off */
	public enum Status {
		NONE,
		/* Sender */
		SENDING, SENT, ACK,
		/* Receiver */
		RECEIVED, NEW, READED;

		public static Status getValueOf(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return NONE;
			}
		}
	};

	/* @formatter:on */

	private Status status = Status.NONE;
	private String statusString = ConstantKeys.STRING_DEFAULT;
	private int statusIcon;

	private int total = 100;
	private String humanSize = ConstantKeys.STRING_DEFAULT;

	private int mediaThumbnailSize;
	private int mediaCancelButtonSize;
	private int mediaTypeIconSize;
	private int mediaTypeIconResource;
	private int messageFromSize;
	private int messageStatusSize;
	private int avatarSize;

	private int marginRight;
	private int marginLeft;

	private int alignLayout;

	private Boolean canCall;
	private String payload;

	private Long partyId = ConstantKeys.LONG_DEFAULT;
	private String partyType = ConstantKeys.STRING_DEFAULT;

	private boolean expanded = false;
	private int backgroundColor = Color.TRANSPARENT;
	private boolean isAnimated = false;

	public MessageObject() {
		setTimeline(new TimelinePartyUpdate());
		setFrom(new UserReadAvatarResponse());
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public void updateStatusValues(Context ctx) {
		if (Status.SENDING.equals(status)) {
			statusString = ctx.getString(R.string.msg_sending);
			statusIcon = R.drawable.ic_clock;
		} else if (Status.SENT.equals(status)) {
			statusString = ctx.getString(R.string.msg_sending);
			statusIcon = R.drawable.ic_clock;
		} else if (Status.ACK.equals(status)) {
			statusString = ctx.getString(R.string.msg_sent);
			statusIcon = R.drawable.ic_check;
		} else if (Status.RECEIVED.equals(status)) {
			statusString = ctx.getString(R.string.msg_received);
			statusIcon = R.drawable.ic_transparent;
		} else if (Status.NEW.equals(status)) {
			statusString = ctx.getString(R.string.msg_new);
			statusIcon = R.drawable.ic_transparent;
		} else {
			statusString = ctx.getString(R.string.msg_readed);
			statusIcon = R.drawable.ic_transparent;
		}
	}

	public void incStatus(Status status) {
		if (Status.ACK.equals(this.status)) {
			return;
		}

		if (status.compareTo(this.status) > 0) {
			this.status = status;
		}
	}

	public void incStatusAndUpdate(Status status, Context ctx) {
		incStatus(status);
		updateStatusValues(ctx);
	}

	public String getStatusString() {
		return statusString;
	}

	public int getStatusIcon() {
		return statusIcon;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Long getPartyId() {
		return this.getTimeline().getId();
	}

	public void setPartyId(Long partyId) {
		this.partyId = partyId;
	}

	public String getPartyType() {
		return this.getTimeline().getType();
	}

	public void setPartyType(String partyType) {
		this.partyType = partyType;
	}

	public String getPartyName () {
		return this.getTimeline().getName();
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
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

	public int getMediaThumbnailSize() {
		return mediaThumbnailSize;
	}

	public void setMediaThumbnailSize(int mediaThumbnailSize) {
		this.mediaThumbnailSize = mediaThumbnailSize;
	}

	public int getMediaCancelButtonSize() {
		return mediaCancelButtonSize;
	}

	public void setMediaCancelButtonSize(int mediaCancelButtonSize) {
		this.mediaCancelButtonSize = mediaCancelButtonSize;
	}

	public int getMediaTypeIconSize() {
		return mediaTypeIconSize;
	}

	public void setMediaTypeIconSize(int mediaTypeIconSize) {
		this.mediaTypeIconSize = mediaTypeIconSize;
	}

	public String getHumanSize() {
		return humanSize;
	}

	public void setHumanSize(String humanSize) {
		this.humanSize = humanSize;
	}

	public int getMediaTypeIconResource() {
		return mediaTypeIconResource;
	}

	public void setMediaTypeIconResource(int mediaTypeIconResource) {
		this.mediaTypeIconResource = mediaTypeIconResource;
	}

	public int getMarginRight() {
		return marginRight;
	}

	public void setMarginRight(int marginRight) {
		this.marginRight = marginRight;
	}

	public int getMarginLeft() {
		return marginLeft;
	}

	public void setMarginLeft(int marginLeft) {
		this.marginLeft = marginLeft;
	}

	public int getAlignLayout() {
		return alignLayout;
	}

	public void setAlignLayout(int alignLayout) {
		this.alignLayout = alignLayout;
	}

	public int getAvatarSize() {
		return avatarSize;
	}

	public void setAvatarSize(int avatarSize) {
		this.avatarSize = avatarSize;
	}

	public int getMessageFromSize() {
		return messageFromSize;
	}

	public void setMessageFromSize(int messageFromSize) {
		this.messageFromSize = messageFromSize;
	}

	public int getMessageStatusSize() {
		return messageStatusSize;
	}

	public void setMessageStatusSize(int messageStatusSize) {
		this.messageStatusSize = messageStatusSize;
	}

	public void setCanCall(Boolean canCall) {
		this.canCall = canCall;
	}

	public Boolean getCanCall() {
		return canCall;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

}
