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
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.transport.TransportException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.LoginValues;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.kurento.agenda.services.pojo.AccountReadInfoResponse;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

	private static final Logger log = LoggerFactory
			.getLogger(AuthenticatorActivity.class.getSimpleName());

	public static final String PARAM_USERNAME = "com.kurento.kas.health.communicator.auth.USERNAME";
	public static final String PARAM_PASSWORD = "com.kurento.kas.health.communicator.auth.PASSWORD";

	public static final int QR = 100;

	private EditText usernameEdit;
	private EditText passwordEdit;
	private TextView messageTextView;
	private boolean updateCredentials;

	private Animation myAnimationDown;

	private LinearLayout dialog;
	private RelativeLayout dialogBackground;

	private InputMethodManager imm;
	private AccountReadInfoResponse accountResponse;

	private static final int REQUEST_CODE = 0123;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_activity);

		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(+ R.drawable.bg))));

		usernameEdit = (EditText) findViewById(R.id.username);
		passwordEdit = (EditText) findViewById(R.id.passwd);
		messageTextView = (TextView) findViewById(R.id.message);

		myAnimationDown = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_down);
		dialog = (LinearLayout) findViewById(R.id.includer);
		dialogBackground = (RelativeLayout) findViewById(R.id.dialog_background);

		if (Build.VERSION.SDK_INT >= 23) {
			if (AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.READ_CONTACTS)
					== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.WRITE_CONTACTS)
							== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.GET_ACCOUNTS)
							== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.CAMERA)
							== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
							== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
							== PackageManager.PERMISSION_GRANTED &&
					AuthenticatorActivity.this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
							== PackageManager.PERMISSION_GRANTED) {

				String username = getIntent().getStringExtra(PARAM_USERNAME);
				if (username != null) {
					usernameEdit.setEnabled(false);
					usernameEdit.setText(username);

					Button button = (Button) findViewById(R.id.login);
					((LayoutParams) button.getLayoutParams()).weight = 0;
				} else {
					AppUtils.purgeApp(this);
				}

				updateCredentials = username != null;

			} else {
				requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,
								Manifest.permission.WRITE_CONTACTS,
								Manifest.permission.GET_ACCOUNTS,
								Manifest.permission.CAMERA,
								Manifest.permission.RECORD_AUDIO,
								Manifest.permission.READ_EXTERNAL_STORAGE,
								Manifest.permission.WRITE_EXTERNAL_STORAGE},
						REQUEST_CODE);
			}
		} else { //permission is automatically granted on sdk<23 upon installation
			String username = getIntent().getStringExtra(PARAM_USERNAME);
			if (username != null) {
				usernameEdit.setEnabled(false);
				usernameEdit.setText(username);

				Button button = (Button) findViewById(R.id.login);
				((LayoutParams) button.getLayoutParams()).weight = 0;
			} else {
				AppUtils.purgeApp(this);
			}

			updateCredentials = username != null;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == REQUEST_CODE &&
				grantResults[0]== PackageManager.PERMISSION_GRANTED &&
				grantResults[1]== PackageManager.PERMISSION_GRANTED &&
				grantResults[2]== PackageManager.PERMISSION_GRANTED &&
				grantResults[3]== PackageManager.PERMISSION_GRANTED &&
				grantResults[4]== PackageManager.PERMISSION_GRANTED &&
				grantResults[5]== PackageManager.PERMISSION_GRANTED &&
				grantResults[6]== PackageManager.PERMISSION_GRANTED){

			String username = getIntent().getStringExtra(PARAM_USERNAME);
			if (username != null) {
				usernameEdit.setEnabled(false);
				usernameEdit.setText(username);

				Button button = (Button) findViewById(R.id.login);
				((LayoutParams) button.getLayoutParams()).weight = 0;
			} else {
				AppUtils.purgeApp(this);
			}

			updateCredentials = username != null;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		AppUtils.CancelNotification(getApplicationContext());

		if ((Preferences.getAccountId(getApplicationContext()))
				.equals(ConstantKeys.STRING_DEFAULT)) {
			createDialog();
		} else {
			checkAccount(Preferences.getAccountId(getApplicationContext()));
		}

		registerReceiver(mReceiverServerRegister, intentFilterServerRegister);
	}

	@Override
	protected void onPause() {
		super.onPause();

		TimelineListActivity.setIsAuthenticatorDisplayed(false);
		unregisterReceiver(mReceiverServerRegister);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode != RESULT_OK) {
			return;
		}

		if (requestCode == QR) {
			String contents = intent.getStringExtra("SCAN_RESULT");
			EditText edit = (EditText) dialog.findViewById(R.id.account_id);
			edit.setText(contents);
		}
	}

	public void createDialog() {
		dialog.setVisibility(View.VISIBLE);
		dialogBackground.setVisibility(View.VISIBLE);

		final AuthenticatorActivity auth = this;

		//Disable login elements
		usernameEdit.setEnabled(false);
		passwordEdit.setEnabled(false);
		((Button) AuthenticatorActivity.this
				.findViewById(R.id.register))
				.setVisibility(View.GONE);

		EditText edit = (EditText) dialog.findViewById(R.id.account_id);
		edit.requestFocus();
		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);

		edit.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				int result = actionId & EditorInfo.IME_MASK_ACTION;
				switch (result) {
				case EditorInfo.IME_ACTION_DONE:
					EditText edit = (EditText) dialog
							.findViewById(R.id.account_id);
					String text = edit.getText().toString();

					if (text.trim().length() <= 0) {
						imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(
								((EditText) AuthenticatorActivity.this
										.findViewById(R.id.account_id))
										.getWindowToken(), 0);

						Toast.makeText(auth,
								getString(R.string.account_needed),
								Toast.LENGTH_SHORT).show();
					} else {
						checkAccount(text);
					}

					return true;
				}
				return false;
			}
		});

		Button button = (Button) dialog.findViewById(R.id.account_ok);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				EditText edit = (EditText) dialog.findViewById(R.id.account_id);
				String text = edit.getText().toString();

				if (text.trim().length() <= 0) {
					imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(
							((EditText) AuthenticatorActivity.this
									.findViewById(R.id.account_id))
									.getWindowToken(), 0);

					Toast.makeText(auth, getString(R.string.account_needed),
							Toast.LENGTH_SHORT).show();
				} else {
					checkAccount(text);
				}
			}
		});

		ImageView qrButton = (ImageView) dialog.findViewById(R.id.qr_button);
		qrButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(
						"com.google.zxing.client.android.SCAN");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, QR);
			}
		});
	}

	private void showMessage(int msgResId) {
		messageTextView.setText(msgResId);
		messageTextView.setVisibility(View.VISIBLE);
		messageTextView.startAnimation(myAnimationDown);
		messageTextView.setClickable(false);
	}

	public void onClickLogin(View loginButton) {
		String username = usernameEdit.getText().toString();
		String passwd = passwordEdit.getText().toString();

		if (username.isEmpty()) {
			showMessage(R.string.login_activity_fail_bad_username);
		} else if (passwd.isEmpty()) {
			showMessage(R.string.login_activity_fail_bad_passwd);
		} else {
			messageTextView.setText(ConstantKeys.STRING_DEFAULT);
			messageTextView.setVisibility(View.GONE);
			login(username, passwd);
		}
	}

	public void onClickRegister(View loginButton) {
		if (Preferences.isUserAutoRegister(this)) {
			Intent intent = new Intent(AuthenticatorActivity.this,
					RegisterUserActivity.class);
			startActivity(intent);
		} else {
			Toast.makeText(this, getString(R.string.create_user_access_deny),
					Toast.LENGTH_SHORT).show();
		}
	}

	private void checkAccount(final String accountId) {
		new AsyncTask<Void, Void, String>() {

			private final ProgressDialog pd = new ProgressDialog(
					AuthenticatorActivity.this);

			@Override
			protected void onPreExecute() {
				pd.setTitle(R.string.login_activity_checking_account);
				pd.setMessage(getString(R.string.please_wait));
				pd.setCancelable(false);
				pd.show();
			}

			@Override
			protected String doInBackground(Void... params) {
				try {
					accountResponse = new AuthClientService(
							AuthenticatorActivity.this).checkAccount(accountId);

					return ConstantKeys.SENDING_OK;
				} catch (NotFoundException e) {

					return ConstantKeys.SENDING_FAIL;
				} catch (TransportException e) {

					return ConstantKeys.SENDING_OFFLINE;
				} catch (Exception e) {
					log.error("Cannot check account", e);

					return null;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				pd.dismiss();

				imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						((EditText) AuthenticatorActivity.this
								.findViewById(R.id.account_id))
								.getWindowToken(), 0);

				if (result == null || result.equals(ConstantKeys.SENDING_FAIL)) {
					((Button) AuthenticatorActivity.this
							.findViewById(R.id.register))
							.setVisibility(View.GONE);

					Toast.makeText(AuthenticatorActivity.this,
							getString(R.string.wrong_account),
							Toast.LENGTH_SHORT).show();

					createDialog();
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					((Button) AuthenticatorActivity.this
							.findViewById(R.id.register))
							.setVisibility(View.GONE);

					Toast.makeText(AuthenticatorActivity.this,
							getString(R.string.no_connection),
							Toast.LENGTH_SHORT).show();

					createDialog();
				} else if (result.equals(ConstantKeys.SENDING_OK)) {
					usernameEdit.setEnabled(true);
					passwordEdit.setEnabled(true);
					((Button) AuthenticatorActivity.this
							.findViewById(R.id.register))
							.setVisibility(View.VISIBLE);
					dialog.setVisibility(View.GONE);
					dialogBackground.setVisibility(View.GONE);

					Preferences.setAccountId(getApplicationContext(),
							accountResponse.getId().toString());
					Preferences.setAccountName(getApplicationContext(),
							accountResponse.getName());
					Preferences.setUserAutoRegister(getApplicationContext(),
							accountResponse.getUserAutoregister());
					Preferences.setGroupAutoRegister(getApplicationContext(),
							accountResponse.getGroupAutoregister());

					Account account = AccountUtils.getAccount(
							getApplicationContext(), false);
					if (account != null) {
						AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
						String channelId = am.getUserData(account,
								JsonKeys.CHANNEL_ID);
						if (channelId != null && !channelId.isEmpty()) {
							AuthenticatorActivity.this.finish();
						}
					}
				}
			}
		}.execute();
	}

	private void login(final String username, final String passwd) {
		new AsyncTask<Void, Void, LoginValues>() {

			private final ProgressDialog pd = new ProgressDialog(
					AuthenticatorActivity.this);
			private final String md5Passwd = AccountUtils.computeMD5(passwd);

			@Override
			protected void onPreExecute() {
				messageTextView.setVisibility(View.GONE);

				pd.setTitle(R.string.login_activity_logging_in);
				pd.setMessage(getString(R.string.please_wait_login));
				pd.setCancelable(false);
				pd.show();
			}

			@Override
			protected LoginValues doInBackground(Void... params) {
				return new AuthClientService(AuthenticatorActivity.this).login(
						username, md5Passwd);
			}

			@Override
			protected void onPostExecute(LoginValues result) {
				pd.dismiss();
				finishLogin(result, username, md5Passwd);
			}
		}.execute();
	}

	private void finishLogin(LoginValues loginValues, String username,
			String md5Passwd) {
		String authToken = null;
		if (loginValues != null) {
			authToken = loginValues.getToken();
		}

		if (authToken == null || authToken.isEmpty()) {
			messageTextView.setText(R.string.login_activity_login_fail);
			messageTextView.setVisibility(View.VISIBLE);
			messageTextView.startAnimation(myAnimationDown);
			messageTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent(AuthenticatorActivity.this,
							PasswordRecoveryActivity.class);
					startActivity(intent);
				}
			});

			return;
		}

		messageTextView.setVisibility(View.GONE);

		Account account = new Account(username,
				getString(R.string.account_type));

		AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		if (updateCredentials) {
			am.setPassword(account, md5Passwd);
		} else {
			am.addAccountExplicitly(account, md5Passwd, null);
			ContentResolver.setSyncAutomatically(account,
					ContactsContract.AUTHORITY, true);
		}

		AccountUtils.storeUserData(account, am, loginValues, authToken);
		AccountUtils.getAccount(this, true);

		Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
				getString(R.string.account_type));
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);

		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.login_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		startActivity(new Intent(this, Preferences.class));

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		if (dialogBackground.getVisibility() == View.VISIBLE) {
			// do nothing
		} else {
			super.onBackPressed();
		}
	}

	/* Broadcast receivers */
	private final IntentFilter intentFilterServerRegister = new IntentFilter(
			ConstantKeys.BROADCAST_SERVER_REGISTER);
	private final BroadcastReceiver mReceiverServerRegister = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			AuthenticatorActivity.this.finish();
		}
	};

}
