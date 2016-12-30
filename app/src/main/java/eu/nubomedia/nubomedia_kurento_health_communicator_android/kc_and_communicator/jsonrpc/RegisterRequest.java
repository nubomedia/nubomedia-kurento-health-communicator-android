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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.jsonrpc;

import java.util.HashMap;
import java.util.Map;

import com.kurento.khc.jsonrpc.JsonRpcRequest.Method;

public class RegisterRequest extends Request {

	private static final String PARAM_CHANNEL_ID = "channelId";

	public RegisterRequest(long channelId) {
		super(Method.REGISTER);

		Map<String, Object> m = new HashMap<String, Object>();
		m.put(PARAM_CHANNEL_ID, channelId);
		req.setParams(m);
	}

}
