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

import com.kurento.agenda.services.pojo.GroupUpdate;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;

public class GroupObject extends GroupUpdate {

	private String groupId;
	private String name;
	private int backgroundColor = Color.TRANSPARENT;
	private int adminIconSize = 0;
	private boolean toDelete = false;
	private String statusToPain = ConstantKeys.STRING_DEFAULT;

	private String phone = "";

	public GroupObject() {
		setLocalId(ConstantKeys.LONG_DEFAULT);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(int backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public int getAdminIconSize() {
		return adminIconSize;
	}

	public void setAdminIconSize(int adminIconSize) {
		this.adminIconSize = adminIconSize;
	}

	public boolean isToDelete() {
		return toDelete;
	}

	public void setToDelete(boolean toDelete) {
		this.toDelete = toDelete;
	}

	public String getStatusToPain() {
		return statusToPain;
	}

	public void setStatusToPain(String statusToPain) {
		this.statusToPain = statusToPain;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
}
