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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.AuthClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.InvalidDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo;
import com.kurento.agenda.services.pojo.UserCreate;
import com.kurento.agenda.services.pojo.UserReadResponse;

public class RegisterUserActivity extends AnalyticsBaseActivity {

	private static final Logger log = LoggerFactory
			.getLogger(RegisterUserActivity.class.getSimpleName());

	private static final int CROP_CAMERA_IMAGE = 100;
	private static final int PICK_FROM_CAMERA = 101;

	private UserReadResponse userRead;
	private ProgressDialog pd;
	private Uri mFileCaptureUri;
	private String tempFilePath;
	private Bitmap bmp = null;
	public String userName = ConstantKeys.STRING_DEFAULT;
	private ProgressBar progressBar;

	private Animation myAnimationDown;
	private Animation myAnimationUp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_user);

		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(+ R.drawable.bg))));

		// change values from edit user to create one
		findViewById(R.id.password_layer).setVisibility(View.VISIBLE);
		findViewById(R.id.password_button).setVisibility(View.GONE);
		((TextView) findViewById(R.id.password_1))
				.setText(getString(R.string.login_activity_password_hint));
		((EditText) findViewById(R.id.password_1_txt))
				.setHint(getString(R.string.login_activity_password_hint));
		((EditText) findViewById(R.id.email_txt)).setFocusableInTouchMode(true);
		((EditText) findViewById(R.id.password_2_txt))
		.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				int result = actionId & EditorInfo.IME_MASK_ACTION;
				switch (result) {
				case EditorInfo.IME_ACTION_DONE:
					sendData();

					return true;
				}

				return false;
			}
		});

		ImageView im = (ImageView) findViewById(R.id.contact_image);
		im.setOnClickListener(new OnClickListener() {

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
					RegisterUserActivity.this.startActivityForResult(intent,
							PICK_FROM_CAMERA);

				} catch (ActivityNotFoundException e) {
					log.debug("There is no camera on device", e);
				}
			}

		});

		progressBar = (ProgressBar)findViewById(R.id.progressBar);
		progressBar.setVisibility(View.GONE);

		myAnimationDown = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_down);
		myAnimationUp = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_up);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_user_menu_editing, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.save_edition) {
			sendData();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showMainError(String error) {
		((TextView) findViewById(R.id.error_message)).setText(error);
		((TextView) findViewById(R.id.error_message))
				.setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.error_message))
				.startAnimation(myAnimationDown);
	}

	private void resetRedTextViews() {
		((EditText) findViewById(R.id.email_txt)).setTextColor(Color.DKGRAY);
		((EditText) findViewById(R.id.phone_txt)).setTextColor(Color.DKGRAY);
	}

	private void sendData() {
		((TextView) findViewById(R.id.error_message))
				.startAnimation(myAnimationUp);
		resetRedTextViews();

		EditText passwdRepeat = (EditText) findViewById(R.id.password_2_txt);
		EditText passwd = (EditText) findViewById(R.id.password_1_txt);
		EditText name = (EditText) findViewById(R.id.name_txt);
		EditText surname = (EditText) findViewById(R.id.surname_txt);
		EditText email = (EditText) findViewById(R.id.email_txt);
		EditText telephone = ((EditText) findViewById(R.id.phone_txt));

		if ((name.getText().toString().isEmpty())
				|| (email.getText().toString().isEmpty())
				|| (telephone.getText().toString().isEmpty())) {
			showMainError(getString(R.string.edit_user_activity_enter_all));
			return;
		}

		if (passwd.getText().toString().isEmpty()) {
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setText(R.string.edit_user_activity_enter_password);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setVisibility(View.VISIBLE);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.startAnimation(myAnimationDown);
			return;
		}

		if (passwdRepeat.getText().toString().isEmpty()
				&& !passwd.getText().toString().isEmpty()) {
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setText(R.string.edit_user_activity_enter_password_repeat);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setVisibility(View.VISIBLE);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.startAnimation(myAnimationDown);
			return;
		}

		if (!passwd.getText().toString()
				.equals(passwdRepeat.getText().toString())) {
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setText(R.string.edit_user_activity_password_not_match);
			passwd.setText(ConstantKeys.STRING_DEFAULT);
			passwdRepeat.setText(ConstantKeys.STRING_DEFAULT);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.setVisibility(View.VISIBLE);
			((TextView) (RegisterUserActivity.this)
					.findViewById(R.id.error_message))
					.startAnimation(myAnimationDown);
			return;
		}

		String md5Passwd = AccountUtils.computeMD5(passwd.getText().toString());

		final UserCreate user = new UserCreate();
		user.setEmail(email.getText().toString());
		user.setName(name.getText().toString());
		user.setPassword(md5Passwd);
		user.setSurname(surname.getText().toString());
		user.setPhone(telephone.getText().toString());

		pd = new ProgressDialog(this);
		pd.setTitle(R.string.edit_user_activity_sending_user_data);
		pd.setMessage(getString(R.string.please_wait));
		pd.setCancelable(false);
		pd.show();

		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				String path = null;
				try {

					if (mFileCaptureUri != null) {
						path = FileUtils.getRealPathFromURI(mFileCaptureUri,
								RegisterUserActivity.this);

						return new AuthClientService(RegisterUserActivity.this)
								.createUser(Preferences
										.getAccountId(getApplicationContext()),
										user, path);
					} else {
						return new AuthClientService(RegisterUserActivity.this)
								.createUser(Preferences
										.getAccountId(getApplicationContext()),
										user);
					}
				} catch (InvalidDataException invalidE) {
					createUserFail(invalidE);
					return false;
				} catch (Exception e) {
					log.error("Cannot create user", e);
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (pd != null)
					pd.dismiss();
				if (result) {
					// We store the image like the new one
					try {
						FileOutputStream out = new FileOutputStream(
								FileUtils.getDir() + "/" + userRead.getId());

						bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.flush();
						out.close();
						bmp = null;

					} catch (Exception e) {
						log.debug("No image uploaded");
					}

					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.account_register_success),
							Toast.LENGTH_SHORT).show();

					finish();
				} else {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.create_user_fail),
							Toast.LENGTH_SHORT).show();
				}

				FileUtils.deleteTemp(tempFilePath);
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	final Handler handlerCloseDialog = new Handler();

	final Runnable runCloseDialog = new Runnable() {
		public void run() {
			if (pd != null)
				pd.dismiss();
		}
	};

	final Handler handlerAuthError = new Handler();

	final Runnable runAuthError = new Runnable() {
		public void run() {
			if (pd != null) {
				pd.dismiss();
			}
			Toast.makeText(getApplicationContext(),
					getApplicationContext().getText(R.string.auth_fail),
					Toast.LENGTH_SHORT).show();
		}
	};

	private void doCrop() {
		Intent intent = new Intent(ConstantKeys.CROP_INTENT);
		intent.setType(ConstantKeys.IMAGE_CROP);

		List<ResolveInfo> list = getPackageManager().queryIntentActivities(
				intent, 0);

		int size = list.size();

		if (size == 0) {
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

		Intent i = new Intent(intent);
		ResolveInfo res = list.get(0);

		i.setComponent(new ComponentName(res.activityInfo.packageName,
				res.activityInfo.name));

		startActivityForResult(i, CROP_CAMERA_IMAGE);
	}

	private void createUserFail(final InvalidDataException invalidE) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String error = getString(R.string.error_unexpected);
				if (KhcInvalidDataInfo.Code.EMAIL_ALREADY_USED.equals(invalidE
						.getInfo().getCode())) {
					error = getString(R.string.error_email_registered);
					((EditText) findViewById(R.id.email_txt))
							.setTextColor(Color.RED);
				} else if (KhcInvalidDataInfo.Code.PHONE_ALREADY_USED
						.equals(invalidE.getInfo().getCode())) {
					error = getString(R.string.error_phone_registered);
					((EditText) findViewById(R.id.phone_txt))
							.setTextColor(Color.RED);
				} else if (KhcInvalidDataInfo.Code.PHONE_FORMAT.equals(invalidE
						.getInfo().getCode())) {
					error = getString(R.string.error_invalid_phone);
					((EditText) findViewById(R.id.phone_txt))
							.setTextColor(Color.RED);
				}

				showMainError(error);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			mFileCaptureUri = null;
			return;
		}

		if (requestCode == PICK_FROM_CAMERA) {
			doCrop();
		}

		if (requestCode == CROP_CAMERA_IMAGE) {
			bmp = FileUtils.decodeSampledBitmapFromPath(FileUtils
					.getRealPathFromURI(mFileCaptureUri,
							RegisterUserActivity.this), 150, 150);
			((ImageView) RegisterUserActivity.this
					.findViewById(R.id.contact_image)).setImageBitmap(bmp);
		}
	}

}