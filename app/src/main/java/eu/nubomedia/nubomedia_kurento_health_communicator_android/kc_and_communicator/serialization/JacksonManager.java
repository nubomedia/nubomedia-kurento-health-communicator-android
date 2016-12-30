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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;

public class JacksonManager {
	private static ObjectMapper mapper;

	private static void createMapper() {
		if (mapper != null) {
			return;
		}

		synchronized (JacksonManager.class) {
			if (mapper != null) {
				return;
			}

			mapper = new ObjectMapper();
			mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, false);
			mapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS,
					false);
			mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES,
					true);
			mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
			mapper.configure(SerializationConfig.Feature.WRITE_NULL_MAP_VALUES,
					true);
			mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
		}
	}

	public static ObjectMapper getMapper() {
		createMapper();

		return mapper;
	}

	public static String toJson(Object obj) throws Exception {
		return getMapper().writeValueAsString(obj);
	}

	public static <T> T fromJson(String json, Class<T> valueType)
			throws Exception {
		return getMapper().readValue(json, valueType);
	}

	public static <T> T fromJson(String json, TypeReference<T> valueTypeRef)
			throws Exception {
		return getMapper().readValue(json, valueTypeRef);
	}

	public static <T> T parseJsonObject(Object json, Class<T> clazz)
			throws Exception {
		ObjectNode params = getMapper().convertValue(json, ObjectNode.class);
		return getMapper().readValue(params, clazz);
	}

	public static String buildJson(Object obj) throws Exception {
		return getMapper().writeValueAsString(
				getMapper().convertValue(obj, ObjectNode.class));
	}

}
