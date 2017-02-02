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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.MessageListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.ActionItem;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.PullToRefreshAttacher;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.PullToRefreshLayout;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.QuickAction;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonParser;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.SendMessageAsyncTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.MessageSend;

public class MessagesActivity extends AnalyticsBaseActivity {

	private static final int ID_CAMERA = 1;
	private static final int ID_VIDIO = 2;
	private static final int ID_GALLERY = 3;
	private QuickAction mQuickAction;

	private static final int ID_CALL = 1;
	private static final int ID_VIDEO_CALL = 2;
	private QuickAction callQuickAction;

	private static final Logger log = LoggerFactory
			.getLogger(MessagesActivity.class.getSimpleName());

	public MessageListAdapter adapter = null;
	public ListView listView = null;
	private boolean fromOnCreate = false;

	private List<MessageObject> messageList = new CopyOnWriteArrayList<MessageObject>();

	public float coordsY = 0;

	private Uri mFileCaptureUri;
	public TimelineObject timeline;

	public String userName = ConstantKeys.STRING_DEFAULT;

	private boolean fromGallery = false;

	protected GroupObject mGroup = null;
	protected UserObject mUser = null;
	private PullToRefreshAttacher mPullToRefreshAttacher;
	private ImageView updateBottom;

	private TextView messageTextView;
	private Animation myAnimationDown;

	private TextView timelineEnabledTextView;
	protected Menu mMenu = null;
	public Account account;

	protected MessagesActivity messagesActivity = null;

	private static final int REQUEST_CODE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timeline);

		messagesActivity = this;

		((ImageView) findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(+ R.drawable.bg))));

		timeline = (TimelineObject) getIntent().getSerializableExtra(
				ConstantKeys.TIMELINE);

		setTitle(timeline.getParty().getName());
		getActionBar().setIcon(R.drawable.ic_timeline_light);

		updateBottom = (ImageView) findViewById(R.id.update_bottom);
		messageTextView = (TextView) findViewById(R.id.message);
		myAnimationDown = AnimationUtils.loadAnimation(getApplicationContext(),
				R.anim.slide_down);

		timelineEnabledTextView = (TextView) findViewById(R.id.enabledMessage);

		adapter = new MessageListAdapter(messagesActivity, messageList,
				updateBottom, timeline);

		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(adapter);

		((ImageButton) findViewById(R.id.send_button))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						sendMessage();
					}
				});

		((ImageView) findViewById(R.id.attach_delete))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mFileCaptureUri = null;
						fromGallery = false;
						((RelativeLayout) messagesActivity
								.findViewById(R.id.atach_layer))
								.setVisibility(View.GONE);

					}
				});

		((EditText) findViewById(R.id.upload_description_content))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listView.getLastVisiblePosition() == listView
								.getCount() - 1) {
							new AsyncTask<Void, Void, Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									SystemClock.sleep(300);
									return null;
								}

								@Override
								protected void onPostExecute(Void result) {
									listView.setSelection(listView.getCount());
								}
							}.execute();
						}
					}
				});

		configurePopup();
		configureCallPopup();
		configurePullToRrefresh();

		fromOnCreate = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.messages_list_with_phone_menu, menu);

		if (mUser != null) {

			((MenuItem) menu.findItem(R.id.configure_groups)).setVisible(false);
		} else if (mGroup != null) {
			if (mGroup.getPhone().equals(ConstantKeys.STRING_DEFAULT)) {
				inflater.inflate(R.menu.messages_list_no_phone_menu, menu);
			} else {
				inflater.inflate(R.menu.messages_list_with_phone_menu, menu);
			}

			if (!mGroup.isAdmin()) {
				((MenuItem) menu.findItem(R.id.configure_groups))
						.setVisible(false);
			} else {
				((MenuItem) menu.findItem(R.id.configure_groups))
						.setVisible(true);
			}
		}

		if ((timeline != null) && (timeline.getState().equals(State.DISABLED))) {
			menu.clear();
		}

		mMenu = menu;

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE,
				messagesActivity);

		int itemId = item.getItemId();
		if (itemId == R.id.configure_groups) {
			Intent i = new Intent();
			i.setClass(getApplicationContext(), GroupAdminActivity.class);
			i.putExtra(ConstantKeys.GROUP, mGroup);
			startActivityForResult(i, AppUtils.GROUP_ADMIN);
		} else if (itemId == R.id.group_phone) {
			if (Build.VERSION.SDK_INT >= 23) {
				if (MessagesActivity.this.checkSelfPermission(Manifest.permission.CAMERA)
						== PackageManager.PERMISSION_GRANTED && MessagesActivity.this.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
						== PackageManager.PERMISSION_GRANTED) {
					Intent i = new Intent(messagesActivity, RoomActivity.class);
					i.putExtra(RoomActivity.EXTRA_ROOM_NAME, timeline.getParty().getId().toString());
					i.putExtra(RoomActivity.EXTRA_USER_NAME, "userTest");

					messagesActivity.startActivity(i);
				} else {
					requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
							REQUEST_CODE);
				}
			} else { //permission is automatically granted on sdk<23 upon installation
				Intent i = new Intent(messagesActivity, RoomActivity.class);
				i.putExtra(RoomActivity.EXTRA_ROOM_NAME, timeline.getParty().getId().toString());
				i.putExtra(RoomActivity.EXTRA_USER_NAME, "userTest");

				messagesActivity.startActivity(i);
			}
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == REQUEST_CODE && grantResults[0]== PackageManager.PERMISSION_GRANTED && grantResults[1]== PackageManager.PERMISSION_GRANTED){
			Intent i = new Intent(messagesActivity, RoomActivity.class);
			i.putExtra(RoomActivity.EXTRA_ROOM_NAME, timeline.getParty().getId().toString());
			i.putExtra(RoomActivity.EXTRA_USER_NAME, "userTest");

			messagesActivity.startActivity(i);
		}
	}


	private void configureCallPopup() {
		ActionItem callItem = new ActionItem(ID_CALL, null, messagesActivity
				.getResources()
				.getDrawable(R.drawable.ic_action_call));
		ActionItem videoCallItem = new ActionItem(ID_VIDEO_CALL, null,
				messagesActivity
						.getResources().getDrawable(R.drawable.ic_action_video));

		callQuickAction = new QuickAction(messagesActivity);

		boolean isVideoCallActive = AppUtils.getResource(this,
				R.bool.active_videocall_action, true);
		boolean isCallActive = AppUtils.getResource(this,
				R.bool.active_call_action, true);

		if (isCallActive) {
			if (((mUser != null) && (mUser.getPhone() != null)) || ((mGroup != null) && (mGroup.getPhone() != null))) {
				callQuickAction.addActionItem(callItem);
			}
		}
		if (isVideoCallActive) {
			callQuickAction.addActionItem(videoCallItem);
		}

		// setup the action item click listener
		callQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(QuickAction quickAction, int pos,
											int actionId) {
						if (actionId == ID_CALL) {
							callPhone();
						} else if (actionId == ID_VIDEO_CALL) {
							callVideo();
						}
					}
				});
	}

	private void callPhone() {
		if ((mGroup == null) && (mUser == null)) {
			log.warn("Group and user not set, cannot call");
			return;
		}

		String uri = null;
		if (mGroup != null) {
			if (mGroup.getPhone().equals(ConstantKeys.STRING_DEFAULT)) {
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getText(
								R.string.popup_call_error), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			uri = ConstantKeys.CALL_PHONE + mGroup.getPhone().trim();
		} else if (mUser != null) {
			if (mUser.getPhone().equals(ConstantKeys.STRING_DEFAULT)) {
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getText(
								R.string.popup_call_error), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			uri = ConstantKeys.CALL_PHONE + mUser.getPhone().trim();
		} else {
			return;
		}

		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse(uri));


		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

			return;
		}
		startActivity(intent);
	}

	protected void callVideo() {
		if ((mGroup == null) && (mUser == null)) {
			log.warn("Group and user not set, cannot call");
			return;
		}

		String userVideoName = "";
		if ((mGroup != null) || (mUser != null)){
			Account ac = AccountUtils.getAccount(messagesActivity, true);
			if (ac != null) {
				account = ac;
				AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
				userVideoName = am.getUserData(ac, JsonKeys.NAME) + am.getUserData(ac, JsonKeys.SURNAME);
				getActionBar().setSubtitle(userName);
			}

			Intent i = new Intent(messagesActivity, RoomActivity.class);
			i.putExtra(RoomActivity.EXTRA_ROOM_NAME, timeline.getParty().getId().toString());
			i.putExtra(RoomActivity.EXTRA_USER_NAME, userVideoName);

			messagesActivity.startActivity(i);
		} else {
			return;
		}
	}

	private void configurePullToRrefresh() {
		mPullToRefreshAttacher = PullToRefreshAttacher.get(messagesActivity);
		PullToRefreshLayout ptrLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
		ptrLayout.setPullToRefreshAttacher(mPullToRefreshAttacher,
				onRrefreshListener);
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

		mQuickAction = new QuickAction(messagesActivity);

		mQuickAction.addActionItem(addItem);
		mQuickAction.addActionItem(acceptItem);
		mQuickAction.addActionItem(uploadItem);

		// setup the action item click listener
		mQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(QuickAction quickAction, int pos,
							int actionId) {
						AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE,
								messagesActivity);

						if (actionId == ID_CAMERA) {
							takePicture();
						} else if (actionId == ID_VIDIO) {
							recordVideo();
						} else if (actionId == ID_GALLERY) {
							takeGallery();
						}
					}
				});

		ImageButton mediaButton = ((ImageButton) messagesActivity
				.findViewById(R.id.media_button));

		mediaButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(
						((EditText) messagesActivity.messagesActivity
						.findViewById(R.id.upload_description_content))
						.getWindowToken(), 0);

				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						SystemClock.sleep(200);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mQuickAction
								.show(((ImageButton) messagesActivity.messagesActivity
								.findViewById(R.id.media_button)));
					}
				}.execute();
			}
		});
	}

	private void takeGallery() {
		fromGallery = true;
		Intent photoLibraryIntent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		photoLibraryIntent.setType("image/* video/*");
		startActivityForResult(photoLibraryIntent, AppUtils.ACTION_GALLERY);
	}

	private void takePicture() {
		fromGallery = false;
		mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(), String
				.valueOf(System.currentTimeMillis())
				+ ConstantKeys.EXTENSION_JPG));
		FileUtils.takePicture(messagesActivity, mFileCaptureUri);
	}

	private void recordVideo() {
		fromGallery = false;
		mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(), String
				.valueOf(System.currentTimeMillis())
				+ ConstantKeys.EXTENSION_3GP));

		FileUtils.recordVideo(messagesActivity, mFileCaptureUri);
	}

	private void updateListView(boolean keepCurrentPos) {
		updateListView(keepCurrentPos, 0, false);
	}

	private void updateListView(boolean keepCurrentPos, int inc, boolean newList) {
		if (messageList.isEmpty()) {
			listView.setVisibility(View.GONE);
			return;
		}

		int idx = messageList.size() - 1;
		int offset = 0;
		if (keepCurrentPos) {
			idx = Math.min(idx, listView.getFirstVisiblePosition() + inc);
			View vfirst = listView.getChildAt(0);
			if (vfirst != null) {
				offset = vfirst.getTop();
			}
		}

		if ((newList) || (messageList.size() == 1)) {
			adapter = new MessageListAdapter(messagesActivity,
					messageList, updateBottom, timeline);
			listView.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}

		listView.setSelectionFromTop(idx, offset);
		listView.setVisibility(View.VISIBLE);
	}

	private void doRefresh(final boolean keepCurrentPos) {
		doRefresh(keepCurrentPos, 0, false);
	}

	private void doRefresh(final boolean keepCurrentPos, final int inc,
			final boolean newMsgs) {
		new AsyncTask<Void, Void, List<MessageObject>>() {
			@Override
			protected List<MessageObject> doInBackground(Void... params) {
				// First we need to know if the timeline has a new id to know if
				// this actual timeline is not a local anymore
				ArrayList<TimelineObject> list = DataBasesAccess.getInstance(
						getApplicationContext()).TimelinesDataBaseRead();
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i).getParty().getId()
							.equals(timeline.getParty().getId())) {
						timeline.setId(list.get(i).getId());
						break;
					}
				}

				List<MessageObject> auxList;
				if (timeline.getId() == ConstantKeys.LONG_DEFAULT) {
					auxList = DataBasesAccess.getInstance(
							getApplicationContext())
							.MessagesDataBaseReadSelected(null,
									timeline.getParty().getId());
				} else {
					auxList = DataBasesAccess.getInstance(
							getApplicationContext())
							.MessagesDataBaseReadSelected(timeline.getId(),
									null);
				}

				setupMessagesToBeShown(auxList);

				return auxList;
			}

			@Override
			protected void onPostExecute(List<MessageObject> list) {
				if (newMsgs
						&& listView.getLastVisiblePosition() < list.size() - 1) {
					updateBottom.setVisibility(View.VISIBLE);
				}

				messageList = new CopyOnWriteArrayList<MessageObject>(list);
				updateListView(keepCurrentPos, inc, true);
			}
		}.execute();
	}

	private void doRefreshMsgCreated(MessageObject mo) {
		if (!timeline.getId().equals(mo.getTimeline().getId())) {
			return;
		}

		setupMsgToBeShown(mo);
		messageList.add(mo);
		updateListView(false);
	}

	protected void setupMsgToBeShown(MessageObject mo) {
		if (mo.getContent().getContentSize() > 0) {
			mo.setHumanSize(AppUtils.humanReadableByteCount(mo.getContent()
					.getContentSize(), true));
		}

		Account ac = AccountUtils.getAccount(messagesActivity, false);
		if (ac == null) {
			log.warn("Cannot setup messages to be shown. Account is null");
			return;
		}

		AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
		String userId = am.getUserData(ac, JsonKeys.ID_STORED);
		String fromId = String.valueOf(mo.getFrom().getId());

		if (userId.equals(fromId)) {
			mo.setBackgroundColor(R.drawable.bg_right);
			mo.setMarginLeft(getResources().getDimensionPixelSize(
					R.dimen.message_margin));
			mo.setMarginRight(0);
			mo.setMessageFromSize(0);
			mo.setMessageStatusSize(getResources().getDimensionPixelSize(
					R.dimen.message_status_icon));
			mo.setAlignLayout(RelativeLayout.ALIGN_RIGHT);
			mo.setAvatarSize(0);
			mo.getFrom().setName(ConstantKeys.STRING_DEFAULT);
			mo.getFrom().setSurname(ConstantKeys.STRING_DEFAULT);
			mo.setCanCall(null);
		} else {
			mo.setMessageStatusSize(0);
			mo.setBackgroundColor(R.drawable.bg_left);
			mo.setMarginLeft(0);
			mo.setMarginRight(getResources().getDimensionPixelSize(
					R.dimen.message_margin));
			mo.setMessageFromSize(getResources().getDimensionPixelSize(
					R.dimen.message_from));
			mo.setAlignLayout(RelativeLayout.ALIGN_LEFT);
			mo.setAvatarSize(getResources().getDimensionPixelSize(
					R.dimen.message_avatar));
			mo.setCanCall(null);
		}

		if (mo.getContent().getId() != 0) {
			mo.setMediaThumbnailSize(getResources().getDimensionPixelSize(
					R.dimen.media_thumnail));
			mo.setMediaCancelButtonSize(0);
			mo.setMediaTypeIconSize(getResources().getDimensionPixelSize(
					R.dimen.media_type_icon));
			if (mo.getContent().getContentType().contains(ConstantKeys.IMAGE)) {
				mo.setMediaTypeIconResource(android.R.drawable.ic_menu_camera);
			} else if (mo.getContent().getContentType()
					.contains(ConstantKeys.VIDEO)) {
				mo.setMediaTypeIconResource(android.R.drawable.ic_media_play);
			} else {
				mo.setMediaTypeIconResource(android.R.drawable.ic_menu_gallery);
			}

		} else {
			mo.setMediaThumbnailSize(0);
			mo.setMediaCancelButtonSize(0);
			mo.setMediaTypeIconSize(0);
			// we need something to load, but it's not gonna be shown
			mo.setMediaTypeIconResource(android.R.drawable.ic_menu_gallery);
		}
	}

	private void setupMessagesToBeShown(List<MessageObject> messageList) {
		for (MessageObject mo : messageList) {
			setupMsgToBeShown(mo);
		}
	}

	private void sendMessage() {
		String messageBody = ((EditText) findViewById(R.id.upload_description_content))
				.getText().toString();

		if ((mFileCaptureUri == null) && messageBody.isEmpty()) {
			return;
		}

		String localId;
		if ((mFileCaptureUri != null) && (!fromGallery)) {
			localId = mFileCaptureUri
					.getLastPathSegment()
					.replace(ConstantKeys.EXTENSION_JPG,
							ConstantKeys.STRING_DEFAULT)
					.replace(ConstantKeys.EXTENSION_3GP,
							ConstantKeys.STRING_DEFAULT);
		} else {
			localId = String.valueOf(System.currentTimeMillis());
		}

		new SendMessageAsyncTask(messagesActivity, messageBody,
				mFileCaptureUri,
				fromGallery, timeline, Long.valueOf(localId), null).execute();
	}

	private void resetValues() {
		mFileCaptureUri = null;
		fromGallery = false;
		((EditText) messagesActivity
				.findViewById(R.id.upload_description_content))
				.setText(ConstantKeys.STRING_DEFAULT);
		((RelativeLayout) messagesActivity.findViewById(R.id.atach_layer))
				.setVisibility(View.GONE);
	}

	private void registerReceivers() {
		registerReceiver(mProgressbarReceiver, progressbarFilter);
		registerReceiver(mDownloadFinishReceiver, downloadFilter);
		registerReceiver(mTimelineChangedReceiver, timelineFilter);
		registerReceiver(mProgressbarDialogReceiver, progressbarDialogFilter);
		registerReceiver(mGCMReceiver, gcmFilter);
		registerReceiver(mMessageSentReceiver, messageSentFilter);
		registerReceiver(mMessageCreatedReceiver, messageCreatedFilter);

	}

	private void unregisterReceivers() {
		unregisterReceiver(mProgressbarReceiver);
		unregisterReceiver(mDownloadFinishReceiver);
		unregisterReceiver(mTimelineChangedReceiver);
		unregisterReceiver(mProgressbarDialogReceiver);
		unregisterReceiver(mGCMReceiver);
		unregisterReceiver(mMessageSentReceiver);
		unregisterReceiver(mMessageCreatedReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerReceivers();

		if (AppUtils.getDefaults(ConstantKeys.FROMLOGIN, messagesActivity)
				.equalsIgnoreCase("true")) {
			finish();
		}

		AppUtils.CancelNotification(getApplicationContext());

		Account ac = AccountUtils.getAccount(messagesActivity, true);
		if (ac != null) {
			account = ac;
			AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
			userName = am.getUserData(ac, JsonKeys.NAME) + " "
					+ am.getUserData(ac, JsonKeys.SURNAME);
			getActionBar().setSubtitle(userName);
		}

		// First we need to take the group from this timeline
		if (timeline.getParty().getType().equals(ConstantKeys.GROUP)) {
			ArrayList<GroupObject> groups = DataBasesAccess.getInstance(
					getApplicationContext()).GroupsDataBase(
					DataBasesAccess.READ, null);
			for (GroupObject group : groups) {
				if (group.getGroupId().equals(
						String.valueOf(timeline.getParty().getId()))) {
					mGroup = group;
					break;
				}
			}
		} else if (timeline.getParty().getType().equals(ConstantKeys.USER)) {
			ArrayList<UserObject> users = DataBasesAccess.getInstance(
					getApplicationContext()).UsersDataBase(
					DataBasesAccess.READ, null);
			for (UserObject user : users) {
				if (user.getId().toString()
						.equals(String.valueOf(timeline.getParty().getId()))) {
					mUser = user;
					break;
				}
			}
		}

		DataBasesAccess.getInstance(getApplicationContext())
				.TimelinesDataBaseWriteNewMessageFalse(timeline);

		doRefresh(!fromOnCreate);
		updateTimeline();
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceivers();

		DataBasesAccess.getInstance(getApplicationContext())
				.TimelinesDataBaseWriteNewMessageFalse(timeline);

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText) messagesActivity
				.findViewById(R.id.upload_description_content))
				.getWindowToken(), 0);

		for (MessageObject mo : messageList) {
			if (MessageObject.Status.NEW.equals(mo.getStatus())) {
				mo.incStatusAndUpdate(MessageObject.Status.READED, messagesActivity);
				DataBasesAccess.getInstance(getApplicationContext())
						.MessagesDataBaseSetMessageStatus(mo.getLocalId(),
								mo.getStatus());
			}
		}

		fromOnCreate = false;

		if (refreshAsyncTask != null) {
			refreshAsyncTask.cancel(false);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			resetValues();
			System.gc();
			fromGallery = false;

			return;
		}

		if (requestCode == AppUtils.GROUP_ADMIN) {
			if (data.getExtras().getBoolean(ConstantKeys.DELETED, false)) {
				finish();
			}
			return;
		}

		if (requestCode == AppUtils.ACTION_RESPONSE) {
			if (!data.getExtras().getBoolean(ConstantKeys.BACKBUTTON, false)) {
				doRefresh(true);
			}
			return;
		}

		if (requestCode == AppUtils.ACTION_GALLERY) {
			mFileCaptureUri = data.getData();
			fromGallery = true;
		}

		if (mFileCaptureUri == null) {
			Toast.makeText(
					getApplicationContext(),
					getApplicationContext().getText(
							R.string.error_capturing_media), Toast.LENGTH_SHORT)
					.show();
			resetValues();
			System.gc();
			fromGallery = false;
			return;
		}

		RelativeLayout rl = (RelativeLayout) messagesActivity
				.findViewById(R.id.atach_layer);
		rl.setVisibility(View.VISIBLE);
	}

	private void updateTimeline() {
		// ID at the same time that the group, then we could delete part of this
		// function
		if (mGroup != null) {
			mGroup = DataBasesAccess.getInstance(getApplicationContext())
					.getGroupDataBase(mGroup.getGroupId());
		}

		if ((timeline != null)
				&& (!timeline.getId().equals(ConstantKeys.LONG_DEFAULT))) {
			timeline = DataBasesAccess.getInstance(getApplicationContext())
					.TimelinesDataBaseReadTimelineIdSelected(timeline.getId());
		} else if (mGroup != null) {
			DataBasesAccess dba = DataBasesAccess.getInstance(messagesActivity);
			boolean recoverTimeline = dba
					.TimelinesDataBaseIsRecoverTimeline(Long.valueOf(mGroup
							.getGroupId()));
			if (recoverTimeline) {
				timeline = dba.TimelinesDataBaseReadPartyIdSelected(Long
						.valueOf(mGroup.getGroupId()));
			}
		} else if (mUser != null) {
			DataBasesAccess dba = DataBasesAccess.getInstance(messagesActivity);
			boolean recoverTimeline = dba
					.TimelinesDataBaseIsRecoverTimeline(Long.valueOf(mUser.getId()));
			if (recoverTimeline) {
				timeline = dba.TimelinesDataBaseReadPartyIdSelected(Long
						.valueOf(mUser.getId()));
			}
		}

		String name = ConstantKeys.STRING_WHITE;
		if (mGroup != null) {
			name = mGroup.getName();
		} else if (timeline != null) {
			name = timeline.getParty().getName();
		}

		if ((timeline != null) && (timeline.getState().equals(State.DISABLED))) {
			((ImageView) findViewById(R.id.separator_layer))
					.setVisibility(View.GONE);
			((RelativeLayout) findViewById(R.id.input_layer))
					.setVisibility(View.GONE);

			timelineEnabledTextView.setText(R.string.timeline_no_enabled);
			timelineEnabledTextView.setVisibility(View.VISIBLE);
		}

		if (mMenu != null) {
			this.onCreateOptionsMenu(mMenu);
		}

		setTitle(name);
	}

	/* Getting more messages */
	private RefreshAsyncTask refreshAsyncTask;

	private PullToRefreshAttacher.OnRefreshListener onRrefreshListener = new PullToRefreshAttacher.OnRefreshListener() {
		@Override
		public void onRefreshStarted(View view) {
			refreshAsyncTask = new RefreshAsyncTask();
			refreshAsyncTask.execute();
		}
	};

	private class RefreshAsyncTask extends
			AsyncTask<Void, Void, List<MessageObject>> {
		@Override
		protected List<MessageObject> doInBackground(Void... params) {
			JSONArray jsonObject = AppUtils.getMessagesFromServer(
					getApplicationContext(), timeline.getId().toString(), true,
					messageList.get(0).getId().toString());

			List<MessageObject> auxList = new ArrayList<MessageObject>();

			if (jsonObject == null || jsonObject.length() <= 0) {
				return auxList;
			}

			Context ctx = messagesActivity;
			Account account = AccountUtils.getAccount(ctx, false);
			AccountManager am = (AccountManager) ctx
					.getSystemService(Context.ACCOUNT_SERVICE);

			String userId = am.getUserData(account, JsonKeys.ID_STORED);

			for (int i = 0; i < jsonObject.length(); i++) {
				if (isCancelled()) {
					break;
				}

				MessageObject message;
				try {
					message = JsonParser.jsonToMessageRead(jsonObject.get(i)
							.toString(), true);
				} catch (JSONException e) {
					log.error("Error parsing message", e);
					continue;
				}

				String fromId = String.valueOf(message.getFrom().getId());
				if (userId.equals(fromId)) {
					message.incStatusAndUpdate(MessageObject.Status.ACK, ctx);
				} else {
					message.incStatusAndUpdate(MessageObject.Status.READED, ctx);
				}

				message.getTimeline().setId(timeline.getId());
				auxList.add(message);
				int num = DataBasesAccess.getInstance(getApplicationContext())
						.MessagesDataBaseRead().size();
				if (num < Integer.valueOf(Preferences
						.getMaxMessages(getApplicationContext()))) {
					DataBasesAccess.getInstance(getApplicationContext())
							.MessagesDataBaseWrite(message);
				}

				setupMsgToBeShown(message);
			}

			return auxList;
		}

		@Override
		protected void onPostExecute(List<MessageObject> list) {
			if (!list.isEmpty()) {
				for (MessageObject msg : list) {
					messageList.add(0, msg);
				}

				updateListView(true, list.size(), false);
			} else {
				showMessage(R.string.no_refresh_messages);
			}

			mPullToRefreshAttacher.setRefreshComplete();
		}
	};

	private void showMessage(int msgResId) {
		messageTextView.setText(msgResId);
		messageTextView.setVisibility(View.VISIBLE);
		messageTextView.startAnimation(myAnimationDown);
		messageTextView.setClickable(false);
		messageTextView.clearAnimation();

		final Handler h = new Handler();
		h.postDelayed(new Runnable() {
			@Override
			public void run() {
				messageTextView.setVisibility(View.GONE);
			}
		}, 3000);
	}

	private void CreateGroupDeletedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.group_deleted_popup_title);
		builder.setMessage(getString(R.string.group_deleted_popup_message));
		builder.setPositiveButton(getString(android.R.string.ok), null);
		builder.show();
	}

	/* Broadcast receivers */

	// TODO: get in the intent, a list of the timelines updates
	// and only make the refresh if is the actual one
	// registering our receiver
	private IntentFilter gcmFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM);
	private BroadcastReceiver mGCMReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_TIMELINE)) {
				updateTimeline();

				if (groupUpdatedListener != null) {
					groupUpdatedListener.groupUpdated(mGroup.getName());
				}
				groupUpdatedListener = null;
			}

			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_MESSAGE)) {
				String[] list = intent.getExtras().getStringArray(
						JsonKeys.COMMAND_TYPE_UPDATE_MESSAGE_ARRAY);
				for (String item : list) {
					if (item.equals(String.valueOf(timeline.getId()))) {
						doRefresh(true, 1, true);
						return;
					}
				}
			}

			if (mGroup != null
					&& intent.getExtras().getBoolean(
							Command.METHOD_UPDATE_GROUP)) {
				ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
						.getSerializableExtra(JsonKeys.COMMAND_TYPE_UPDATE_GROUP_ARRAY);

				for (GroupObject item : list) {
					if (item.getName().equals(mGroup.getName())) {
						mGroup = DataBasesAccess.getInstance(
								getApplicationContext()).getGroupDataBase(
								mGroup.getGroupId());

						if (mMenu != null) {
							messagesActivity.onCreateOptionsMenu(mMenu);
						}

						if (addGroupAdminListener != null) {
							addGroupAdminListener.adminAdded();
						}
						addGroupAdminListener = null;

						if (removeGroupAdminListener != null) {
							removeGroupAdminListener.adminRemoved();
						}
						removeGroupAdminListener = null;
					}
				}
			}

			if (intent.getExtras().getBoolean(Command.METHOD_DELETE_GROUP)) {
				ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
						.getSerializableExtra(JsonKeys.COMMAND_TYPE_DELETE_GROUP_ARRAY);

				for (GroupObject item : list) {
					if (item.getGroupId().equals(mGroup.getGroupId())) {
						CreateGroupDeletedDialog();
						mGroup = null;

						updateTimeline();

						if (groupDeletedListener != null) {
							groupDeletedListener.groupDeleted(item);
						}
						groupDeletedListener = null;
					}
				}
			}

			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
				if (account != null) {
					AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
					userName = am.getUserData(account, JsonKeys.NAME) + " "
							+ am.getUserData(account, JsonKeys.SURNAME);
					getActionBar().setSubtitle(userName);

					if (editUserListener != null) {
						editUserListener.userEdited();
					}
					editUserListener = null;
				}
			}
		}
	};

	private IntentFilter messageCreatedFilter = new IntentFilter(
			ConstantKeys.BROADCAST_MESSAGE_CREATED);
	private BroadcastReceiver mMessageCreatedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			MessageObject mo = (MessageObject) intent.getExtras()
					.getSerializable(ConstantKeys.MESSAGE_CREATED_EXTRA);
			doRefreshMsgCreated(mo);
			resetValues();
		}
	};

	private IntentFilter messageSentFilter = new IntentFilter(
			ConstantKeys.BROADCAST_MESSAGE_SENT);
	private BroadcastReceiver mMessageSentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String messageLocalId = intent.getExtras().getString(
					ConstantKeys.LOCALID);

			// TODO we must to control this in another way
			if (adapter != null) {
				adapter.refreshMessageStatustoSent(Long.valueOf(messageLocalId));
			}
		}
	};

	private IntentFilter progressbarFilter = new IntentFilter(
			ConstantKeys.BROADCAST_PROGRESSBAR);
	private BroadcastReceiver mProgressbarReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String json = (String) intent.getExtras().get(ConstantKeys.JSON);
			MessageSend message;
			try {
				JSONObject obj = new JSONObject(json);
				message = JsonParser.jsonToMessageSend(obj.getJSONObject(
						JsonKeys.PARAMS).toString());
			} catch (JSONException e) {
				log.error("Broadcast message is malformed", e);
				return;
			}

			if (adapter != null) {
				adapter.refreshProgress(message.getLocalId(), intent
						.getExtras().getInt(ConstantKeys.TOTAL), false);
			}

			int total = Integer.valueOf(intent.getExtras().getInt(
					ConstantKeys.TOTAL));

			DataBasesAccess.getInstance(context.getApplicationContext())
					.MessagesDataBaseWriteTotal(
							message.getLocalId().toString(), total);
		}
	};

	private IntentFilter downloadFilter = new IntentFilter(
			ConstantKeys.BROADCAST_DIALOG_DOWNLOAD_FINISH);
	private BroadcastReceiver mDownloadFinishReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				if (intent.getExtras().get(ConstantKeys.LOCALID) instanceof String) {
					Long localId = Long.valueOf((String) intent.getExtras()
							.get(ConstantKeys.LOCALID));

					for (int i = 0; i < AppUtils.getlistOfDownload().size(); i++) {
						if (AppUtils.getlistOfDownload().get(i).equals(localId)) {
							AppUtils.getlistOfDownload().remove(i);
						}
					}

					DataBasesAccess
							.getInstance(context.getApplicationContext())
							.MessagesDataBaseWriteTotal(localId.toString(), 100);

					// TODO we must to control this in another way
					if (adapter != null) {
						adapter.refreshProgress(localId, 100, true);
					}
				}
			} catch (Exception e) {
				log.debug("localId has some bad value", e);
			}
		}
	};

	private IntentFilter timelineFilter = new IntentFilter(
			ConstantKeys.BROADCAST_TIMELINE_UPDATE);
	private BroadcastReceiver mTimelineChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String timelineReceived = (String) intent.getExtras().get(
					ConstantKeys.TIMELINE);
			String value = (String) intent.getExtras().get(ConstantKeys.DRIFT);
			if (timeline.getId().toString().equals(timelineReceived)) {
				timeline.setTimestampDrift(Long.valueOf(value));
			}
		}
	};

	private IntentFilter progressbarDialogFilter = new IntentFilter(
			ConstantKeys.BROADCAST_DIALOG_PROGRESSBAR);
	private BroadcastReceiver mProgressbarDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				int total = (Integer) intent.getExtras()
						.get(ConstantKeys.TOTAL);
				Long id = (Long) intent.getExtras().get(ConstantKeys.LOCALID);

				// TODO we must to control this in another way
				if (adapter != null) {
					adapter.refreshProgress(id, total, true);
				}
				DataBasesAccess.getInstance(context.getApplicationContext())
						.MessagesDataBaseWriteTotal(id.toString(), total);

			} catch (Exception e) {
				log.error("Broadcast fail to take the dialog", e);
			}
		}
	};

	/* Test utilities */
	private EditUserConversationListener editUserListener;

	public interface EditUserConversationListener {
		void userEdited();
	}

	public void setEditUserListener(EditUserConversationListener l) {
		this.editUserListener = l;
	}

	private GroupUpdatedConversationListener groupUpdatedListener;

	public interface GroupUpdatedConversationListener {
		void groupUpdated(String groupName);
	}

	public void setGroupUpdatedListener(GroupUpdatedConversationListener l) {
		this.groupUpdatedListener = l;
	}

	private GroupDeletedConversationListener groupDeletedListener;

	public interface GroupDeletedConversationListener {
		void groupDeleted(GroupObject group);
	}

	public void setGroupDeletedListener(GroupDeletedConversationListener l) {
		this.groupDeletedListener = l;
	}

	private AddGroupAdminItemListListener addGroupAdminListener;

	public interface AddGroupAdminItemListListener {
		void adminAdded();
	}

	public void setAddGroupAdminListener(AddGroupAdminItemListListener l) {
		this.addGroupAdminListener = l;
	}

	private RemoveGroupAdminItemListListener removeGroupAdminListener;

	public interface RemoveGroupAdminItemListListener {
		void adminRemoved();
	}

	public void setRemoveGroupAdminListener(RemoveGroupAdminItemListListener l) {
		this.removeGroupAdminListener = l;
	}
}
