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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.GroupAdminActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupMemberObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;

public class GroupAdminUsersFragmentListAdapter extends BaseAdapter implements
		OnClickListener {

	public static final int ADD_ADMIN = 100;
	public static final int REMOVE_MEMBER = 101;

	private Context mContext;
	private ArrayList<GroupMemberObject> mList = new ArrayList<GroupMemberObject>();
	private ActionMode mActionMode = null;
	private Animation myFadeInAnimation;
	private GroupMemberObject itemSelected = null;
	public ProgressDialog pd = null;
	private ImageDownloader downloader = null;
	private boolean isAdmin = false;

	private Menu mMenu;
	private ActionMode mMode;

	public GroupAdminUsersFragmentListAdapter(Context context,
			ArrayList<GroupMemberObject> list, boolean isAdmin) {
		mList = list;
		mContext = context;
		myFadeInAnimation = AnimationUtils.loadAnimation(
				mContext.getApplicationContext(), R.anim.tween);
		this.isAdmin = isAdmin;
		downloader = new ImageDownloader();
	}

	private class ViewHolder {
		private ImageView image;
		private TextView name;
		private TextView userStatus;
	}

	public void refreshList(ArrayList<GroupMemberObject> list) {
		mList = list;
	}

	@Override
	public int getCount() {
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;
		if (convertView == null) {
			view = ((Activity) mContext).getLayoutInflater().inflate(
					R.layout.user_item_list, null);

			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.user_name);
			holder.image = (ImageView) view.findViewById(R.id.user_image);
			holder.userStatus = (TextView) view.findViewById(R.id.user_status);

			view.setTag(holder);
		} else
			holder = (ViewHolder) view.getTag();

		view.clearAnimation();
		view.setBackgroundColor(mList.get(position).getBackgroundColor());
		if (mList.get(position).isAnimated()) {
			view.startAnimation(myFadeInAnimation);
		}

		try {
			downloader.downloadUserAvatar(mContext.getApplicationContext(),
					holder.image, mList.get(position).getUser().getId()
							.toString(), mList.get(position).getUser()
							.getPicture().toString());
		} catch (Exception e) {
			Log.d(mContext.getPackageName(), "no avatar find");
		}

		holder.name.setTag(position);
		holder.name.setText(mList.get(position).getUser().getName());

		holder.userStatus.setText(mList.get(position).getAdminLayer());

		view.setOnClickListener(this);
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (!isAdmin) {
					return false;
				}

				if (mActionMode != null) {
					return false;
				}

				final ViewHolder holder = (ViewHolder) v.getTag();

				int position = (Integer) holder.name.getTag();
				itemSelected = mList.get(position);
				mList.get(position).setBackgroundColor(Color.GRAY);
				v.setBackgroundColor(Color.GRAY);
				v.startAnimation(myFadeInAnimation);
				mActionMode = ((GroupAdminActivity) mContext)
						.startActionMode(mActionModeCallback);
				v.setSelected(true);

				setItems(itemSelected);

				return true;
			}

		});

		return view;
	}

	@Override
	public void onClick(View v) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false", mContext);
		ViewHolder holder = (ViewHolder) v.getTag();
		int mPosition = (Integer) holder.name.getTag();
		// Check if contextual menu is activated
		if (mActionMode != null) {
			deSelect();
			v.setBackgroundColor(Color.GRAY);
			v.startAnimation(myFadeInAnimation);
			itemSelected = mList.get(mPosition);
			mList.get(mPosition).setBackgroundColor(Color.GRAY);
			v.setSelected(true);

			setItems(itemSelected);

			return;
		}
	}

	public void setItems(GroupMemberObject item) {
		mMenu.clear();
		MenuInflater inflater = mMode.getMenuInflater();
		if (itemSelected.isAdmin()) {
			inflater.inflate(R.menu.group_admin_contextual_menu_for_admins,
					mMenu);
		} else {
			inflater.inflate(R.menu.group_admin_contextual_menu_no_admins,
					mMenu);
		}

	}

	public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			mMenu = menu;
			mMode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.group_admin_contextual_menu_for_admins,
					mMenu);
			return true;
		}

		// Called each time the action mode is shown. Always called after
		// onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			int itemId = item.getItemId();
			if (itemId == R.id.delete_user) {
				deleteFromList();
				sendExitComand(itemSelected, REMOVE_MEMBER);
				mode.finish();
				return true;
			} else if (itemId == R.id.add_admin) {
				sendExitComand(itemSelected, ADD_ADMIN);
				mode.finish();
				return true;
			}

			return false;
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			deSelect();
			mActionMode = null;
		}
	};

	private void deSelect() {
		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).getUser().getId().toString()
					.equals(itemSelected.getUser().getId().toString())) {
				try {
					View v = ((GroupAdminActivity) mContext).usersListView
							.getChildAt(i
									- ((GroupAdminActivity) mContext).usersListView
											.getFirstVisiblePosition());
					v.setBackgroundColor(Color.TRANSPARENT);
					v.clearAnimation();
				} catch (Exception e) {
					// The item doesn't on screen
				}
				mList.get(i).setBackgroundColor(Color.TRANSPARENT);
			}
		}
		itemSelected = null;
	}

	private void deleteFromList() {
		DataBasesAccess.getInstance(mContext.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.CANCEL_ADD_PROCESS,
						itemSelected, null);

		DataBasesAccess.getInstance(mContext.getApplicationContext())
				.GroupMembersDataBase(DataBasesAccess.DELETE_PROCESS,
						itemSelected, null);

		for (int i = 0; i < mList.size(); i++) {
			if (mList.get(i).getUser().getId()
					.equals(itemSelected.getUser().getId())) {
				mList.remove(i);
				break;
			}
		}

		((GroupAdminActivity) mContext).usersAdapter.notifyDataSetInvalidated();
		((GroupAdminActivity) mContext).usersAdapter.notifyDataSetChanged();
	}

	private void sendExitComand(final GroupMemberObject actionItem,
			final int commandType) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				CommandStoreService cs = new CommandStoreService(mContext);
				try {
					JSONObject jsonToSend = new JSONObject();
					JSONObject party = new JSONObject();
					JSONObject user = new JSONObject();
					party.put(JsonKeys.ID, actionItem.getGroup().getId());
					user.put(JsonKeys.ID, actionItem.getUser().getId());

					jsonToSend.put(JsonKeys.GROUP, party);
					jsonToSend.put(JsonKeys.USER, user);

					if (commandType == ADD_ADMIN) {
						String commandToSend;
						if (actionItem.isAdmin()) {
							commandToSend = Command.METHOD_REMOVE_GROUP_ADMIN;
						} else {
							commandToSend = Command.METHOD_ADD_GROUP_ADMIN;
						}

						if (cs.createCommand(jsonToSend.toString(),
								commandToSend, null)) {
							return ConstantKeys.SENDING_OK;
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}
					} else if (commandType == REMOVE_MEMBER) {
						if (cs.createCommand(jsonToSend.toString(),
								Command.METHOD_REMOVE_GROUP_MEMBER, null)) {
							return ConstantKeys.SENDING_OK;
						} else {
							return ConstantKeys.SENDING_OFFLINE;
						}
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
					Log.d(mContext.getPackageName(), "Command send to database");
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(
							mContext.getApplicationContext(),
							mContext.getApplicationContext().getText(
									R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					if (commandType == ADD_ADMIN) {
						if (actionItem.isAdmin()) {
							Toast.makeText(
									mContext.getApplicationContext(),
									mContext.getApplicationContext().getText(
											R.string.group_remove_admin_error),
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(
									mContext.getApplicationContext(),
									mContext.getApplicationContext().getText(
											R.string.group_admin_error),
									Toast.LENGTH_SHORT).show();
						}
					} else if (commandType == REMOVE_MEMBER) {
						Toast.makeText(
								mContext.getApplicationContext(),
								mContext.getApplicationContext().getText(
										R.string.delete_user_error),
								Toast.LENGTH_SHORT).show();
						DataBasesAccess.getInstance(
								mContext.getApplicationContext())
								.GroupMembersDataBase(
										DataBasesAccess.CANCEL_DELETE_PROCESS,
										itemSelected, null);
					}

				}
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
			Toast.makeText(
					mContext.getApplicationContext(),
					mContext.getApplicationContext()
							.getText(R.string.auth_fail), Toast.LENGTH_SHORT)
					.show();
		}
	};
}
