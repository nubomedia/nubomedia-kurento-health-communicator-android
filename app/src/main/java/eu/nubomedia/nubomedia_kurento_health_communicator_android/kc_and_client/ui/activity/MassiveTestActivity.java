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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.ActionItem;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.QuickAction;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.SendMessageAsyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class MassiveTestActivity extends AnalyticsBaseActivity {

	private static final Logger log = LoggerFactory
			.getLogger(MassiveTestActivity.class.getSimpleName());

	private TimelineObject timeline = null;

	private static final int ID_CAMERA = 1;
	private static final int ID_VIDIO = 2;
	private static final int ID_GALLERY = 3;

	private QuickAction mQuickAction;
	private Uri mFileCaptureUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.massive_test_screen);

		timeline = (TimelineObject) getIntent().getSerializableExtra(
				ConstantKeys.TIMELINE);

		setTitle(getString(R.string.test_activity_header));
		getActionBar().setIcon(R.drawable.ic_timeline);
		getActionBar().setBackgroundDrawable(new ColorDrawable(0xffff8800));

		configurePopup();

		((Button) findViewById(R.id.plain_messages_button))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						int nMsgs = getIntValueFromEditText(R.id.plain_messages_text);

						if (nMsgs == 0) {
							return;
						}

						sendMasiveMessages(nMsgs, null);
					}
				});

		((Button) findViewById(R.id.media_messages_button))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						int nMsgs = getIntValueFromEditText(R.id.media_messages_text);

						if ((mFileCaptureUri == null) && nMsgs == 0) {
							return;
						}

						sendMasiveMessages(nMsgs, mFileCaptureUri);
					}
				});

		((Button) findViewById(R.id.log))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						new AsyncTask<Void, Void, Void>() {
							@Override
							protected Void doInBackground(Void... params) {
								//LogService
								//		.sendLogToServer(getApplicationContext());
								return null;
							}
						}.execute();
					}
				});
	}

	private int getIntValueFromEditText(int id) {
		View v = findViewById(id);
		if (v == null) {
			return 0;
		}

		String str = ((EditText) v).getText().toString();

		int value = 0;
		try {
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			log.warn(str + " can not be parsed to int", e);
		}

		return value;
	}

	private void sendMasiveMessages(final int iterations, final Uri mediaUri) {
		final int delay = getIntValueFromEditText(R.id.delay_text);
		final String baseBody = getApplicationContext().getString(
				R.string.test_message);

		new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < iterations; i++) {
					if (delay > 0) {
						try {
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							log.warn("Cannot add delay", e);
						}
					}

					String body = baseBody + i;

					new SendMessageAsyncTask(MassiveTestActivity.this, body,
							mediaUri, mediaUri != null, timeline, Long
									.valueOf(System.currentTimeMillis()), null)
							.execute();
				}
			}
		}).start();
	}

	private void configurePopup() {
		ActionItem addItem = new ActionItem(ID_CAMERA,
				getString(R.string.popup_camera), getResources().getDrawable(
						android.R.drawable.ic_menu_camera));
		ActionItem acceptItem = new ActionItem(ID_VIDIO,
				getString(R.string.popup_vidio), getResources().getDrawable(
						android.R.drawable.ic_menu_slideshow));
		ActionItem uploadItem = new ActionItem(ID_GALLERY,
				getString(R.string.popup_gallery), getResources().getDrawable(
						android.R.drawable.ic_menu_gallery));

		mQuickAction = new QuickAction(this);

		mQuickAction.addActionItem(addItem);
		mQuickAction.addActionItem(acceptItem);
		mQuickAction.addActionItem(uploadItem);
		mQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(QuickAction quickAction, int pos,
							int actionId) {
						if (actionId == ID_CAMERA) {
							takePicture();
						} else if (actionId == ID_VIDIO) {
							recordVideo();
						} else if (actionId == ID_GALLERY) {
							takeGallery();
						}
					}
				});

		ImageButton mediaButton = ((ImageButton) this
				.findViewById(R.id.media_button));
		mediaButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mQuickAction.show(((ImageButton) MassiveTestActivity.this
						.findViewById(R.id.media_button)));
			}
		});
	}

	public void takeGallery() {
		Intent photoLibraryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		photoLibraryIntent.setType("image/* video/*");
		startActivityForResult(photoLibraryIntent, AppUtils.ACTION_GALLERY);
	}

	public void takePicture() {
		mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(), String
				.valueOf(System.currentTimeMillis())
				+ ConstantKeys.EXTENSION_JPG));
		FileUtils.takePicture(MassiveTestActivity.this, mFileCaptureUri);
	}

	public void recordVideo() {
		mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(), String
				.valueOf(System.currentTimeMillis())
				+ ConstantKeys.EXTENSION_3GP));
		FileUtils.recordVideo(MassiveTestActivity.this, mFileCaptureUri);
	}

	@Override
	protected void onResume() {
		super.onResume();

		AppUtils.CancelNotification(getApplicationContext());

		Account ac = AccountUtils.getAccount(this, true);
		if (ac != null) {
			getActionBar().setSubtitle(ac.name);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}

		if (requestCode == AppUtils.GROUP_ADMIN) {
			if (data.getExtras().getBoolean(ConstantKeys.DELETED, false)) {
				finish();
				return;
			}
		}

		if (requestCode == AppUtils.ACTION_GALLERY) {
			mFileCaptureUri = data.getData();
		}

		if (mFileCaptureUri == null) {
			Toast.makeText(
					getApplicationContext(),
					getApplicationContext().getText(
							R.string.error_capturing_media), Toast.LENGTH_SHORT)
					.show();
			System.gc();
			return;
		}

		ImageButton button = (ImageButton) this.findViewById(R.id.media_button);
		button.setBackgroundColor(Color.GREEN);
	}

	@Override
	public void onBackPressed() {
		Intent i = getIntent();
		i.putExtra(ConstantKeys.BACKBUTTON, true);
		setResult(RESULT_OK, i);
		MassiveTestActivity.this.finish();
		overridePendingTransition(R.anim.lefttoright_leftactivity,
				R.anim.lefttoright_rightactivity);
	}

}
