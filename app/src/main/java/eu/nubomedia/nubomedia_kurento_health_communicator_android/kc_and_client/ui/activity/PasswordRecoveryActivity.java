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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.AuthClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PasswordRecoveryActivity extends AnalyticsBaseActivity {

	private static final Logger log = LoggerFactory
			.getLogger(PasswordRecoveryActivity.class.getSimpleName());

	private AuthClientService authClient;
	private InputMethodManager imm;
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recover_password);

		authClient = new AuthClientService(this.getApplicationContext());

		Button getCodeButton = (Button) this.findViewById(R.id.get_code_button);
		getCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog = ProgressDialog.show(
						PasswordRecoveryActivity.this,
						ConstantKeys.STRING_DEFAULT,
						PasswordRecoveryActivity.this.getResources().getString(
								R.string.requesting_code), true, true);
				new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							return authClient
									.getRecoveryCode(((EditText) findViewById(R.id.email_txt))
											.getText().toString());
						} catch (Exception e) {
							log.error("Cannot revover code", e);
							return false;
						}
					}

					@Override
					protected void onPostExecute(Boolean result) {
						if (result) {
							imm.hideSoftInputFromWindow(
									((EditText) findViewById(R.id.email_txt))
											.getWindowToken(), 0);

							Toast.makeText(
									getApplicationContext(),
									getApplicationContext().getText(
											R.string.checkout_mail),
									Toast.LENGTH_SHORT).show();

							dialog.dismiss();
						} else {
							Toast.makeText(
									getApplicationContext(),
									getApplicationContext().getText(
											R.string.error_requesting_code),
									Toast.LENGTH_SHORT).show();
							dialog.dismiss();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});

		Button sendCodeButton = (Button) this
				.findViewById(R.id.send_code_button);
		sendCodeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				final EditText passwdRepeat = ((EditText) (PasswordRecoveryActivity.this)
						.findViewById(R.id.password2));
				final EditText passwd = ((EditText) (PasswordRecoveryActivity.this)
						.findViewById(R.id.password1));
				final EditText code = ((EditText) (PasswordRecoveryActivity.this)
						.findViewById(R.id.code_txt));

				if (code.getText().toString().isEmpty()) {
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setText(R.string.empty_code);
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setVisibility(View.VISIBLE);
				}

				if (passwd.getText().toString().isEmpty()) {
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setText(R.string.edit_user_activity_enter_password);
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setVisibility(View.VISIBLE);
					return;
				}

				if (passwdRepeat.getText().toString().isEmpty()
						&& !passwd.getText().toString().isEmpty()) {
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setText(R.string.edit_user_activity_enter_password_repeat);
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setVisibility(View.VISIBLE);
					return;
				}

				if (!passwd.getText().toString()
						.equals(passwdRepeat.getText().toString())) {
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setText(R.string.edit_user_activity_password_not_match);
					passwd.setText(ConstantKeys.STRING_DEFAULT);
					passwdRepeat.setText(ConstantKeys.STRING_DEFAULT);
					((TextView) (PasswordRecoveryActivity.this)
							.findViewById(R.id.error_message))
							.setVisibility(View.VISIBLE);
					return;
				}

				final String md5Passwd = AccountUtils.computeMD5(passwd.getText().toString());

				final String gTxt = code.getText().toString();

				dialog = ProgressDialog.show(
						PasswordRecoveryActivity.this,
						ConstantKeys.STRING_DEFAULT,
						PasswordRecoveryActivity.this.getResources().getString(
								R.string.sending_code), true, true);

				new AsyncTask<Void, Void, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							return authClient.sendRecoveryCode(md5Passwd, gTxt);
						} catch (Exception e) {
							log.error("Cannot send recovery code", e);
							return false;
						}
					}

					@Override
					protected void onPostExecute(Boolean result) {
						dialog.dismiss();
						if (result) {
							Toast.makeText(
									getApplicationContext(),
									getApplicationContext().getText(
											R.string.password_update),
									Toast.LENGTH_SHORT).show();
							finish();
						} else {
							Toast.makeText(
									getApplicationContext(),
									getApplicationContext().getText(
											R.string.password_update_fail),
									Toast.LENGTH_SHORT).show();
						}
					}

				}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}
		});

		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

	}

	@Override
	protected void onResume() {
		super.onResume();

		AppUtils.CancelNotification(getApplicationContext());
	}

}