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
import java.io.IOException;

import org.apache.http.HttpStatus;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.HttpResp;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Environment;

public class LogService {

	private static final Logger log = LoggerFactory.getLogger(LogService.class
			.getSimpleName());

	private static final String LOG_DIR = "khc_log";

	public static boolean sendLogToServer(Context context) {
		String version = context.getString(R.string.version);
		String buildNumber = context.getString(R.string.build_number);
		log.debug("VERSION: {}, BUILD NUMBER: {}", new Object[] { version,
				buildNumber });
		log.debug("sendLogToServer...");

		File root = Environment.getExternalStorageDirectory();
		File dir = new File(root + "/" + LOG_DIR);

		if (!dir.exists()) {
			if (!dir.mkdir()) {
				log.error("Cannot create dir {}", dir);
			}
		}

		File file;
		try {
			file = new File(root + "/" + LOG_DIR, Long.toString(System
					.currentTimeMillis()));
			log.debug("LOG FILE NAME: {}", file.getAbsolutePath());
			Runtime.getRuntime().exec(
					"logcat -d -v time -f " + file.getAbsolutePath());
		} catch (IOException e) {
			log.error("Cannot read logcat", e);
			return false;
		}

		String channelId = ChannelService.getChannelId(context);
		try {
			HttpResp<Void> resp = HttpManager.sendPostVoid(context,
					context.getString(R.string.url_log, channelId), file);

			int code = resp.getCode();
			if (code == HttpStatus.SC_FORBIDDEN
					|| code == HttpStatus.SC_BAD_REQUEST) {
				throw new KurentoCommandException("Command error (" + code
						+ ")");
			}

			return code == HttpStatus.SC_CREATED;
		} catch (Exception e) {
			log.error("Cannot send log", e);
			return false;
		}
	}
}
