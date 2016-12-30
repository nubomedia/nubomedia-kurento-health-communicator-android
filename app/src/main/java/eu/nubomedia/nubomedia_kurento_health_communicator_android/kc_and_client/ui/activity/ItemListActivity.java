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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.GroupsFragmentListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.UsersFragmentListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.ActionItem;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.QuickAction;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.*;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;

public class ItemListActivity extends FragmentActivity implements
		ActionBar.TabListener {

	private static final Logger log = LoggerFactory
			.getLogger(ItemListActivity.class.getSimpleName());

	public static ItemListActivity self = null;
	public static ArrayList<GroupObject> groups = null;
	public static ArrayList<UserObject> users = null;

	private ViewPager mViewPager;
	private View userTab = null;
	private View groupTab = null;

	public static ListView usersListView;
	public static UsersFragmentListAdapter usersAdapter = null;
	public static ListView groupsListView;
	public static GroupsFragmentListAdapter groupsAdapter = null;

	// Popup values
	private static final int ID_CAMERA = 1;
	private static final int ID_GALLERY = 2;
	private static final int CROP_CAMERA_IMAGE = 100;
	private static final int PICK_FROM_CAMERA = 101;

	private QuickAction mQuickAction;
	private InputMethodManager imm;
	private Uri mFileCaptureUri;
	private String tempFilePath;
	private LinearLayout dialog;
	private RelativeLayout dialogBackground;
	private Animation myAnimationShowPopup;

	private Menu mMenu = null;
	private Account account;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_list_activity);
		self = this;

		getActionBar().setIcon(R.drawable.ic_group_light);
		setTitle(getString(R.string.agenda_menu));

		AppSectionsPagerAdapter mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(
				getSupportFragmentManager());
		userTab = findViewById(R.id.users_tab_select);
		groupTab = findViewById(R.id.groups_tab_select);

		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						if (position == 1) {
							mMenu.clear();

							MenuInflater inflater = getMenuInflater();
							inflater.inflate(R.menu.configure_users_menu, mMenu);

							if (groupsAdapter != null) {
								groupsAdapter.deselect();
							}

							userTab.setVisibility(View.VISIBLE);
							groupTab.setVisibility(View.GONE);
						} else if (position == 0) {
							mMenu.clear();

							MenuInflater inflater = getMenuInflater();
							inflater.inflate(R.menu.configure_groups_menu,
									mMenu);

							if (usersAdapter != null) {
								usersAdapter.deselect();
							}

							userTab.setVisibility(View.GONE);
							groupTab.setVisibility(View.VISIBLE);
						}
					}
				});

		((RelativeLayout) findViewById(R.id.users_tab))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mViewPager.setCurrentItem(1, true);
					}
				});

		((RelativeLayout) findViewById(R.id.groups_tab))
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mViewPager.setCurrentItem(0, true);
					}
				});

		// configuring popup features
		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		dialog = (LinearLayout) findViewById(R.id.includer);
		dialogBackground = (RelativeLayout) findViewById(R.id.dialog_background);
		configurePopup();

		myAnimationShowPopup = AnimationUtils.loadAnimation(
				getApplicationContext(), R.anim.slide_down);
		myAnimationShowPopup.setDuration(200);

		buildUserActionsPopup();
	}

	@Override
	protected void onResume() {
		super.onResume();

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
				String userName = am.getUserData(ac, JsonKeys.NAME) + " "
						+ am.getUserData(ac, JsonKeys.SURNAME);
				getActionBar().setSubtitle(userName);
			}
		} catch (Exception e) {
			Log.d("Error trying to get user data", e.toString());
		}

		// registering our receiver
		registerReceiver(mGCMReceiver, gcmFilter);
		refreshList();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mViewPager.getCurrentItem() == 0) {
			groupsAdapter.deselect();
		} else if (mViewPager.getCurrentItem() == 1) {
			usersAdapter.deselect();
		}

		unregisterReceiver(mGCMReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.configure_groups_menu, menu);
		mMenu = menu;

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false", this);

		int itemId = item.getItemId();
		if (itemId == R.id.add_group) {
			if (Preferences.isGroupAutoRegister(this)) {
				showPopup();
			} else {
				Toast.makeText(this,
						getString(R.string.create_group_access_deny),
						Toast.LENGTH_SHORT).show();
			}
		} else if (itemId == R.id.sync_contacts) {
			sendContactsRequest();
		} else if (itemId == R.id.search) {
			SearchView searchView = (SearchView) mMenu.findItem(R.id.search).getActionView();
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String s) {
					return false;
				}

				@Override
				public boolean onQueryTextChange(String s) {
					if (mViewPager.getCurrentItem() == 0) {
						groupsAdapter.getFilter().filter(s);
					} else if (mViewPager.getCurrentItem() == 1) {
						usersAdapter.getFilter().filter(s);
					}

					return false;
				}
			});
		} else if (itemId == R.id.main_menu_select_account) {
			Intent editUserIntent = new Intent(this, EditUserActivity.class);
			startActivity(editUserIntent);
		} else if (itemId == R.id.main_menu_preferences) {
			Intent preferencesIntent = new Intent(this, Preferences.class);
			startActivityForResult(preferencesIntent, AppUtils.RETURN_SETUP);
		}

		return super.onOptionsItemSelected(item);
	}

	private void sendContactsRequest() {
		Toast.makeText(this, getString(R.string.synchronizing_contacts),
				Toast.LENGTH_SHORT).show();

		AccountUtils.requestContacts(getApplicationContext());
	}

	private void createGroup() {
		new AsyncTask<Void, Void, Boolean>() {

			private Long localId;
			private GroupObject groupObject = null;
			private String name;
			private String phone;

			@Override
			protected void onPreExecute() {
				name = ((EditText) ItemListActivity.this
						.findViewById(R.id.group_name_text)).getText()
						.toString();
				phone = ((EditText) ItemListActivity.this
						.findViewById(R.id.group_phone_text)).getText()
						.toString();

				localId = Long.valueOf(System.currentTimeMillis());
				groupObject = new GroupObject();

				groupObject.setGroupId(ConstantKeys.STRING_CERO);
				groupObject.setLocalId(localId);
				groupObject.setName(name);
				groupObject.setCanRead(false);
				groupObject.setCanSend(false);
				groupObject.setCanLeave(false);
				groupObject.setIsAdmin(false);
				groupObject.setPhone(phone);
				groupObject.setPicture(ConstantKeys.LONG_DEFAULT);

				DataBasesAccess.getInstance(getApplicationContext())
						.GroupsDataBase(DataBasesAccess.WRITE, groupObject);

				refreshList();
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				String path = null;
				if (mFileCaptureUri != null) {
					path = FileUtils.getRealPathFromURI(mFileCaptureUri,
							ItemListActivity.this);
				}

				GroupCreate group = new GroupCreate();
				group.setLocalId(localId);
				group.setName(name);
				group.setPhone(phone);

				AccountId accountId = new AccountId();
				accountId.setId(Long.valueOf(Preferences
						.getAccountId(getApplicationContext())));
				// TODO we need to check if there are media to send

				return AppUtils.createGroup(ItemListActivity.this, path, group,
						accountId, true);
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getText(
									R.string.creating_group),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ItemListActivity.this,
							getString(R.string.creating_group_error),
							Toast.LENGTH_SHORT).show();

					// Delete the local group
					DataBasesAccess.getInstance(getApplicationContext())
							.GroupsDataBase(DataBasesAccess.DELETE_LOCAL,
									groupObject);
				}

				FileUtils.deleteTemp(tempFilePath);
				clearPopup();
			}
		}.execute();
	}

	public void clearPopup() {
		((EditText) (ItemListActivity.this).findViewById(R.id.group_name_text))
				.setText("");
		((EditText) (ItemListActivity.this).findViewById(R.id.group_phone_text))
				.setText("");
		((ImageView) dialog.findViewById(R.id.group_image))
				.setImageBitmap(null);
		mFileCaptureUri = null;
	}

	private void showPopup() {
		dialog.setVisibility(View.VISIBLE);
		dialogBackground.setVisibility(View.VISIBLE);

		dialog.startAnimation(myAnimationShowPopup);

		Button create_group = (Button) dialog.findViewById(R.id.create_button);
		create_group.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((EditText) ItemListActivity.this
						.findViewById(R.id.group_name_text)).getText()
						.toString().length() == 0) {
					Toast.makeText(ItemListActivity.this,
							getString(R.string.creating_group_mandatory_name),
							Toast.LENGTH_SHORT).show();
					return;
				}
				dialog.setVisibility(View.GONE);
				dialogBackground.setVisibility(View.GONE);

				imm.hideSoftInputFromWindow(((EditText) ItemListActivity.this
						.findViewById(R.id.group_name_text)).getWindowToken(),
						0);
				imm.hideSoftInputFromWindow(((EditText) ItemListActivity.this
						.findViewById(R.id.group_phone_text)).getWindowToken(),
						0);

				createGroup();
			}
		});

		Button cancel_group = (Button) dialog.findViewById(R.id.cancel_button);
		cancel_group.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				imm.hideSoftInputFromWindow(((EditText) ItemListActivity.this
						.findViewById(R.id.group_name_text)).getWindowToken(),
						0);
				imm.hideSoftInputFromWindow(((EditText) ItemListActivity.this
						.findViewById(R.id.group_phone_text)).getWindowToken(),
						0);

				dialog.setVisibility(View.GONE);
				dialogBackground.setVisibility(View.GONE);
				clearPopup();
			}
		});

		ImageView image = (ImageView) dialog.findViewById(R.id.group_image);
		image.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				imm.hideSoftInputFromWindow(((EditText) dialog
						.findViewById(R.id.group_name_text)).getWindowToken(),
						0);
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						SystemClock.sleep(200);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mQuickAction.show(((ImageView) dialog
								.findViewById(R.id.group_image)));
					}
				}.execute();
			}
		});

		dialogBackground.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.setVisibility(View.GONE);
				dialogBackground.setVisibility(View.GONE);
			}
		});
	}

	public void takeGallery() {
		Intent photoLibraryIntent = new Intent(Intent.ACTION_PICK,
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		photoLibraryIntent.setType("image/*");
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
			log.warn("Cannot launch any app to take picture", e);
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			mFileCaptureUri = null;
			return;
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
			((ImageView) dialog.findViewById(R.id.group_image))
					.setImageBitmap(bmp);
		}
	}

	private void refreshList() {
		groups = DataBasesAccess.getInstance(getApplicationContext())
				.GroupsDataBase(DataBasesAccess.READ, null);
		ArrayList<GroupObject> aux = new ArrayList<GroupObject>();
		for (GroupObject g : groups) {
			if (!g.isToDelete()) {
				if (ConstantKeys.STRING_CERO.equals(g.getGroupId())) {
					g.setStatusToPain(getString(R.string.creating_group));
				}
				aux.add(g);
			}
		}

		groups = aux;
		if (groupsAdapter != null) {
			groupsAdapter.refreshList(groups);
		}

		users = DataBasesAccess.getInstance(getApplicationContext())
				.UsersDataBase(DataBasesAccess.READ, null);

		if (usersAdapter != null) {
			usersAdapter.refreshList(users);
		}
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
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onBackPressed() {
		if (dialogBackground.getVisibility() == View.VISIBLE) {
			// do nothing
		} else {
			super.onBackPressed();
		}
	}

	private static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

		public AppSectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch (i) {
			case 0:
				return new GroupFragment();

			default:
				Fragment fragment = new UserFragment();
				Bundle args = new Bundle();
				args.putInt(UserFragment.ARG_SECTION_NUMBER, i + 1);
				fragment.setArguments(args);
				return fragment;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "Section " + (position + 1);
		}
	}

	/* Users management */

	private static PopupWindow userActionsPopup;
	private static UserObject userSelected;

	public static class UserFragment extends Fragment {
		public static final String ARG_SECTION_NUMBER = "section_number";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			final View rootView = inflater.inflate(
					R.layout.item_list_user_list, container, false);

			usersListView = (ListView) rootView
					.findViewById(R.id.users_list_view);

			usersAdapter = new UsersFragmentListAdapter(self, users);
			usersListView.setAdapter(usersAdapter);

			usersListView.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView absListView, int i) {
				}

				@Override
				public void onScroll(final AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					//TODO: Show and hide fast scroll

					if (visibleItemCount == totalItemCount) {
						absListView.setFastScrollAlwaysVisible(false);
						return;
					}
				}
			});

			usersListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, final View view,
						int position, long id) {
					UserObject userItem = users.get(position);
					AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false", self);

					if (!userItem.getId().equals(ConstantKeys.STRING_CERO)) {
						addTimeline(userItem);
					}
				}
			});
			usersListView
					.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> parent,
								final View view, int position, long id) {
							userSelected = users.get(position);
							showUserActionsPopup(rootView);
							return true;
						}
					});

			return rootView;
		}
	}

	private void buildUserActionsPopup() {
		ListView listView = new ListView(self);
		listView.setBackgroundColor(Color.WHITE);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				userActionsPopup.dismiss();
			}
		});

		userActionsPopup = new PopupWindow(listView,
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		userActionsPopup.setOutsideTouchable(true);
		userActionsPopup.setFocusable(true);
		userActionsPopup.setBackgroundDrawable(new BitmapDrawable());

		View v = userActionsPopup.getContentView();
		v.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					userActionsPopup.dismiss();
				}
				return false;
			}
		});
	}

	private static void showUserActionsPopup(View rootView) {
		userActionsPopup.showAtLocation(rootView, Gravity.CENTER, 0, 0);
	}

	private interface OnSelectedListener {
		void onSelected();
	}

	private static class Selectable {

		private String name;
		private OnSelectedListener listener;

		public Selectable(String name, OnSelectedListener listener) {
			this.name = name;
			this.listener = listener;
		}

		public void select() {
			listener.onSelected();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/* Groups management */
	public static class GroupFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.item_list_group_list,
					container, false);

			groupsListView = (ListView) rootView
					.findViewById(R.id.groups_list_view);

			groupsAdapter = new GroupsFragmentListAdapter(self, groups);
			groupsListView.setAdapter(groupsAdapter);

			groupsListView.setOnScrollListener(new AbsListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView absListView, int i) {
				}

				@Override
				public void onScroll(final AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					//TODO: Show and hide fast scroll

					if (visibleItemCount == totalItemCount) {
						absListView.setFastScrollAlwaysVisible(false);
						return;
					}
				}
			});

			return rootView;
		}
	}

	/* Broadcast receivers */
	private IntentFilter gcmFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM);
	private BroadcastReceiver mGCMReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_GROUP)) {
				refreshList();
				ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
						.getSerializableExtra(JsonKeys.COMMAND_TYPE_UPDATE_GROUP_ARRAY);
				for (GroupObject item : list) {
					if (groupCreatedListener != null) {
						groupCreatedListener.groupCreated(item);
					}
					groupCreatedListener = null;

					if (groupUpdatedListener != null) {
						groupUpdatedListener.groupUpdated(item.getName());
					}
					groupUpdatedListener = null;

					if (addGroupAdminListener != null) {
						addGroupAdminListener.adminAdded();
					}
					addGroupAdminListener = null;

					if (removeGroupAdminListener != null) {
						removeGroupAdminListener.adminRemoved();
					}
					removeGroupAdminListener = null;
				}
			} else if (intent.getExtras().getBoolean(
					Command.METHOD_DELETE_GROUP)) {
				refreshList();
				ArrayList<GroupObject> list = (ArrayList<GroupObject>) intent
						.getSerializableExtra(JsonKeys.COMMAND_TYPE_DELETE_GROUP_ARRAY);

				// TODO Here we will find if any group is on list and we will
				// delete it from local list.
				for (GroupObject g : list) {
					if (groupDeletedListener != null) {
						groupDeletedListener.groupDeleted(g);
					}
				}
			} else if (intent.getExtras().getBoolean(
					Command.METHOD_UPDATE_CONTACT)) {
				refreshList();
				if (userSynchroListener != null) {
					userSynchroListener.userSynchro();
				}
			}

			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
				if (account != null) {
					AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
					String userName = am.getUserData(account, JsonKeys.NAME)
							+ " " + am.getUserData(account, JsonKeys.SURNAME);
					getActionBar().setSubtitle(userName);
				}
			}
		}
	};

	/* Test utilities */

	private GroupCreatedListener groupCreatedListener;

	public interface GroupCreatedListener {
		void groupCreated(GroupObject group);
	}

	public void setGroupCreatedListener(GroupCreatedListener l) {
		this.groupCreatedListener = l;
	}

	private GroupUpdatedAgendaListener groupUpdatedListener;

	public interface GroupUpdatedAgendaListener {
		void groupUpdated(String groupName);
	}

	public void setGroupUpdatedListener(GroupUpdatedAgendaListener l) {
		this.groupUpdatedListener = l;
	}

	private GroupDeletedListener groupDeletedListener;

	public interface GroupDeletedListener {
		void groupDeleted(GroupObject group);
	}

	public void setGroupDeletedListener(GroupDeletedListener l) {
		this.groupDeletedListener = l;
	}

	public static String getGroupId(String groupName) {
		for (GroupObject g : groups) {
			if (g.getName().equals(groupName)) {
				return g.getGroupId();
			}
		}
		return null;
	}

	public static Long getUserId(String userName) {
		for (UserObject u : users) {
			if (u.getName().equals(userName)) {
				return u.getId();
			}
		}

		return null;
	}

	private UserSynchroListener userSynchroListener;

	public interface UserSynchroListener {
		void userSynchro();
	}

	public void setUserSynchroListener(UserSynchroListener l) {
		this.userSynchroListener = l;
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

	/* Timeline utilities */

	public void moveToTimeline(TimelineObject timeline) {
		Class<?> className = AppUtils.getClassByName(this,
				R.string.messages_activity_package);
		Intent i = new Intent();
		if (className != null)
			i.setClass(self, className);
		i.putExtra(ConstantKeys.TIMELINE, timeline);
		self.startActivity(i);
	}

	private static void addTimeline(UserObject userSelected) {
		DataBasesAccess dba = DataBasesAccess.getInstance(self);
		boolean recoverTimeline = dba.TimelinesDataBaseIsRecoverTimeline(Long
				.valueOf(userSelected.getId()));

		TimelineObject addTimeline;
		if (!recoverTimeline) {
			addTimeline = new TimelineObject();
			addTimeline.getParty().setType(ConstantKeys.USER);
			addTimeline.getParty().setName(
					new String(userSelected.getName()
							+ ConstantKeys.STRING_WHITE
							+ userSelected.getSurname()));
			addTimeline.setId(ConstantKeys.LONG_DEFAULT);
			addTimeline.setLastMessageId(ConstantKeys.LONG_DEFAULT);
			addTimeline.setLastMessageBody(ConstantKeys.STRING_DEFAULT);
			addTimeline.setNewMessages(false);
			addTimeline.setOwnerId(ConstantKeys.LONG_DEFAULT);
			addTimeline.getParty().setId(Long.valueOf(userSelected.getId()));
			addTimeline.setShowIt(false);
			addTimeline.setState(State.ENABLED);

			dba.TimelinesDataBaseWrite(addTimeline);
		} else {
			// recover the real timeline for the list
			addTimeline = dba.TimelinesDataBaseReadPartyIdSelected(Long
					.valueOf(userSelected.getId()));
		}

		self.moveToTimeline(addTimeline);
	}

}
