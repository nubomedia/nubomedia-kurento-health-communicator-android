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

import com.kurento.khc.jsonrpc.JsonRpcError;
import com.kurento.khc.jsonrpc.JsonRpcRequest;
import com.kurento.khc.jsonrpc.JsonRpcResponse;
import com.kurento.khc.jsonrpc.JsonRpcResult;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;

public class Response extends Message {

	private final JsonRpcResponse resp = new JsonRpcResponse();

	public Response(JsonRpcRequest req, JsonRpcResult result) {
		resp.setId(req.getId());
		resp.setResult(result);
	}

	public Response(JsonRpcRequest req, JsonRpcError error) {
		resp.setId(req.getId());
		resp.setError(error);
	}

	@Override
	public String toJson() throws Exception {
		return JacksonManager.toJson(resp);
	}

}
