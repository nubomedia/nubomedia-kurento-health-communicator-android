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

import java.io.Serializable;

import android.graphics.Color;

import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class GroupMemberObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private GroupInfo group;
	private UserReadAvatarResponse user;
	private Boolean admin = false;

	private boolean toDeleted = false;
	private boolean toAdd = false;

	private int backgroundColor = Color.TRANSPARENT;
	private boolean isAnimated = false;
	private String adminLayer = ConstantKeys.STRING_DEFAULT;

	public GroupMemberObject() {
		group = new GroupInfo();
		user = new UserReadAvatarResponse();
	}

	public void setGroup(GroupInfo group) {
		this.group = group;
	}

	public GroupInfo getGroup() {
		return group;
	}

	public void setUser(UserReadAvatarResponse user) {
		this.user = user;
	}

	public UserReadAvatarResponse getUser() {
		return user;
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

	public Boolean isAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}

	public String getAdminLayer() {
		return adminLayer;
	}

	public void setAdminLayer(String adminLayer) {
		this.adminLayer = adminLayer;
	}

	public boolean isToDeleted() {
		return toDeleted;
	}

	public void setToDeleted(boolean toDeleted) {
		this.toDeleted = toDeleted;
	}

	public boolean isToAdd() {
		return toAdd;
	}

	public void setToAdd(boolean toAdd) {
		this.toAdd = toAdd;
	}

}
