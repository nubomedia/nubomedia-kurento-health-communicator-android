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

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.util.MyLifecycleHandler;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandRunService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.UserEdit;

public class EditUserActivity extends AnalyticsBaseActivity {

	private static final Logger log = LoggerFactory
			.getLogger(EditUserActivity.class.getSimpleName());

	private static final int CROP_CAMERA_IMAGE = 100;
	private static final int PICK_FROM_CAMERA = 101;
	private static final int LOG_OUT = 1;

	private Uri mFileCaptureUri;
	private String tempFilePath;
	private Bitmap bmp = null;

	private boolean goBack = true;

	private Animation myAnimationDown;
	private Animation myAnimationUp;

	private ImageView contactImageView;
	private ProgressBar progressBar;

	private long userId;

	private Menu mMenu = null;
	private boolean isEditing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_user);

		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(R.drawable.bg))));

		Button showPassword = (Button) this.findViewById(R.id.password_button);
		showPassword.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (findViewById(R.id.password_layer).getVisibility() == View.VISIBLE) {
					findViewById(R.id.password_layer).setVisibility(View.GONE);
				} else {
					findViewById(R.id.password_layer).setVisibility(
							View.VISIBLE);
				}
			}
		});

		contactImageView = (ImageView) EditUserActivity.this
				.findViewById(R.id.contact_image);
		contactImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(),
						ConstantKeys.TEMP
								+ String.valueOf(System.currentTimeMillis())
								+ ConstantKeys.EXTENSION_JPG));

				tempFilePath = mFileCaptureUri.getPath();

				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

				intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileCaptureUri);

				try {
					intent.putExtra(ConstantKeys.RETURNDATA, true);
					EditUserActivity.this.startActivityForResult(intent,
							PICK_FROM_CAMERA);
				} catch (ActivityNotFoundException e) {
					log.warn("Cannot capture picture. There is not any app to do it.");
				}
			}
		});

		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		progressBar.setVisibility(View.VISIBLE);

		Account account = AccountUtils.getAccount(this, true);
		setTitle(account.name);

		getActionBar().setIcon(R.drawable.ic_profile_light);
		setTitle(getString(R.string.account_preferences_edit_account_summary));
		getData();
		setNoEditing();

		myAnimationDown = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_down);
		myAnimationUp = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_up);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_user_menu, menu);
		mMenu = menu;
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (AppUtils.getDefaults(ConstantKeys.FROMLOGIN, this)
				.equalsIgnoreCase("true")) {
			finish();
		}

		AppUtils.CancelNotification(getApplicationContext());

		Account ac = AccountUtils.getAccount(this, true);
		if (ac != null) {
			AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
			String userName = am.getUserData(ac, JsonKeys.NAME) + " "
					+ am.getUserData(ac, JsonKeys.SURNAME);
			getActionBar().setSubtitle(userName);
		}

		registerReceiver(mGCMReceiver, gcmFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mGCMReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuInflater inflater = getMenuInflater();

		int itemId = item.getItemId();
		if (itemId == R.id.allow_editing) {
			mMenu.clear();
			inflater.inflate(R.menu.edit_user_menu_editing, mMenu);

			setAllowedEditing();

			return true;
		} else if (itemId == R.id.log_out) {
			startLoggingOut();

			return true;
		} else if (itemId == R.id.save_edition) {
			sendData();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			MenuInflater inflater = getMenuInflater();
			if (isEditing) {
				mMenu.clear();
				inflater.inflate(R.menu.edit_user_menu, mMenu);

				getData();
				setNoEditing();
				getWindow()
						.setSoftInputMode(
								WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

				return false;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void setAllowedEditing() {
		isEditing = true;

		Account account = AccountUtils.getAccount(this, true);
		AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);

		EditText name_txt = (EditText) findViewById(R.id.name_txt);
		name_txt.setText(am.getUserData(account, JsonKeys.NAME));
		name_txt.setFocusableInTouchMode(true);

		EditText surname_txt = (EditText) findViewById(R.id.surname_txt);
		surname_txt.setText(am.getUserData(account, JsonKeys.SURNAME));
		surname_txt.setFocusableInTouchMode(true);

		View phone_txt = findViewById(R.id.phone_txt);
		phone_txt.setClickable(true);
		phone_txt.setFocusableInTouchMode(true);

		View password_button = findViewById(R.id.password_button);
		password_button.setVisibility(View.VISIBLE);

		TextView email_layer = (TextView) findViewById(R.id.email);
		email_layer.setText(R.string.edit_user_activity_email_no_editable);

		View contact_image = findViewById(R.id.contact_image);
		contact_image.setClickable(true);
	}

	private void setNoEditing() {
		isEditing = false;

		EditText name_txt = (EditText) findViewById(R.id.name_txt);
		name_txt.setFocusable(false);

		View surname_txt = findViewById(R.id.surname_txt);
		surname_txt.setFocusable(false);

		View phone_txt = findViewById(R.id.phone_txt);
		phone_txt.setFocusable(false);

		View password_button = findViewById(R.id.password_button);
		password_button.setVisibility(View.GONE);

		TextView email_layer = (TextView) findViewById(R.id.email);
		email_layer.setText(R.string.edit_user_activity_email);

		View contact_image = findViewById(R.id.contact_image);
		contact_image.setClickable(false);

		findViewById(R.id.password_layer).setVisibility(View.GONE);
		EditText passwdRepeat = ((EditText) (EditUserActivity.this)
				.findViewById(R.id.password_2_txt));
		passwdRepeat.setText(ConstantKeys.STRING_DEFAULT);
		EditText passwd = ((EditText) (EditUserActivity.this)
				.findViewById(R.id.password_1_txt));
		passwd.setText(ConstantKeys.STRING_DEFAULT);
	}

	private void startLoggingOut() {
		final Account account = AccountUtils.getAccount(this, true);
		final Intent loggingOut = new Intent(this, AuthenticatorActivity.class);
		final Context context = this;

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.log_out_dialog_title);
		alertDialogBuilder.setIcon(R.drawable.ic_leave);
		alertDialogBuilder
				.setMessage(R.string.log_out_dialog_message)
				.setCancelable(false)
				.setPositiveButton(R.string.log_out_dialog_button_ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
								am.removeAccount(account, null, null);

								AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.TRUE,
										context);

								//Disable WebSocket and QoS.
								MyLifecycleHandler.close();

								startActivityForResult(loggingOut, LOG_OUT);

								Toast.makeText(context, R.string.log_out_toast,
										Toast.LENGTH_SHORT).show();
							}
						})
				.setNegativeButton(R.string.log_out_dialog_button_cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void sendData() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		EditText edit = (EditText) (EditUserActivity.this)
				.findViewById(R.id.name_txt);
		imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);

		edit = (EditText) (EditUserActivity.this)
				.findViewById(R.id.surname_txt);
		imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);

		edit = (EditText) (EditUserActivity.this).findViewById(R.id.phone_txt);
		imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);

		edit = (EditText) (EditUserActivity.this)
				.findViewById(R.id.password_1_txt);
		imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);

		edit = (EditText) (EditUserActivity.this)
				.findViewById(R.id.password_2_txt);
		imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);

		((TextView) (EditUserActivity.this).findViewById(R.id.error_message))
				.startAnimation(myAnimationUp);

		EditText passwdRepeat = ((EditText) (EditUserActivity.this)
				.findViewById(R.id.password_2_txt));
		EditText passwd = ((EditText) (EditUserActivity.this)
				.findViewById(R.id.password_1_txt));

		String passwdValue = passwd.getText().toString();
		String passwdRepeatValues = passwdRepeat.getText().toString();

		if (!passwdValue.equals(passwdRepeatValues)) {
			((TextView) (EditUserActivity.this)
					.findViewById(R.id.error_message))
					.setText(R.string.edit_user_activity_password_not_match);
			passwd.setText(ConstantKeys.STRING_DEFAULT);
			passwdRepeat.setText(ConstantKeys.STRING_DEFAULT);
			((TextView) (EditUserActivity.this)
					.findViewById(R.id.error_message))
					.setVisibility(View.VISIBLE);
			((TextView) (EditUserActivity.this)
					.findViewById(R.id.error_message))
					.startAnimation(myAnimationDown);
			return;
		}

		final UserEdit userEdit = new UserEdit();
		userEdit.setName(((EditText) (EditUserActivity.this)
				.findViewById(R.id.name_txt)).getText().toString());
		userEdit.setSurname(((EditText) (EditUserActivity.this)
				.findViewById(R.id.surname_txt)).getText().toString());
		userEdit.setPhone(((EditText) (EditUserActivity.this)
				.findViewById(R.id.phone_txt)).getText().toString());

		if (!passwdValue.isEmpty()) {
			String md5Passwd = AccountUtils.computeMD5(passwdValue);
			userEdit.setPassword(md5Passwd);

			Account account = AccountUtils.getAccount(this,
					false);
			AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
			am.clearPassword(account);
			am.setPassword(account, md5Passwd);
		}

		// Username and id can't be changed
		userEdit.setId(userId);

		mMenu.clear();

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_user_menu, mMenu);

		setNoEditing();
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		new AsyncTask<Void, Void, String>() {

			private final ProgressDialog pd = new ProgressDialog(
					EditUserActivity.this);

			@Override
			protected void onPreExecute() {
				goBack = false;

				pd.setTitle(R.string.edit_user_activity_sending_user_data);
				pd.setMessage(getString(R.string.please_wait));
				pd.setCancelable(false);
				pd.show();
			};

			@Override
			protected String doInBackground(Void... params) {
				String path = null;
				if (mFileCaptureUri != null) {
					path = FileUtils.getRealPathFromURI(mFileCaptureUri,
							EditUserActivity.this);
				}

				CommandStoreService cs = new CommandStoreService(EditUserActivity.this);
				try {
					if (cs.createCommand(JsonParser.userEditToJson(userEdit),
							Command.METHOD_UPDATE_USER, path)) {
						return ConstantKeys.SENDING_OK;
					} else {
						return ConstantKeys.SENDING_OFFLINE;
					}
				} catch (JSONException e) {
					log.error("Error editing user", e);
					return ConstantKeys.SENDING_FAIL;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				goBack = true;
				pd.dismiss();

				if (result.equals(ConstantKeys.SENDING_OK)) {
					// We store the image like the new one
					try {
						if (bmp != null) {
							FileOutputStream out = new FileOutputStream(
									FileUtils.getDir() + "/" + userId);

							bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
							out.flush();
							out.close();
						}
					} catch (Exception e) {
						log.error("Cannot get contact picture", e);
					}

					try {
						CommandRunService.runUpdateUser(EditUserActivity.this,
								JsonParser.userEditToJson(userEdit));
					} catch (JSONException e) {
						log.error("Error updating user", e);
					}

					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(R.string.upload_ok),
							Toast.LENGTH_SHORT).show();
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.account_update_fail),
							Toast.LENGTH_SHORT).show();
				}

				FileUtils.deleteTemp(tempFilePath);
			}
		}.execute();
	}

	private void getData() {
		Account account = AccountUtils.getAccount(this, true);
		if (account == null) {
			log.warn("There is not any account. User data can not be got.");
			return;
		}

		AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
		userId = Long.valueOf(am.getUserData(account, JsonKeys.ID_STORED));
		String userName = am.getUserData(account, JsonKeys.NAME) + " "
				+ am.getUserData(account, JsonKeys.SURNAME);
		getActionBar().setSubtitle(userName);

		((EditText) (EditUserActivity.this).findViewById(R.id.name_txt))
				.setText(am.getUserData(account, JsonKeys.NAME));
		((EditText) (EditUserActivity.this).findViewById(R.id.surname_txt))
				.setText(am.getUserData(account, JsonKeys.SURNAME));
		((EditText) (EditUserActivity.this).findViewById(R.id.phone_txt))
				.setText(am.getUserData(account, JsonKeys.PHONE));
		((EditText) (EditUserActivity.this).findViewById(R.id.email_txt))
				.setText(am.getUserData(account, JsonKeys.EMAIL));

		mFileCaptureUri = null;
		bmp = null;
		tempFilePath = null;
		String avatarId = am.getUserData(account, JsonKeys.PICTURE);
		if (avatarId == null) {
			avatarId = ConstantKeys.STRING_CERO;
		}

		if (avatarId.equals(ConstantKeys.STRING_CERO)) {
			progressBar.setVisibility(View.GONE);
		} else {
			progressBar.setVisibility(View.VISIBLE);
		}

		ImageDownloader downloader = new ImageDownloader();
		downloader.downloadMyAvatar(getApplicationContext(), contactImageView,
				am.getUserData(account, JsonKeys.ID_STORED), avatarId);
	}

	private void doCrop() {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setType("image/*");

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);

		if (list.isEmpty()) {
			Toast.makeText(this, "Can not find image crop app",
					Toast.LENGTH_SHORT).show();

			return;
		}

		intent.setData(mFileCaptureUri);

		intent.putExtra("outputX", 100);
		intent.putExtra("outputY", 100);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("scale", true);
		intent.putExtra("noFaceDetection", true);
		mFileCaptureUri = Uri.fromFile(new File(Environment
				.getExternalStorageDirectory(), ConstantKeys.CROP
				+ String.valueOf(System.currentTimeMillis())
				+ ConstantKeys.EXTENSION_JPG));
		intent.putExtra("output", mFileCaptureUri);

		ResolveInfo res = list.get(0);
		Intent i = new Intent(intent);
		i.setComponent(new ComponentName(res.activityInfo.packageName,
				res.activityInfo.name));

		startActivityForResult(i, CROP_CAMERA_IMAGE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			mFileCaptureUri = null;
			return;
		}

		if (requestCode == PICK_FROM_CAMERA) {
			doCrop();
		} else if (requestCode == CROP_CAMERA_IMAGE) {
			bmp = FileUtils.decodeSampledBitmapFromPath(
					FileUtils.getRealPathFromURI(mFileCaptureUri,
							EditUserActivity.this), 150, 150);
			((ImageView) EditUserActivity.this.findViewById(R.id.contact_image))
					.setImageBitmap(bmp);
		}
	}

	@Override
	public void onBackPressed() {
		if (goBack) {
			super.onBackPressed();
		} else {
			// do nothing.
		}
	}

	/* Broadcast receivers */
	private IntentFilter gcmFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM);
	private BroadcastReceiver mGCMReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
				getData();

				if (editUserListener != null) {
					editUserListener.userEdited();
				}
				editUserListener = null;
			}
		}
	};

	/* Test utilities */

	public Long getUserId() {
		return this.userId;
	}

	private EditUserListener editUserListener;

	public interface EditUserListener {
		void userEdited();
	}

	public void setEditUserListener(EditUserListener l) {
		this.editUserListener = l;
	}
}
