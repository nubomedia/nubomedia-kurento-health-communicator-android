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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.GeneralUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.KurentoCommandException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;

public class HttpManager {

	private static final Logger log = LoggerFactory.getLogger(HttpManager.class
			.getSimpleName());

	public static final String PROTOCOL_HTTPS = "https";

	public static final String COOKIE_NAME = "JSESSIONID=";
	public static final String COOKIE_HEADER = "Cookie";
	public static final String SET_COOKIE_HEADER = "Set-Cookie";

	public static final String HEADER_CONTENT_TYPE = "Content-type";
	public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";

	public static final int NUMBER_OF_CORES = Runtime.getRuntime()
			.availableProcessors();

	/* HTTP clients management */

	public static final int HTTP_CLIENT_CONNECTIONS = 1;
	public static final int HTTP_CLIENT_MULTIPART_CONNECTIONS = NUMBER_OF_CORES;

	private static HttpClient mHttpClient = null;
	private static HttpClient mHttpPostClient = null;
	private static HttpClient[] httpClientsMultipart = null;
	private static int roundRobinIndex = 0;

	private static void shutdown(final HttpClient httpClient) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					httpClient.getConnectionManager().shutdown();
				} catch (NullPointerException e) {
					log.error("This must not occur", e);
				}
				return null;
			}
		};

	}

	public static synchronized void shutdownHttpClients(Context context) {
		if (mHttpClient != null) {
			shutdown(mHttpClient);
			mHttpClient = null;
		}
		if (mHttpPostClient != null) {
			shutdown(mHttpPostClient);
			mHttpPostClient = null;
		}

		if (httpClientsMultipart != null) {
			for (HttpClient hc : httpClientsMultipart) {
				shutdown(hc);
			}
			httpClientsMultipart = null;
		}
	}

	private static HttpClient createHttpClient(Context context, int nConnections) {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme(context
				.getString(R.string.standard_protocol), PlainSocketFactory
				.getSocketFactory(), context.getResources().getInteger(
				R.integer.standard_port)));

		String protocol = Preferences.getServerProtocol(context);
		if (PROTOCOL_HTTPS.equalsIgnoreCase(protocol)) {
			try {
				SSLSocketFactory sf = KSSLSocketFactory
						.createDefaultSSLSocketFactory();
				sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				registry.register(new Scheme(protocol, sf, Preferences
						.getServerPort(context)));
			} catch (Exception e) {
				log.warn("Cannot create HTTPS support", e);
			}
		}

		HttpParams params = new BasicHttpParams();
		/* Android only allows 2 max connections */
		ConnManagerParams.setMaxTotalConnections(params, nConnections);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

		ClientConnectionManager ccm = new ThreadSafeClientConnManager(params,
				registry);

		return new DefaultHttpClient(ccm, params);
	}

	private static synchronized HttpClient getHttpClient(Context context) {
		refreshCredentials(context);

		if (mHttpClient != null) {
			return mHttpClient;
		}
		mHttpClient = createHttpClient(context, 1);

		return mHttpClient;
	}

	private static synchronized HttpClient getHttpPostClient(Context context) {
		refreshCredentials(context);

		if (mHttpPostClient != null) {
			return mHttpPostClient;
		}
		mHttpPostClient = createHttpClient(context, 1);

		return mHttpPostClient;
	}

	private static synchronized HttpClient getHttpClientMultipart(
			Context context) {
		refreshCredentials(context);

		if (httpClientsMultipart == null) {
			httpClientsMultipart = new HttpClient[HTTP_CLIENT_MULTIPART_CONNECTIONS];
		}

		if (httpClientsMultipart[roundRobinIndex] == null) {
			httpClientsMultipart[roundRobinIndex] = createHttpClient(context, 1);
		}

		HttpClient hc = httpClientsMultipart[roundRobinIndex];
		roundRobinIndex = (roundRobinIndex + 1)
				% HTTP_CLIENT_MULTIPART_CONNECTIONS;

		return hc;
	}

	/* HTTP credentials management */

	private static HttpContext httpContext = null;

	private static synchronized HttpContext getHttpContext() {
		return httpContext;
	}

	public static synchronized void setCredentials(Context ctx,
			String username, String password) {
		shutdownHttpClients(ctx);

		AuthScope scope = new AuthScope(Preferences.getServerAddress(ctx),
				Preferences.getServerPort(ctx));
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(
				username, password);

		CredentialsProvider cp = new BasicCredentialsProvider();
		cp.setCredentials(scope, creds);
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.CREDS_PROVIDER, cp);
	}

	private static synchronized void retrieveCredentials(Context ctx,
			boolean requestAccount) {
		Account ac = AccountUtils.getAccount(ctx, requestAccount);

		if (ac == null) {
			log.warn("Cannot get account");
			return;
		}

		AccountManager am = AccountManager.get(ctx);
		String password = am.getPassword(ac);
		setCredentials(ctx, ac.name, password);
	}

	private static synchronized void refreshCredentials(Context ctx) {
		if (httpContext != null) {
			return;
		}

		retrieveCredentials(ctx, false);
	}

	/* HTTP GET requests */

	public static HttpResp<Bitmap> sendGetBimap(Context ctx, String resource,
			String contentId) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		HttpResponse response = sendGet(ctx, resource);
		HttpEntity entity = response.getEntity();

		String name = contentId + ConstantKeys.AVATAR
				+ ConstantKeys.EXTENSION_JPG;

		Bitmap bitmap = FileUtils.entityToBitmap(ctx, entity, name, false,
				ConstantKeys.LONG_DEFAULT);
		if (bitmap == null) {
			String msg = "Cannot retrieve bitmap from entity";
			log.error(msg);
			throw new TransportException(msg);
		}

		HttpResp<Bitmap> ret = new HttpResp<Bitmap>(response, bitmap);
		consumeContent(response);

		return ret;
	}

	public static HttpResp<String> sendGetString(Context ctx, String resource)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		return sendGetString(ctx, resource, true);
	}

	public static HttpResp<String> sendGetString(Context ctx, String resource,
			boolean requestAccount) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		HttpResponse response = sendGet(ctx, resource, requestAccount);
		String str = getStringBody(response);

		return new HttpResp<String>(response, str);
	}

	public static HttpResponse sendGet(Context ctx, String resource)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		return sendGet(ctx, resource, true);
	}

	public static HttpResponse sendGet(Context ctx, String resource,
			boolean requestAccount) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		try {
			return sendGetRetry(ctx, resource, false);
		} catch (IllegalStateException e) {
			log.warn("Retry GET sending");
			return sendGetRetry(ctx, resource, true);
		}
	}

	private static HttpResponse sendGetRetry(Context ctx, String resource,
			boolean requestAccount) throws TransportException,
			InvalidDataException, NotFoundException, KurentoCommandException {
		HttpGet get = new HttpGet();
		HttpClient httpClient = getHttpClient(ctx);

		return sendRequest(ctx, httpClient, get, resource, requestAccount);
	}

	/* HTTP POST requests */

	public static HttpResp<Void> sendPostVoid(Context ctx, String resource,
			String body) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		StringEntity sEntity;
		try {
			sEntity = new StringEntity(body, HTTP.UTF_8);
			sEntity.setContentType(CONTENT_TYPE_APPLICATION_JSON);
		} catch (Exception e) {
			String msg = "Cannot generate string entity";
			log.error(msg, e);
			throw new TransportException(msg);
		}

		return HttpManager.sendPostVoid(ctx, resource, sEntity);
	}

	public static HttpResp<Void> sendPostVoid(Context ctx, String resource,
			File file) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		FileEntity fEntity = new FileEntity(file, CONTENT_TYPE_TEXT_PLAIN);
		return HttpManager.sendPostVoid(ctx, resource, fEntity);
	}

	public static HttpResp<Void> sendPostVoid(Context ctx, String resource,
			HttpEntity entity) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		HttpResponse response = sendPost(ctx, resource, entity);
		consumeContent(response);

		return new HttpResp<Void>(response, null);
	}

	public static HttpResp<String> sendPostString(Context ctx, String resource,
			String body) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		StringEntity sEntity;
		try {
			sEntity = new StringEntity(body, HTTP.UTF_8);
			sEntity.setContentType(CONTENT_TYPE_APPLICATION_JSON);
		} catch (Exception e) {
			String msg = "Cannot generate string entity";
			log.error(msg, e);
			throw new TransportException(msg);
		}

		HttpResponse response = sendPost(ctx, resource, sEntity);
		String str = getStringBody(response);

		return new HttpResp<String>(response, str);
	}

	private static HttpResponse sendPost(Context ctx, String resource,
			HttpEntity entity) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		try {
			return sendPostRetry(ctx, resource, entity);
		} catch (IllegalStateException e) {
			log.warn("Retry POST sending");
			return sendPostRetry(ctx, resource, entity);
		}
	}

	private static HttpResponse sendPostRetry(Context ctx, String resource,
			HttpEntity entity) throws TransportException, InvalidDataException,
			NotFoundException, KurentoCommandException {
		HttpPost post = new HttpPost();
		post.setEntity(entity);

		HttpClient httpClient;
		if (entity instanceof MultipartEntity) {
			httpClient = getHttpClientMultipart(ctx);
		} else {
			httpClient = getHttpPostClient(ctx);
		}

		return sendRequest(ctx, httpClient, post, resource);
	}

	/* HTTP generic request */

	private static HttpResponse sendRequest(Context ctx, HttpClient httpClient,
			HttpRequestBase request, String resource)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		return sendRequest(ctx, httpClient, request, resource, true);
	}

	private static HttpResponse sendRequest(Context ctx, HttpClient httpClient,
			HttpRequestBase request, String resource, boolean requestAccount)
			throws TransportException, InvalidDataException, NotFoundException,
			KurentoCommandException {
		URL url = null;
		try {
			url = GeneralUtils.buildURL(ctx, resource);
			request.setURI(url.toURI());
		} catch (Exception e) {
			String msg = "Cannot create URL";
			log.error(msg, e);
			throw new TransportException(msg);
		}

		HttpHost host = new HttpHost(url.getHost(), url.getPort(),
				url.getProtocol());

		log.trace("Send {} request to {}", request.getMethod(),
				request.getURI());

		HttpResponse response;
		try {
			response = httpClient.execute(host, request, getHttpContext());
		} catch (IllegalStateException e) {
			request.abort();
			String msg = "Connection fails sending " + request.getMethod()
					+ " to " + request.getURI();
			log.error(msg, e);
			shutdownHttpClients(ctx);
			throw e;
		} catch (Exception e) {
			request.abort();
			String msg = "Connection fails sending " + request.getMethod()
					+ " to " + request.getURI();
			log.error(msg, e);
			throw new TransportException(msg);
		}

		StatusLine status = response.getStatusLine();
		int code = status.getStatusCode();
		log.trace("Code:" + code);

		if (code == HttpStatus.SC_UNAUTHORIZED) {
			consumeContent(response);
			AccountUtils.resetAuthToken(ctx);

			String msg = "Invalid credentials";
			if (requestAccount) {
				resetAccount(ctx);
				throw new IllegalStateException(msg);
			}

			retrieveCredentials(ctx, requestAccount);
			throw new IllegalStateException(msg);
		} else if (code == HttpStatus.SC_FORBIDDEN) {
			String msg = "Forbidden command";
			log.error(msg);
			consumeContent(response);
			throw new KurentoCommandException(msg);
		} else if (code == HttpStatus.SC_BAD_REQUEST) {
			InvalidDataException exception = new InvalidDataException();

			String str;
			try {
				str = getStringBody(response);
			} catch (TransportException e) {
				log.warn("Cannot get string body", e);
				throw exception;
			}

			try {
				KhcInvalidDataInfo info = JacksonManager.fromJson(str,
						KhcInvalidDataInfo.class);
				exception.setInfo(info);
			} catch (Exception e) {
				log.warn("Cannot get invalid info from json " + str, e);
			}

			throw exception;
		} else if (code == HttpStatus.SC_NOT_FOUND) {
			NotFoundException exception = new NotFoundException();

			String str;
			try {
				str = getStringBody(response);
			} catch (TransportException e) {
				log.warn("Cannot get string body", e);
				throw exception;
			}

			try {
				KhcNotFoundInfo info = JacksonManager.fromJson(str,
						KhcNotFoundInfo.class);
				exception.setInfo(info);
			} catch (Exception e) {
				log.warn("Cannot get info from json " + str, e);
			}

			throw exception;
		}

		return response;
	}

	/* Utils */
	private static String getStringBody(HttpResponse response)
			throws TransportException {
		HttpEntity entity = response.getEntity();

		String str;
		try {
			str = EntityUtils.toString(entity);
		} catch (IOException e) {
			String msg = "Cannot retrieve string from entity";
			log.error(msg, e);
			throw new TransportException(msg);
		} finally {
			consumeContent(response);
		}

		return str;
	}

	private static void consumeContent(HttpResponse response) {
		HttpEntity respEntity = response.getEntity();

		if (respEntity == null) {
			return;
		}

		try {
			respEntity.consumeContent();
		} catch (IOException e) {
			log.error("Error consuming content", e);
		}
	}

	private static AtomicBoolean accountDeletionRequested = new AtomicBoolean(
			false);

	/* Using more than one account is not ensured to work fine */
	private static void resetAccount(final Context ctx) {
		if (!accountDeletionRequested.compareAndSet(false, true)) {
			return;
		}

		Account ac = AccountUtils.getAccount(ctx, true);

		if (ac == null) {
			log.debug("There is any account to reset");
			return;
		}

		AccountManager am = AccountManager.get(ctx);
		am.removeAccount(ac, new AccountManagerCallback<Boolean>() {
			@Override
			public void run(AccountManagerFuture<Boolean> future) {
				try {
					if (future.getResult()) {
						retrieveCredentials(ctx, true);
						accountDeletionRequested.set(false);
					} else {
						log.warn("Account can not be removed");
					}
				} catch (Exception e) {
					log.warn("Cannot get result", e);
				}
			}
		}, null);
	}

}
