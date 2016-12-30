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

import java.util.concurrent.atomic.AtomicLong;

import com.kurento.khc.jsonrpc.JsonRpcRequest;
import com.kurento.khc.jsonrpc.JsonRpcRequest.Method;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;

public abstract class Request extends Message {

	protected final JsonRpcRequest req = new JsonRpcRequest();
	private static final AtomicLong inc = new AtomicLong(1);

	protected Request(Method method) {
		req.setId(inc.getAndIncrement());
		req.setMethod(method);
	}

	@Override
	public String toJson() throws Exception {
		return JacksonManager.toJson(req);
	}

}
