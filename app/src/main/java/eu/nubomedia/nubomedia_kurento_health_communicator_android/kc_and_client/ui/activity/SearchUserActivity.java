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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.SearchUserListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;

public class SearchUserActivity extends AnalyticsBaseActivity {

	public static Context mContext = null;
	public String userName = ConstantKeys.STRING_DEFAULT;

	public static ListView usersListView;
	public static SearchUserListAdapter usersAdapter = null;
	public GroupObject mGroup = null;

	private Account account;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_user);
		mContext = this;

		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(+ R.drawable.bg))));

		mGroup = (GroupObject) getIntent().getSerializableExtra(
				ConstantKeys.GROUP);

		setTitle(getString(R.string.invitate_user));
		getActionBar().setIcon(R.drawable.add_user_light);

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
				userName = am.getUserData(ac, JsonKeys.NAME) + " "
						+ am.getUserData(ac, JsonKeys.SURNAME);
				getActionBar().setSubtitle(userName);
			}
		} catch (Exception e) {
			Log.d(getApplicationContext().getPackageName(),
					"Error trying to get user data" + e.toString());
		}

		registerReceiver(mReceiverGCM, intentFilter);

		createList();
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiverGCM);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.add_user_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE, this);

		if (item.getItemId() == R.id.add_user) {
			sendAddComand(usersAdapter.getList());
		}

		return super.onOptionsItemSelected(item);
	}

	private void createList() {

		ArrayList<UserObject> allUsers = DataBasesAccess.getInstance(
				mContext.getApplicationContext()).UsersDataBase(
				DataBasesAccess.READ, null);

		ArrayList<GroupMemberObject> usersOnGroup = DataBasesAccess
				.getInstance(mContext.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.READ_SELECTED, null,
						Long.valueOf(mGroup.getGroupId()));

		ArrayList<UserObject> list = new ArrayList<UserObject>();
		for (int i = 0; i < allUsers.size(); i++) {
			boolean findIt = false;
			for (int j = 0; j < usersOnGroup.size(); j++) {
				if (allUsers.get(i).getId()
						.equals(usersOnGroup.get(j).getUser().getId())
						&& (!usersOnGroup.get(j).isToDeleted())) {
					findIt = true;
				}
			}
			if (!findIt) {
				list.add(allUsers.get(i));
			}
		}

		usersListView = (ListView) findViewById(R.id.user_list);

		usersAdapter = new SearchUserListAdapter(SearchUserActivity.this, list,
				mGroup);
		usersListView.setAdapter(usersAdapter);
	}

	private GroupMemberObject setNewGroupMember(final UserObject newMember) {
		GroupMemberObject ob = new GroupMemberObject();
		ob.getUser().setId(newMember.getId());
		ob.getUser().setName(newMember.getName());
		ob.getUser().setPicture(ConstantKeys.LONG_DEFAULT);
		ob.getUser().setSurname(ConstantKeys.STRING_CERO);
		ob.getGroup().setId(Long.valueOf(mGroup.getGroupId()));
		ob.getGroup().setName(mGroup.getName());
		ob.getGroup().setPicture(mGroup.getPicture());
		ob.setToAdd(true);
		ob.setToDeleted(false);
		ob.setAdmin(false);

		return ob;
	}

	private void sendAddComand(final ArrayList<UserObject> actionItem) {

		final ArrayList<GroupMemberObject> selectedList = new ArrayList<GroupMemberObject>();
		final ArrayList<GroupMemberObject> usersOnGroup = DataBasesAccess
				.getInstance(mContext.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.READ_SELECTED, null,
						Long.valueOf(mGroup.getGroupId()));

		for (int i = 0; i < actionItem.size(); i++) {
			if (actionItem.get(i).isChecked()) {
				GroupMemberObject ob = null;

				if (usersOnGroup.isEmpty()) {
					ob = setNewGroupMember(actionItem.get(i));

					DataBasesAccess.getInstance(getApplicationContext())
							.GroupMembersDataBase(DataBasesAccess.WRITE, ob,
									null);
				} else {
					for (int j = 0; j < usersOnGroup.size(); j++) {
						if (actionItem.get(i).getId()
								.equals(usersOnGroup.get(j).getUser().getId())) {
							ob = usersOnGroup.get(j);
							DataBasesAccess
									.getInstance(getApplicationContext())
									.GroupMembersDataBase(
											DataBasesAccess.CANCEL_DELETE_PROCESS,
											ob, null);

							DataBasesAccess
									.getInstance(getApplicationContext())
									.GroupMembersDataBase(
											DataBasesAccess.ADD_PROCESS, ob,
											null);
						} else {
							ob = setNewGroupMember(actionItem.get(i));

							DataBasesAccess
									.getInstance(getApplicationContext())
									.GroupMembersDataBase(
											DataBasesAccess.WRITE, ob, null);
						}
					}
				}

				selectedList.add(ob);
			}
		}

		if (selectedList.size() == 0) {
			Toast.makeText(
					mContext.getApplicationContext(),
					mContext.getApplicationContext().getText(
							R.string.adding_user_empty), Toast.LENGTH_SHORT)
					.show();
			return;
		}

		for (int i = 0; i < selectedList.size(); i++) {
			final int position = i;
			new AsyncTask<Void, Void, String>() {
				GroupMemberObject localItem = selectedList.get(position);

				@Override
				protected String doInBackground(Void... params) {
					CommandStoreService cs = new CommandStoreService(mContext);
					try {
						JSONObject jsonToSend = new JSONObject();
						JSONObject party = new JSONObject();
						JSONObject user = new JSONObject();
						party.put(JsonKeys.ID,
								Long.valueOf(mGroup.getGroupId()));
						user.put(JsonKeys.ID, localItem.getUser().getId());

						jsonToSend.put(JsonKeys.GROUP, party);
						jsonToSend.put(JsonKeys.USER, user);

						if (cs.createCommand(jsonToSend.toString(),
								Command.METHOD_ADD_GROUP_MEMBER, null)) {
							return ConstantKeys.SENDING_OK;
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}

					} catch (JSONException e) {
						Log.e(mContext.getPackageName(), e.getMessage());
						return ConstantKeys.SENDING_FAIL;
					}
				}

				@Override
				protected void onPostExecute(String result) {
					if (result.equals(ConstantKeys.SENDING_OK)) {
						Log.d(getApplicationContext().getPackageName(),
								"Command send to database");
					} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
						Toast.makeText(
								mContext.getApplicationContext(),
								mContext.getApplicationContext().getText(
										R.string.upload_offline),
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(
								mContext.getApplicationContext(),
								mContext.getApplicationContext().getText(
										R.string.adding_user_error),
								Toast.LENGTH_SHORT).show();

						DataBasesAccess.getInstance(
								mContext.getApplicationContext())
								.GroupMembersDataBase(DataBasesAccess.DELETE,
										localItem, null);
					}
				}

			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
		((Activity) mContext).finish();

	}

	private void CreateGroupDeletedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				SearchUserActivity.this);
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
				SearchUserActivity.this);
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

	/* Broadcast receivers */
	private IntentFilter intentFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM);
	private BroadcastReceiver mReceiverGCM = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
				if (account != null) {
					AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
					userName = am.getUserData(account, JsonKeys.NAME) + " "
							+ am.getUserData(account, JsonKeys.SURNAME);
					getActionBar().setSubtitle(userName);
				}
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

			if (intent.getExtras().getBoolean(
					Command.METHOD_ADD_GROUP_MEMBER)) {
				createList();

				if (addUserGroupListener != null) {
					addUserGroupListener.addUserGroup();
				}
				addUserGroupListener = null;

				if (removeGroupMemberListener != null) {
					removeGroupMemberListener.userRemoved();
				}
				removeGroupMemberListener = null;
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
					}
				}
			}
		}
	};

	/* Test utilities */

	private GroupDeletedSearchUserListener groupDeletedListener;

	public interface GroupDeletedSearchUserListener {
		void groupDeleted(GroupObject group);
	}

	public void setGroupDeletedListener(GroupDeletedSearchUserListener l) {
		this.groupDeletedListener = l;
	}

	private AddUserGroupSearchUserListener addUserGroupListener;

	public interface AddUserGroupSearchUserListener {
		void addUserGroup();
	}

	public void setAddUserGroupListener(AddUserGroupSearchUserListener l) {
		this.addUserGroupListener = l;
	}

	private RemoveGroupMemberSearchUserListener removeGroupMemberListener;

	public interface RemoveGroupMemberSearchUserListener {
		void userRemoved();
	}

	public void setRemoveGroupMemberListener(
			RemoveGroupMemberSearchUserListener l) {
		this.removeGroupMemberListener = l;
	}
}
