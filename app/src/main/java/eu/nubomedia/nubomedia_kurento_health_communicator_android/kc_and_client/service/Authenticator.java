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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.service;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.AuthenticatorActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.AuthClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.LoginValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

public class Authenticator extends AbstractAccountAuthenticator {

	private static final Logger log = LoggerFactory
			.getLogger(Authenticator.class.getSimpleName());

	// Authentication Service context
	protected final Context context;

	public Authenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,
			String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options) {
		final Intent intent = new Intent(context, AuthenticatorActivity.class);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
				response);
		final Bundle bundle = new Bundle();
		bundle.putParcelable(AccountManager.KEY_INTENT, intent);
		return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) {
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,
			String accountType) {
		throw new UnsupportedOperationException();
	}

	public static String getLocalAuthToken(Context ctx, Account account) {
		AccountManager am = AccountManager.get(ctx);

		return am.getUserData(account, JsonKeys.AUTH_TOKEN);
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle loginOptions)
			throws NetworkErrorException {
		if (!authTokenType.equals(context.getString(R.string.account_type))) {
			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE,
					"invalid authTokenType");

			return result;
		}

		AccountManager am = AccountManager.get(context);
		String password = am.getPassword(account);

		String authToken = getLocalAuthToken(context, account);
		if (!authToken.isEmpty()) {
			final Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE,
					context.getString(R.string.account_type));
			result.putString(AccountManager.KEY_AUTHTOKEN,
					am.getUserData(account, JsonKeys.AUTH_TOKEN));

			return result;
		}

		if (password != null) {
			LoginValues loginValues = new AuthClientService(context).login(
					account.name, password);
			if (loginValues != null) {
				authToken = loginValues.getToken();
				if (!TextUtils.isEmpty(authToken)) {
					final Bundle result = new Bundle();
					result.putString(AccountManager.KEY_ACCOUNT_NAME,
							account.name);
					result.putString(AccountManager.KEY_ACCOUNT_TYPE,
							context.getString(R.string.account_type));
					result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

					// Adding user data to the selected account
					AccountUtils.storeUserData(account, am, loginValues,
							authToken);

					return result;
				}
			}
		}

		Bundle b = new Bundle();
		Intent i = new Intent(context, AuthenticatorActivity.class);
		i.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
		i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		b.putParcelable(AccountManager.KEY_INTENT, i);

		return b;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,
			Account account, String[] features) {
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle loginOptions) {
		return null;
	}
}
