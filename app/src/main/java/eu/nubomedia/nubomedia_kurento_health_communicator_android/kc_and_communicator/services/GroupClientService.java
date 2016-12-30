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

import org.apache.http.HttpStatus;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.graphics.Bitmap;

public class GroupClientService {

	private static final Logger log = LoggerFactory
			.getLogger(GroupClientService.class.getSimpleName());

	private final Context context;

	public GroupClientService(Context context) {
		this.context = context;
	}

	public Bitmap getAvatar(String authToken, String groupId)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		HttpResp<Bitmap> resp = HttpManager.sendGetBimap(context,
				context.getString(R.string.url_group_avatar, groupId), groupId);

		int code = resp.getCode();
		if (code != HttpStatus.SC_OK) {
			String msg = "Cannot get avatar (" + code + ")";
			log.warn(msg);
			throw new TransportException(msg);
		}

		return resp.getBody();
	}

}
