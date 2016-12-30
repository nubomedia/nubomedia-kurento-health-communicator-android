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

import java.text.Normalizer;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.ItemListActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;

public class GroupsFragmentListAdapter extends BaseAdapter implements SectionIndexer, Filterable {

	private static final Logger log = LoggerFactory
			.getLogger(ItemListActivity.class.getSimpleName());

	public static final int DELETE_GROUP = 100;
	public static final int EXIT_GROUP = 101;

	private ItemListActivity activityParent;
	private Context appContext;
	private ArrayList<GroupObject> mList = new ArrayList<GroupObject>();
	private ArrayList<GroupObject> mListOriginal = new ArrayList<GroupObject>();
	private ActionMode mActionMode = null;
	private Animation myFadeInAnimation;
	private boolean isActiveGroupEditMenu = true;
	private Filter groupFilter;

	public GroupsFragmentListAdapter(ItemListActivity parent,
			ArrayList<GroupObject> list) {
		mList = list;
		mListOriginal = mList;
		activityParent = parent;
		appContext = activityParent.getApplicationContext();
		myFadeInAnimation = AnimationUtils.loadAnimation(appContext,
				R.anim.tween);
		isActiveGroupEditMenu = AppUtils.getResource(appContext,
				R.bool.active_group_edit_menu, isActiveGroupEditMenu);
	}

	private class ViewHolder {
		private ImageView image;
		private TextView name;
		private TextView status;
	}

	public void refreshList(ArrayList<GroupObject> list) {
		mList = list;
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if (mList == null)
			return 0;
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

	public void deselect() {
		if (groupItemManager != null) {
			groupItemManager.deselect();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;
		if (convertView == null) {
			view = activityParent.getLayoutInflater().inflate(
					R.layout.item_list_main_item, null);

			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.main_name);
			holder.image = (ImageView) view.findViewById(R.id.main_image);
			holder.status = (TextView) view.findViewById(R.id.main_status);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		final GroupObject groupItem = mList.get(position);

		view.clearAnimation();
		view.setBackgroundColor(groupItem.getBackgroundColor());

		try {
			ImageDownloader downloader = new ImageDownloader();
			downloader.downloadGroupAvatar(appContext, groupItem.getGroupId()
					.toString(), groupItem.getPicture().toString(),
					holder.image);
		} catch (Exception e) {
			log.warn("No group avatar loaded");
		}

		holder.name.setText(groupItem.getName());
		holder.status.setText(groupItem.getStatusToPain());

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false",
						appContext);
				if (groupItem.getGroupId().equals(ConstantKeys.STRING_CERO)) {
					return;
				}

				if (mActionMode != null) {
					groupItemManager.select(groupItem, v);

					return;
				}

				addTimeline(groupItem);
			}
		});

		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mActionMode != null) {
					return false;
				}

				if (!isActiveGroupEditMenu)
					return false;

				if (groupItem.getGroupId().equals(ConstantKeys.STRING_CERO)) {
					return false;
				}

				groupItemManager.select(groupItem, v);
				mActionMode = activityParent.startActionMode(groupItemManager);

				return true;
			}
		});

		return view;
	}

	private final GroupItemManager groupItemManager = new GroupItemManager();

	private class GroupItemManager implements ActionMode.Callback {

		private GroupObject itemSelected;
		private View view;

		private ActionMode mode;
		private Menu menu;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			this.mode = mode;
			this.menu = menu;
			refreshMenu();

			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			int itemId = item.getItemId();
			if (itemId == R.id.delete_group) {
				if (itemSelected.isAdmin()) {
					deleteFromList();
					sendItemComand(itemSelected, DELETE_GROUP);
					mode.finish();
				} else {
					Toast.makeText(appContext,
							appContext.getText(R.string.group_cant_delete),
							Toast.LENGTH_SHORT).show();
				}
				return true;
			} else if (itemId == R.id.exit_group) {
				if (itemSelected.getCanLeave()) {
					deleteFromList();
					sendItemComand(itemSelected, EXIT_GROUP);
					mode.finish();
				} else {
					Toast.makeText(appContext,
							appContext.getText(R.string.group_cant_leave),
							Toast.LENGTH_SHORT).show();
				}
				return true;
			}

			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			deSelect();
			mActionMode = null;
		}

		private void refreshMenu() {
			if (mode == null || menu == null || itemSelected == null) {
				return;
			}

			menu.clear();
			MenuInflater inflater = mode.getMenuInflater();
			if (itemSelected.isAdmin()) {
				inflater.inflate(R.menu.configure_groups_contextual_admin_menu,
						menu);
			} else {
				inflater.inflate(
						R.menu.configure_groups_contextual_no_admin_menu, menu);
			}
		}

		public void select(GroupObject item, View v) {
			this.itemSelected = item;
			this.view = v;
			select();
		}

		public void deselect() {
			if (mode != null) {
				mode.finish();
			}
		}

		public void select() {
			deSelect();

			view.setSelected(true);
			itemSelected.setBackgroundColor(Color.GRAY);
			view.setBackgroundColor(Color.GRAY);
			view.startAnimation(myFadeInAnimation);
			refreshMenu();
		}

		private void deSelect() {
			for (int i = 0; i < mList.size(); i++) {
				View v = ItemListActivity.groupsListView.getChildAt(i
						- ItemListActivity.groupsListView
								.getFirstVisiblePosition());
				if (v != null) {
					v.setBackgroundColor(Color.TRANSPARENT);
					v.clearAnimation();
				}

				GroupObject group = mList.get(i);
				group.setBackgroundColor(Color.TRANSPARENT);
			}
		}

		private void deleteFromList() {
			DataBasesAccess.getInstance(appContext).GroupsDataBase(
					DataBasesAccess.DELETE_PROCESS, itemSelected);
			DataBasesAccess.getInstance(appContext).TimelinesDataBaseSetState(
					Long.valueOf(itemSelected.getGroupId()), State.DISABLED);

			for (int i = 0; i < mList.size(); i++) {
				if (mList.get(i).getGroupId().equals(itemSelected.getGroupId())) {
					mList.remove(i);
				}
			}

			notifyDataSetInvalidated();
			notifyDataSetChanged();
		}
	};

	private void sendItemComand(final GroupObject actionItem,
			final int commandType) {
		new AsyncTask<Void, Void, String>() {
			private GroupObject localItem = actionItem;

			@Override
			protected String doInBackground(Void... params) {
				// Let's put the timeline to DISABLE status
				DataBasesAccess.getInstance(appContext)
						.TimelinesDataBaseSetState(
								Long.valueOf(localItem.getGroupId()), State.DISABLED);

				CommandStoreService cs = new CommandStoreService(appContext);
				try {
					if (commandType == EXIT_GROUP) {
						Account account = AccountUtils.getAccount(appContext,
								true);
						AccountManager am = (AccountManager) appContext
								.getSystemService(Context.ACCOUNT_SERVICE);

						JSONObject jsonToSend = new JSONObject();
						JSONObject party = new JSONObject();
						JSONObject user = new JSONObject();
						party.put(JsonKeys.ID, localItem.getGroupId());
						user.put(JsonKeys.ID,
								am.getUserData(account, JsonKeys.ID_STORED));

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
					log.debug("Command send to database");
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(appContext,
							appContext.getText(R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					if (commandType == EXIT_GROUP) {
						Toast.makeText(appContext,
								appContext.getText(R.string.group_exit_error),
								Toast.LENGTH_SHORT).show();
					} else if (commandType == DELETE_GROUP) {
						Toast.makeText(
								appContext,
								appContext.getText(R.string.group_delete_error),
								Toast.LENGTH_SHORT).show();
					}

					// Lets restore all the data
					DataBasesAccess.getInstance(appContext)
							.TimelinesDataBaseSetState(
									Long.valueOf(localItem.getGroupId()), State.ENABLED);
					DataBasesAccess.getInstance(appContext).GroupsDataBase(
							DataBasesAccess.CANCEL_DELETE_PROCESS, localItem);
					notifyDataSetInvalidated();
					notifyDataSetChanged();
				}
			}
		}.execute();
	}

	private void addTimeline(GroupObject groupSelected) {
		DataBasesAccess dba = DataBasesAccess.getInstance(appContext);
		boolean recoverTimeline = dba.TimelinesDataBaseIsRecoverTimeline(Long
				.valueOf(groupSelected.getGroupId()));

		TimelineObject addTimeline;
		if (!recoverTimeline) {
			addTimeline = new TimelineObject();
			addTimeline.getParty().setType(ConstantKeys.GROUP);
			addTimeline.getParty().setName(groupSelected.getName());
			addTimeline.setId(ConstantKeys.LONG_DEFAULT);
			addTimeline.setLastMessageId(ConstantKeys.LONG_DEFAULT);
			addTimeline.setLastMessageBody(ConstantKeys.STRING_DEFAULT);
			addTimeline.setNewMessages(false);
			addTimeline.setOwnerId(ConstantKeys.LONG_DEFAULT);
			addTimeline.getParty().setId(
					Long.valueOf(groupSelected.getGroupId()));
			addTimeline.setShowIt(false);
			addTimeline.setState(State.ENABLED);

			dba.TimelinesDataBaseWrite(addTimeline);
		} else {
			// recover the real timeline for the list
			addTimeline = dba.TimelinesDataBaseReadPartyIdSelected(Long
					.valueOf(groupSelected.getGroupId()));
		}

		activityParent.moveToTimeline(addTimeline);
	}

	/* Section indexer */
	@Override
	public Object[] getSections() {
		String[] sectionsArr = new String[mList.size()];
		for (int i=0; i < mList.size(); i++) {
			sectionsArr[i] = mList.get(i).getName().substring(0, 1).toUpperCase();
		}

		return sectionsArr;
	}

	@Override
	public int getPositionForSection(int i) {
		return i;
	}

	@Override
	public int getSectionForPosition(int i) {
		return 0;
	}

	/* Filterable */
	@Override
	public Filter getFilter() {
		if (groupFilter == null) {
			groupFilter = new GroupFilter();
		}

		return groupFilter;
	}

	private class GroupFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			mList = mListOriginal;
			FilterResults results = new FilterResults();
			// Implement here the filter logic
			if (constraint == null || constraint.length() == 0) {
				// No filter implemented we return all the list
				results.values = mList;
				results.count = mList.size();
			} else {
				// Perform filtering operation
				List<GroupObject> nGroupList = new ArrayList<GroupObject>();
				for (GroupObject g : mList) {
					if (Normalizer.normalize(g.getName().toUpperCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").contains(
							Normalizer.normalize(constraint.toString().toUpperCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""))) {
						nGroupList.add(g);
					}
				}

				results.values = nGroupList;
				results.count = nGroupList.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,FilterResults results) {
			mList = (ArrayList<GroupObject>) results.values;
			notifyDataSetChanged();
		}

	}
}
