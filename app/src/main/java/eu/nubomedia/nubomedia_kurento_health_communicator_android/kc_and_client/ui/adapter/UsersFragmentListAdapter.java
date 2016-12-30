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
import java.util.List;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.ItemListActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.ActionItem;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.util.QuickAction;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
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

import com.kurento.agenda.datamodel.pojo.Timeline.State;

public class UsersFragmentListAdapter extends BaseAdapter implements SectionIndexer, Filterable {

	private static final Logger log = LoggerFactory
			.getLogger(UsersFragmentListAdapter.class.getSimpleName());

	private ItemListActivity activityParent;
	private Context appContext;
	private ArrayList<UserObject> mList = new ArrayList<UserObject>();
	private ArrayList<UserObject> mListOriginal = new ArrayList<UserObject>();
	private Animation myFadeInAnimation;
	private ActionMode mActionMode = null;
	private QuickAction mQuickAction;
	private Filter userFilter;

	private boolean isVideoCallActive = true;
	private boolean isCallActive = true;
	private boolean isActionModeActive = true;

	private static final int ID_CALL = 1;
	private static final int ID_VIDEO_CALL = 2;

	public UsersFragmentListAdapter(ItemListActivity parent,
			ArrayList<UserObject> list) {
		mList = list;
		mListOriginal = mList;
		activityParent = parent;
		appContext = activityParent.getApplicationContext();
		myFadeInAnimation = AnimationUtils.loadAnimation(appContext,
				R.anim.tween);

		loadConfiguration();

		configurePopup();
	}

	private class ViewHolder {
		private ImageView image;
		private TextView name;
		private TextView userStatus;
	}

	public void refreshList(ArrayList<UserObject> list) {
		mList = list;
		notifyDataSetChanged();
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

	public void deselect() {
		if (userItemManager != null) {
			userItemManager.deselect();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;
		if (convertView == null) {
			view = activityParent.getLayoutInflater().inflate(
					R.layout.user_item_list, null);

			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.user_name);
			holder.image = (ImageView) view.findViewById(R.id.user_image);
			holder.userStatus = (TextView) view.findViewById(R.id.user_status);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		final UserObject userItem = mList.get(position);

		view.clearAnimation();
		view.setBackgroundColor(userItem.getBackgroundColor());
		if (userItem.isAnimated()) {
			view.startAnimation(myFadeInAnimation);
		}

		try {
			ImageDownloader downloader = new ImageDownloader();
			downloader.downloadUserAvatar(appContext, holder.image, userItem
					.getId().toString(), userItem.getPicture().toString());
		} catch (Exception e) {
			log.warn("No contact avatar loaded", e);
		}

		holder.name.setTag(position);
		holder.name.setText(userItem.getName() + ConstantKeys.STRING_WHITE
				+ userItem.getSurname());
		holder.userStatus.setText(ConstantKeys.STRING_DEFAULT);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false",
						appContext);

				if (mActionMode != null) {
					userItemManager.select(userItem, v);

					return;
				}

				if (!userItem.getId().equals(ConstantKeys.STRING_CERO)) {
					addTimeline(userItem);
				}
			}
		});

		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(final View v) {
				if (mActionMode != null) {
					return false;
				}

				userItemManager.select(userItem, v);
				if (isActionModeActive)
					mActionMode = activityParent
							.startActionMode(userItemManager);
				else {
					new AsyncTask<Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							SystemClock.sleep(200);
							return null;
						}

						@Override
						protected void onPostExecute(Void result) {
							mQuickAction.show(v);
						}
					}.execute();
				}

				return true;
			}
		});

		return view;
	}

	private final UserItemManager userItemManager = new UserItemManager();

	private class UserItemManager implements ActionMode.Callback {

		private UserObject itemSelected;
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
		public boolean onActionItemClicked(ActionMode mode, final MenuItem item) {
			if (item.getItemId() == R.id.call_menu) {
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						SystemClock.sleep(200);
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						mQuickAction.show(view);
					}
				}.execute();

				return true;
			}

			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			deSelect();
			mActionMode = null;
		}

		public UserObject getUserSelected () {
			return itemSelected;
		}

		private void refreshMenu() {
			if (mode == null || menu == null || itemSelected == null) {
				return;
			}

			menu.clear();
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.configure_users_contextual_menu, menu);
		}

		public void select(UserObject item, View v) {
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
				View v = ItemListActivity.usersListView.getChildAt(i
						- ItemListActivity.usersListView
								.getFirstVisiblePosition());
				if (v != null) {
					v.setBackgroundColor(Color.TRANSPARENT);
					v.clearAnimation();
				}

				UserObject user = mList.get(i);
				user.setBackgroundColor(Color.TRANSPARENT);
			}
		}
	};

	private void addTimeline(UserObject userSelected) {
		DataBasesAccess dba = DataBasesAccess.getInstance(appContext);
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

		activityParent.moveToTimeline(addTimeline);
	}

	private void configurePopup() {
		ActionItem callItem = new ActionItem(ID_CALL, null, appContext
				.getResources().getDrawable(R.drawable.ic_action_call));
		ActionItem videoCallItem = new ActionItem(ID_VIDEO_CALL, null,
				appContext.getResources().getDrawable(
						R.drawable.ic_action_video));

		mQuickAction = new QuickAction(activityParent);

		if (isCallActive)
			mQuickAction.addActionItem(callItem);
		if (isVideoCallActive)
			mQuickAction.addActionItem(videoCallItem);

		// setup the action item click listener
		mQuickAction
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
		UserObject userSelected = userItemManager.getUserSelected();
		if (userSelected == null) {
			log.warn("User not set, cannot call");
			return;
		}

		String uri = ConstantKeys.CALL_PHONE + userSelected.getPhone().trim();

		Intent intent = new Intent(Intent.ACTION_CALL);
		intent.setData(Uri.parse(uri));
		activityParent.startActivity(intent);
	}

	private void callVideo() {
		UserObject userSelected = userItemManager.getUserSelected();
		if (userSelected == null) {
			log.warn("User not set, cannot call");
			return;
		}
	}

	private void loadConfiguration() {
		isVideoCallActive = AppUtils.getResource(appContext,
				R.bool.active_videocall_action, isVideoCallActive);
		isCallActive = AppUtils.getResource(appContext,
				R.bool.active_call_action, isCallActive);
		isActionModeActive = AppUtils.getResource(appContext,
				R.bool.active_action_mode, isActionModeActive);
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
		if (userFilter == null) {
			userFilter = new UserFilter();
		}

		return userFilter;
	}

	private class UserFilter extends Filter {
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
				List<UserObject> nUserList = new ArrayList<UserObject>();
				for (UserObject g : mList) {
					if (Normalizer.normalize(g.getName().toUpperCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").contains(
							Normalizer.normalize(constraint.toString().toUpperCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""))) {
						nUserList.add(g);
					}
				}

				results.values = nUserList;
				results.count = nUserList.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,FilterResults results) {
			mList = (ArrayList<UserObject>) results.values;
			notifyDataSetChanged();
		}

	}
}
