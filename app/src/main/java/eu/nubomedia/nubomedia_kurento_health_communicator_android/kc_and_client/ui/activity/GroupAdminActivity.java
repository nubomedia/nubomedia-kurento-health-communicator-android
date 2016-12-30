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

import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.GroupAdminUsersFragmentListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.ActionItem;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.QuickAction;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
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
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;

public class GroupAdminActivity extends AnalyticsBaseActivity {

	private static final Logger log = LoggerFactory
			.getLogger(GroupAdminActivity.class.getSimpleName());

	private static final int ID_CAMERA = 1;
	private static final int ID_GALLERY = 2;

	private static final int CROP_CAMERA_IMAGE = 100;
	private static final int PICK_FROM_CAMERA = 101;
	public static final int DELETE_GROUP = 102;
	public static final int EXIT_GROUP = 103;

	private Uri mFileCaptureUri;
	private String tempFilePath;
	public static Context mContext = null;
	public String userName = ConstantKeys.STRING_DEFAULT;
	private InputMethodManager imm;
	private QuickAction mQuickAction;
	private boolean onEdit = false;

	public static ListView usersListView;
	public static GroupAdminUsersFragmentListAdapter usersAdapter = null;

	private BroadcastReceiver mReceiver;
	private IntentFilter intentFilter;

	public static GroupObject mGroup;

	private Menu mMenu;
	private Account account;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group_admin);
		mContext = this;

		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(R.drawable.bg))));

		getActionBar().setIcon(R.drawable.ic_edit_light);
		setTitle(getString(R.string.admin_group));

		mGroup = (GroupObject) getIntent().getSerializableExtra(
				ConstantKeys.GROUP);

		((EditText) findViewById(R.id.name_txt_editable)).setText(mGroup
				.getName());
		((TextView) findViewById(R.id.name_txt_no_editable)).setText(mGroup
				.getName());
		((EditText) findViewById(R.id.phone_txt_editable)).setText(mGroup
				.getPhone());
		((TextView) findViewById(R.id.phone_txt_no_editable)).setText(mGroup
				.getPhone());

		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		usersListView = (ListView) findViewById(R.id.user_list);

		((ImageView) findViewById(R.id.edit_image))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						((TextView) findViewById(R.id.name_txt_no_editable))
								.setText(((EditText) findViewById(R.id.name_txt_editable))
										.getText().toString());

						((TextView) findViewById(R.id.phone_txt_no_editable))
								.setText(((EditText) findViewById(R.id.phone_txt_editable))
										.getText().toString());

						if (onEdit) {

							AlertDialog.Builder builder = new AlertDialog.Builder(
									GroupAdminActivity.this);
							builder.setTitle(R.string.upload_group_changes);
							builder.setMessage(getString(R.string.upload_group_changes_text));
							builder.setPositiveButton(
									getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											editGroup();
										}
									});

							builder.setNegativeButton(
									getString(android.R.string.cancel),
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											((EditText) findViewById(R.id.name_txt_editable))
													.setText(mGroup.getName());
											((TextView) findViewById(R.id.name_txt_no_editable))
													.setText(mGroup.getName());

											((EditText) findViewById(R.id.phone_txt_editable))
													.setText(mGroup.getPhone());
											((TextView) findViewById(R.id.phone_txt_no_editable))
													.setText(mGroup.getPhone());
											resetEditables();
										}
									});
							builder.show();

						} else {
							((ImageView) findViewById(R.id.edit_image))
									.setImageResource(android.R.drawable.ic_menu_save);
							((ImageView) findViewById(R.id.group_image_blue))
									.setVisibility(View.VISIBLE);
							onEdit = true;
							((EditText) findViewById(R.id.name_txt_editable))
									.setKeyListener((KeyListener) ((EditText) findViewById(R.id.name_txt_editable))
											.getTag());
							((EditText) findViewById(R.id.name_txt_editable))
									.requestFocus();
							((EditText) findViewById(R.id.name_txt_editable))
									.setClickable(true);
							findViewById(R.id.name_txt_editable).setVisibility(
									View.VISIBLE);
							findViewById(R.id.name_txt_no_editable)
									.setVisibility(View.GONE);

							((EditText) findViewById(R.id.phone_txt_editable))
									.setKeyListener((KeyListener) ((EditText) findViewById(R.id.phone_txt_editable))
											.getTag());
							((EditText) findViewById(R.id.phone_txt_editable))
									.requestFocus();
							((EditText) findViewById(R.id.phone_txt_editable))
									.setClickable(true);
							findViewById(R.id.phone_txt_editable)
									.setVisibility(View.VISIBLE);
							findViewById(R.id.phone_txt_no_editable)
									.setVisibility(View.GONE);

							findViewById(R.id.cancel_edit).setVisibility(
									View.VISIBLE);

						}
					}
				});

		ImageView image = (ImageView) findViewById(R.id.group_image);
		image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.name_txt_editable))
								.getWindowToken(), 0);
				if (!onEdit) {
					((ImageView) findViewById(R.id.edit_image))
							.setImageResource(android.R.drawable.ic_menu_save);
					((ImageView) findViewById(R.id.group_image_blue))
							.setVisibility(View.VISIBLE);
					onEdit = true;
					((EditText) findViewById(R.id.name_txt_editable))
							.setKeyListener((KeyListener) ((EditText) findViewById(R.id.name_txt_editable))
									.getTag());
					((EditText) findViewById(R.id.name_txt_editable))
							.requestFocus();
					((EditText) findViewById(R.id.name_txt_editable))
							.setClickable(true);
					findViewById(R.id.name_txt_editable).setVisibility(
							View.VISIBLE);
					findViewById(R.id.name_txt_no_editable).setVisibility(
							View.GONE);

					((EditText) findViewById(R.id.phone_txt_editable))
							.setKeyListener((KeyListener) ((EditText) findViewById(R.id.phone_txt_editable))
									.getTag());
					((EditText) findViewById(R.id.phone_txt_editable))
							.requestFocus();
					((EditText) findViewById(R.id.phone_txt_editable))
							.setClickable(true);
					findViewById(R.id.phone_txt_editable).setVisibility(
							View.VISIBLE);
					findViewById(R.id.phone_txt_no_editable).setVisibility(
							View.GONE);

					findViewById(R.id.cancel_edit).setVisibility(View.VISIBLE);
				}
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							sleep(200);
							handlerGoDown.post(runGoDown);
						} catch (InterruptedException e) {
							//
						}
					}
				};
				thread.start();
			}

		});

		ImageView cancelEdit = (ImageView) findViewById(R.id.cancel_edit);
		cancelEdit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.name_txt_editable))
								.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.phone_txt_editable))
								.getWindowToken(), 0);

				resetEditables();

				// Here we need to refresh the data on screen
				((EditText) findViewById(R.id.name_txt_editable))
						.setText(mGroup.getName());
				((TextView) findViewById(R.id.name_txt_no_editable))
						.setText(mGroup.getName());
				((EditText) findViewById(R.id.phone_txt_editable))
						.setText(mGroup.getPhone());
				((TextView) findViewById(R.id.phone_txt_no_editable))
						.setText(mGroup.getPhone());

				mFileCaptureUri = null;
				tempFilePath = null;
				ImageView image = (ImageView) findViewById(R.id.group_image);
				ImageDownloader downloader = new ImageDownloader();
				try {
					downloader.downloadGroupAvatar(
							mContext.getApplicationContext(),
							mGroup.getGroupId(),
							mGroup.getPicture().toString(), image);
				} catch (Exception e) {
					log.warn("No avatar loaded");
				}
			}
		});

		if (!mGroup.isAdmin()) {
			((ImageView) findViewById(R.id.edit_image))
					.setVisibility(View.GONE);
			image.setClickable(false);
		}

		((EditText) findViewById(R.id.name_txt_editable))
				.setTag(((EditText) findViewById(R.id.name_txt_editable))
						.getKeyListener());
		((EditText) findViewById(R.id.name_txt_editable)).setKeyListener(null);
		((EditText) findViewById(R.id.name_txt_editable)).clearFocus();

		((EditText) findViewById(R.id.phone_txt_editable))
				.setTag(((EditText) findViewById(R.id.phone_txt_editable))
						.getKeyListener());
		((EditText) findViewById(R.id.phone_txt_editable)).setKeyListener(null);
		((EditText) findViewById(R.id.phone_txt_editable)).clearFocus();

		ImageDownloader downloader = new ImageDownloader();
		try {
			downloader.downloadGroupAvatar(mContext.getApplicationContext(),
					mGroup.getGroupId(), mGroup.getPicture().toString(), image);
		} catch (Exception e) {
			log.warn("No avatar loaded");
		}

		configurePopup();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (onEdit) {
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.name_txt_editable))
								.getWindowToken(), 0);
				imm.hideSoftInputFromWindow(
						((EditText) findViewById(R.id.phone_txt_editable))
								.getWindowToken(), 0);

				resetEditables();

				// Here we need to refresh the data on screen
				((EditText) findViewById(R.id.name_txt_editable))
						.setText(mGroup.getName());
				((TextView) findViewById(R.id.name_txt_no_editable))
						.setText(mGroup.getName());
				((EditText) findViewById(R.id.phone_txt_editable))
						.setText(mGroup.getPhone());
				((TextView) findViewById(R.id.phone_txt_no_editable))
						.setText(mGroup.getPhone());

				mFileCaptureUri = null;
				tempFilePath = null;
				ImageView image = (ImageView) findViewById(R.id.group_image);
				ImageDownloader downloader = new ImageDownloader();
				try {
					downloader.downloadGroupAvatar(
							mContext.getApplicationContext(),
							mGroup.getGroupId(),
							mGroup.getPicture().toString(), image);
				} catch (Exception e) {
					log.warn("No avatar loaded");
				}

				return false;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	private void setupMembers() {
		ArrayList<GroupMemberObject> users = DataBasesAccess.getInstance(
				mContext.getApplicationContext()).GroupMembersDataBase(
				DataBasesAccess.READ_SELECTED, null,
				Long.valueOf(mGroup.getGroupId()));

		ArrayList<GroupMemberObject> aux = new ArrayList<GroupMemberObject>();
		for (int i = 0; i < users.size(); i++) {
			if (!users.get(i).isToDeleted()) {
				if (users.get(i).isAdmin()) {
					users.get(i).setAdminLayer(
							mContext.getString(R.string.admin_layer));
				}
				aux.add(users.get(i));
			}
		}

		users = aux;

		usersAdapter = new GroupAdminUsersFragmentListAdapter(mContext, users,
				mGroup.isAdmin());
		usersListView.setAdapter(usersAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		MenuInflater inflater = getMenuInflater();
		// TODO this feature must to be checked
		if (mGroup.isAdmin()) {
			inflater.inflate(R.menu.group_admin_menu, menu);
		} else {
			inflater.inflate(R.menu.group_no_admin_menu, menu);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE, this);

		int itemId = item.getItemId();
		if (itemId == R.id.invitate_user) {
			Intent editUserIntent = new Intent(this, SearchUserActivity.class);
			editUserIntent.putExtra(ConstantKeys.GROUP, mGroup);
			startActivityForResult(editUserIntent, AppUtils.GROUP_ADMIN);
		} else if (itemId == R.id.delete_group) {
			if (mGroup.isAdmin()) {
				showDialog(mGroup, DELETE_GROUP);
			} else {
				Toast.makeText(
						mContext.getApplicationContext(),
						mContext.getApplicationContext().getText(
								R.string.group_cant_delete), Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		} else if (itemId == R.id.exit_group) {
			if (mGroup.getCanLeave()) {
				showDialog(mGroup, EXIT_GROUP);
			} else {
				Toast.makeText(
						mContext.getApplicationContext(),
						mContext.getApplicationContext().getText(
								R.string.group_cant_leave), Toast.LENGTH_SHORT)
						.show();
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showDialog(final GroupObject mGroup, final int actionType) {
		String title = "";
		String message = "";
		if (actionType == DELETE_GROUP) {
			title = getString(R.string.delete_group);
			message = getString(R.string.delete_group_message);
		} else if (actionType == EXIT_GROUP) {
			title = getString(R.string.exit_group);
			message = getString(R.string.exit_group_message);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(
				GroupAdminActivity.this);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						deleteFromList();
						sendItemComand(mGroup, actionType);
					}
				});

		builder.setNegativeButton(getString(android.R.string.cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// In this case we don't do anaything
						return;
					}
				});
		builder.show();
	}

	private void deleteFromList() {
		DataBasesAccess.getInstance(mContext.getApplicationContext())
				.GroupsDataBase(DataBasesAccess.DELETE_PROCESS, mGroup);

		DataBasesAccess.getInstance(mContext.getApplicationContext())
				.TimelinesDataBaseSetState(Long.valueOf(mGroup.getGroupId()),
						State.DISABLED);
		Intent returnIntent = new Intent();
		returnIntent.putExtra(ConstantKeys.DELETED, true);
		setResult(RESULT_OK, returnIntent);
		finish();
	}

	private void sendItemComand(final GroupObject actionItem,
			final int commandType) {

		new AsyncTask<Void, Void, String>() {
			private GroupObject localItem = actionItem;

			@Override
			protected String doInBackground(Void... params) {
				// Let's put the timeline to DISABLE status
				DataBasesAccess.getInstance(mContext.getApplicationContext())
						.TimelinesDataBaseSetState(
								Long.valueOf(localItem.getGroupId()), State.DISABLED);

				CommandStoreService cs = new CommandStoreService(mContext);
				try {
					if (commandType == EXIT_GROUP) {
						JSONObject jsonToSend = new JSONObject();
						JSONObject party = new JSONObject();
						JSONObject user = new JSONObject();
						party.put(JsonKeys.ID, localItem.getGroupId());

						String userId = null;
						if (account != null) {
							AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
							userId = am.getUserData(account, JsonKeys.ID_STORED);
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}

						user.put(JsonKeys.ID, userId);

						jsonToSend.put(JsonKeys.GROUP, party);
						jsonToSend.put(JsonKeys.USER, user);

						boolean result = cs.createCommand(
								jsonToSend.toString(),
								Command.METHOD_REMOVE_GROUP_MEMBER, null);
						if (result) {
							return ConstantKeys.SENDING_OK;
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}

					} else if (commandType == DELETE_GROUP) {
						JSONObject jsonToSend = new JSONObject();
						jsonToSend.put(JsonKeys.ID,
								Long.valueOf(localItem.getGroupId()));

						boolean result = cs.createCommand(
								jsonToSend.toString(),
								Command.METHOD_DELETE_GROUP, null);

						if (result) {
							return ConstantKeys.SENDING_OK;
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}
					} else {
						return ConstantKeys.SENDING_FAIL;
					}

				} catch (JSONException e) {
					log.error("Cannot send command", e);
					return ConstantKeys.SENDING_FAIL;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				if (result.equals(ConstantKeys.SENDING_OK)) {
					log.trace("Command send to database");
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(
							mContext.getApplicationContext(),
							mContext.getApplicationContext().getText(
									R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					if (commandType == EXIT_GROUP) {
						Toast.makeText(
								mContext.getApplicationContext(),
								mContext.getApplicationContext().getText(
										R.string.group_exit_error),
								Toast.LENGTH_SHORT).show();
					} else if (commandType == DELETE_GROUP) {
						Toast.makeText(
								mContext.getApplicationContext(),
								mContext.getApplicationContext().getText(
										R.string.group_delete_error),
								Toast.LENGTH_SHORT).show();
					}

					// Lets restore all the data
					DataBasesAccess.getInstance(
							mContext.getApplicationContext())
							.TimelinesDataBaseSetState(
									Long.valueOf(localItem.getGroupId()), State.ENABLED);
					DataBasesAccess.getInstance(
							mContext.getApplicationContext()).GroupsDataBase(
							DataBasesAccess.CANCEL_DELETE_PROCESS, localItem);
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	final Handler handlerAuthError = new Handler();

	final Runnable runAuthError = new Runnable() {
		public void run() {
			Toast.makeText(
					mContext.getApplicationContext(),
					mContext.getApplicationContext()
							.getText(R.string.auth_fail), Toast.LENGTH_SHORT)
					.show();
		}
	};

	private void editGroup() {
		final ProgressDialog pd = new ProgressDialog(this);
		pd.setTitle(R.string.editing_group);
		pd.setMessage(getString(R.string.please_wait));
		pd.setCancelable(false);
		pd.show();

		final String name = ((EditText) ((Activity) mContext)
				.findViewById(R.id.name_txt_editable)).getText()
				.toString();

		final String phone = ((EditText) ((Activity) mContext)
				.findViewById(R.id.phone_txt_editable)).getText()
				.toString();

		new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {

				String path = null;
				if (mFileCaptureUri != null) {
					path = FileUtils.getRealPathFromURI(mFileCaptureUri,
							(Activity) mContext);
				}

				return AppUtils.editGroup(mContext, path, mGroup.getGroupId(),
						name, phone, true);
			}

			@Override
			protected void onPostExecute(String result) {
				pd.cancel();
				if (result.equals(ConstantKeys.SENDING_OK)) {
					log.trace("Command send to database");
					mGroup.setName(((EditText) findViewById(R.id.name_txt_editable))
							.getText().toString());
					mGroup.setPhone(((EditText) findViewById(R.id.phone_txt_editable))
							.getText().toString());
					DataBasesAccess.getInstance(getApplicationContext())
							.GroupsDataBase(DataBasesAccess.WRITE, mGroup);
					resetEditables();

				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mContext,
							getString(R.string.editing_group_error),
							Toast.LENGTH_SHORT).show();
					((EditText) findViewById(R.id.name_txt_editable))
							.setText(mGroup.getName());
					((TextView) findViewById(R.id.name_txt_no_editable))
							.setText(mGroup.getName());

					((EditText) findViewById(R.id.phone_txt_editable))
							.setText(mGroup.getPhone());
					((TextView) findViewById(R.id.phone_txt_no_editable))
							.setText(mGroup.getPhone());
					resetEditables();
				}
				FileUtils.deleteTemp(tempFilePath);
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void resetEditables() {
		((ImageView) findViewById(R.id.edit_image))
				.setImageResource(android.R.drawable.ic_menu_edit);
		((ImageView) findViewById(R.id.group_image_blue))
				.setVisibility(View.GONE);
		onEdit = false;
		((EditText) findViewById(R.id.name_txt_editable)).clearFocus();
		((EditText) findViewById(R.id.name_txt_editable)).setClickable(false);
		((EditText) findViewById(R.id.name_txt_editable)).setKeyListener(null);
		findViewById(R.id.name_txt_editable).setVisibility(View.GONE);
		findViewById(R.id.name_txt_no_editable).setVisibility(View.VISIBLE);

		((EditText) findViewById(R.id.phone_txt_editable)).clearFocus();
		((EditText) findViewById(R.id.phone_txt_editable)).setClickable(false);
		((EditText) findViewById(R.id.phone_txt_editable)).setKeyListener(null);
		findViewById(R.id.phone_txt_editable).setVisibility(View.GONE);
		findViewById(R.id.phone_txt_no_editable).setVisibility(View.VISIBLE);

		findViewById(R.id.cancel_edit).setVisibility(View.GONE);
	}

	final Handler handlerGoDown = new Handler();

	final Runnable runGoDown = new Runnable() {
		public void run() {
			mQuickAction.show(((ImageView) findViewById(R.id.group_image)));
		}
	};

	public void takeGallery() {
		Intent photoLibraryIntent = new Intent(Intent.ACTION_PICK,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		photoLibraryIntent.setType(ConstantKeys.IMAGE_CROP);
		startActivityForResult(photoLibraryIntent, ID_GALLERY);
	}

	public void takePicture() {
		mFileCaptureUri = Uri.fromFile(new File(FileUtils.getDir(),
				ConstantKeys.TEMP + String.valueOf(System.currentTimeMillis())
						+ ConstantKeys.EXTENSION_JPG));

		tempFilePath = mFileCaptureUri.getPath();

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileCaptureUri);

		try {
			intent.putExtra(ConstantKeys.RETURNDATA, true);
			startActivityForResult(intent, PICK_FROM_CAMERA);
		} catch (ActivityNotFoundException e) {
			log.warn("Cannot take photo", e);
		}
	}

	private void configurePopup() {
		ActionItem cameraItem = new ActionItem(ID_CAMERA,
				getString(R.string.popup_camera), getResources().getDrawable(
						android.R.drawable.ic_menu_camera));
		ActionItem galleryItem = new ActionItem(ID_GALLERY,
				getString(R.string.popup_gallery), getResources().getDrawable(
						android.R.drawable.ic_menu_gallery));

		mQuickAction = new QuickAction(this);

		mQuickAction.addActionItem(cameraItem);
		mQuickAction.addActionItem(galleryItem);

		// setup the action item click listener
		mQuickAction
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(QuickAction quickAction, int pos,
							int actionId) {
						if (actionId == ID_CAMERA) {
							takePicture();
						} else if (actionId == ID_GALLERY) {
							takeGallery();
						}
					}
				});
	}

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

	@Override
	protected void onResume() {
		super.onResume();

		// registering our receiver
		intentFilter = new IntentFilter(ConstantKeys.BROADCAST_GCM);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getExtras().getBoolean(
						Command.METHOD_ADD_GROUP_MEMBER)) {
					setupMembers();

					if (addUserGroupListener != null) {
						addUserGroupListener.addUserGroup();
					}
					addUserGroupListener = null;

					if (removeGroupMemberListener != null) {
						removeGroupMemberListener.userRemoved();
					}
					removeGroupMemberListener = null;

					if (addGroupAdminListener != null) {
						addGroupAdminListener.adminAdded();
					}
					addGroupAdminListener = null;

					if (removeGroupAdminListener != null) {
						removeGroupAdminListener.adminRemoved();
					}
					removeGroupAdminListener = null;
				}

				if (intent.getExtras().getBoolean(Command.METHOD_DELETE_GROUP)) {
					ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
							.getSerializableExtra(JsonKeys.COMMAND_TYPE_DELETE_GROUP_ARRAY);

					for (GroupObject item : list) {
						if (item.getGroupId().equals(mGroup.getGroupId())) {
							CreateGroupDeletedDialog();

							if (groupDeletedListener != null) {
								groupDeletedListener
										.groupDeleted(item);
							}
							groupDeletedListener = null;
						}
					}
				}

				if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
					if (account != null) {
						AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
						String userName = am
								.getUserData(account, JsonKeys.NAME)
								+ " "
								+ am.getUserData(account, JsonKeys.SURNAME);
						getActionBar().setSubtitle(userName);
					}
				}

				if (intent.getExtras()
						.getBoolean(Command.METHOD_UPDATE_CONTACT)) {
					setupMembers();
				}

				if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_GROUP)) {

					ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
							.getSerializableExtra(JsonKeys.COMMAND_TYPE_UPDATE_GROUP_ARRAY);

					for (GroupObject item : list) {
						if (item.getGroupId().equals(
								String.valueOf(mGroup.getGroupId()))) {
							if (!item.isAdmin()) {
								CreateAdminDowngradedDialog();

								return;
							}

							// Here we need to refresh the data on screen
							((EditText) findViewById(R.id.name_txt_editable))
									.setText(item.getName());
							((TextView) findViewById(R.id.name_txt_no_editable))
									.setText(item.getName());
							((EditText) findViewById(R.id.phone_txt_editable))
									.setText(item.getPhone());
							((TextView) findViewById(R.id.phone_txt_no_editable))
									.setText(item.getPhone());

							ImageView image = (ImageView) findViewById(R.id.group_image);
							ImageDownloader downloader = new ImageDownloader();
							try {
								downloader.downloadGroupAvatar(mContext
										.getApplicationContext(), mGroup
										.getGroupId(), mGroup.getPicture()
										.toString(), image);
							} catch (Exception e) {
								log.warn("No avatar loaded");
							}

							mGroup = item;
							mMenu.clear();

							// Refresh menu
							MenuInflater inflater = getMenuInflater();
							if (mGroup.isAdmin()) {
								inflater.inflate(R.menu.group_admin_menu, mMenu);
							} else {
								inflater.inflate(R.menu.group_no_admin_menu,
										mMenu);
							}

							if (groupUpdatedListener != null) {
								groupUpdatedListener.groupUpdated(item
										.getName());
							}

							return;
						}
					}
				}
			}
		};
		registerReceiver(mReceiver, intentFilter);

		if (AppUtils.getDefaults(ConstantKeys.FROMLOGIN, this)
				.equalsIgnoreCase("true")) {
			finish();
		}

		AppUtils.CancelNotification(getApplicationContext());

		try {
			Account ac = AccountUtils.getAccount(this, true);
			if (ac != null) {
				account = ac;
				AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
				userName = am.getUserData(ac, JsonKeys.NAME) + " "
						+ am.getUserData(ac, JsonKeys.SURNAME);
				getActionBar().setSubtitle(userName);
			}
		} catch (Exception e) {
			log.error("Error trying to get user data", e);
		}

		setupMembers();
	}

	private void CreateGroupDeletedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				GroupAdminActivity.this);
		builder.setTitle(R.string.group_deleted_popup_title);
		builder.setMessage(getString(R.string.group_deleted_popup_message));
		builder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent returnIntent = new Intent();
						returnIntent.putExtra(ConstantKeys.DELETED, true);
						setResult(RESULT_OK, returnIntent);
						finish();
					}
				});
		builder.show();
	}

	private void CreateAdminDowngradedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				GroupAdminActivity.this);
		builder.setTitle(R.string.group_remove_admin_popup_title);
		builder.setMessage(R.string.group_remove_admin_popup_message);
		builder.setPositiveButton(getString(android.R.string.ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent returnIntent = new Intent();
						returnIntent.putExtra(ConstantKeys.DELETED, false);
						setResult(RESULT_OK, returnIntent);
						finish();
					}
				});
		builder.show();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode != RESULT_OK) {
			mFileCaptureUri = null;
			return;
		}

		if (requestCode == AppUtils.GROUP_ADMIN) {
			if (data.getExtras().getBoolean(ConstantKeys.DELETED, false)) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra(ConstantKeys.DELETED, true);
				setResult(RESULT_OK, returnIntent);
				finish();
			} else {
				Intent returnIntent = new Intent();
				returnIntent.putExtra(ConstantKeys.DELETED, false);
				setResult(RESULT_OK, returnIntent);
				finish();
			}
		}

		if (requestCode == PICK_FROM_CAMERA) {
			doCrop();
		}

		if (requestCode == ID_GALLERY) {
			mFileCaptureUri = data.getData();
			doCrop();
		}

		if (requestCode == CROP_CAMERA_IMAGE) {
			Bitmap bmp = FileUtils.decodeSampledBitmapFromPath(
					FileUtils.getRealPathFromURI(mFileCaptureUri, this), 150,
					150);
			((ImageView) findViewById(R.id.group_image)).setImageBitmap(bmp);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	/* Test utilities */

	private GroupUpdatedListener groupUpdatedListener;

	public interface GroupUpdatedListener {
		void groupUpdated(String groupName);
	}

	public void setGroupUpdatedListener(GroupUpdatedListener l) {
		this.groupUpdatedListener = l;
	}

	private GroupDeletedGroupAdminListener groupDeletedListener;

	public interface GroupDeletedGroupAdminListener {
		void groupDeleted(GroupObject group);
	}

	public void setGroupDeletedListener(GroupDeletedGroupAdminListener l) {
		this.groupDeletedListener = l;
	}

	private AddUserGroupListener addUserGroupListener;

	public interface AddUserGroupListener {
		void addUserGroup();
	}

	public void setAddUserGroupListener(AddUserGroupListener l) {
		this.addUserGroupListener = l;
	}

	private RemoveGroupMemberListener removeGroupMemberListener;

	public interface RemoveGroupMemberListener {
		void userRemoved();
	}

	public void setRemoveGroupMemberListener(RemoveGroupMemberListener l) {
		this.removeGroupMemberListener = l;
	}

	private AddGroupAdminListener addGroupAdminListener;

	public interface AddGroupAdminListener {
		void adminAdded();
	}

	public void setAddGroupAdminListener(AddGroupAdminListener l) {
		this.addGroupAdminListener = l;
	}

	private RemoveGroupAdminListener removeGroupAdminListener;

	public interface RemoveGroupAdminListener {
		void adminRemoved();
	}

	public void setRemoveGroupAdminListener(RemoveGroupAdminListener l) {
		this.removeGroupAdminListener = l;
	}
}
