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

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.TimelineListActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
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
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;

public class TimelineListAdapter extends BaseAdapter implements OnClickListener {

	private static final Logger log = LoggerFactory
			.getLogger(TimelineListAdapter.class.getSimpleName());

	private ArrayList<TimelineObject> mList = new ArrayList<TimelineObject>();
	public TimelineObject itemSelected = null;
	private ActionMode mActionMode = null;
	private Animation myFadeInAnimation;
	private TimelineListActivity activityParent;
	private ListView listView;
	private Context appContext;

	public TimelineListAdapter(TimelineListActivity parent, ListView listView,
			ArrayList<TimelineObject> list) {
		this.activityParent = parent;
		this.listView = listView;
		appContext = activityParent.getApplicationContext();
		mList = list;
		myFadeInAnimation = AnimationUtils.loadAnimation(parent, R.anim.tween);
	}

	private class ViewHolder {
		private ImageView image;
		private TextView name;
		private TextView status;
		private TextView message;
		private RelativeLayout newMessageTagRight;
		private RelativeLayout newMessageTagLeft;
		private ImageView newMessageIcon;
	}

	public void refreshList(ArrayList<TimelineObject> list) {
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
			view = activityParent.getLayoutInflater().inflate(
					R.layout.timeline_item_list, null);

			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.timeline_name);
			holder.image = (ImageView) view
					.findViewById(R.id.timeline_type_image);
			holder.newMessageIcon = (ImageView) view
					.findViewById(R.id.timeline_new_icon);
			holder.status = (TextView) view.findViewById(R.id.timeline_status);
			holder.message = (TextView) view
					.findViewById(R.id.timeline_last_message_body);
			holder.newMessageTagRight = (RelativeLayout) view
					.findViewById(R.id.timeline_new_right);
			holder.newMessageTagLeft = (RelativeLayout) view
					.findViewById(R.id.timeline_new_left);

			view.setTag(holder);
		} else
			holder = (ViewHolder) view.getTag();

		final TimelineObject mObject = mList.get(position);

		view.clearAnimation();
		view.setBackgroundColor(mObject.getBackgroundColor());
		if (mObject.isAnimated()) {
			view.startAnimation(myFadeInAnimation);
		}

		holder.name.setTag(position);
		holder.name.setText(mObject.getParty().getName());
		holder.status.setText(mObject.getStatusToPaint());

		RelativeLayout.LayoutParams newIconParams = (RelativeLayout.LayoutParams) holder.newMessageIcon
				.getLayoutParams();
		newIconParams.width = mObject.getNewIconSize();
		newIconParams.height = mObject.getNewIconSize();
		holder.newMessageIcon.setLayoutParams(newIconParams);
		holder.message.setText(mObject.getLastMessageBody());

		holder.newMessageTagLeft.setBackgroundDrawable(new BitmapDrawable(
				activityParent.getApplicationContext().getResources(),
				BitmapFactory.decodeStream(activityParent
						.getApplicationContext().getResources()
						.openRawResource(mObject.getTagResourceId()))));

		holder.newMessageTagRight.setBackgroundDrawable(new BitmapDrawable(
				activityParent.getApplicationContext().getResources(),
				BitmapFactory.decodeStream(activityParent
						.getApplicationContext().getResources()
						.openRawResource(mObject.getTagResourceId()))));

		DataBasesAccess dba;
		dba = DataBasesAccess.getInstance(appContext);

		try {
			ImageDownloader downloader = new ImageDownloader();

			if (mObject.getParty().getType()
					.equalsIgnoreCase(ConstantKeys.USER)) {
				UserObject recoverUser = dba.getUserDataBase(mObject.getParty()
						.getId().toString());
				downloader.downloadUserAvatar(appContext, holder.image,
						recoverUser.getId().toString(), recoverUser
								.getPicture().toString());
			} else {
				GroupObject recoverGroup = dba.getGroupDataBase(mObject
						.getParty().getId().toString());
				downloader.downloadGroupAvatar(appContext, mObject.getParty()
						.getId().toString(), recoverGroup.getPicture()
						.toString(), holder.image);
			}
		} catch (Exception e) {
			log.warn("No message avatar loaded");

			if (mObject.getParty().getType()
					.equalsIgnoreCase(ConstantKeys.USER)) {
				holder.image.setImageResource(R.drawable.ic_profile);
			}
		}

		view.setOnClickListener(this);
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				if (mActionMode != null) {
					return false;
				}
				final ViewHolder holder = (ViewHolder) v.getTag();

				int position = (Integer) holder.name.getTag();
				itemSelected = mObject;
				mObject.setBackgroundColor(Color.GRAY);
				v.setBackgroundColor(Color.GRAY);
				v.startAnimation(myFadeInAnimation);
				mActionMode = activityParent
						.startActionMode(mActionModeCallback);
				v.setSelected(true);
				return true;
			}

		});

		return view;
	}

	@Override
	public void onClick(View v) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false", activityParent);
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
			return;
		}

		Class<?> className = AppUtils.getClassByName(v.getContext()
				.getApplicationContext(),
				R.string.messages_activity_package);
		Intent i = new Intent();
		if (className != null)
			i.setClass(activityParent, className);
		i.putExtra(ConstantKeys.TIMELINE, mList.get(mPosition));
		activityParent.startActivity(i);
	}

	public ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.timeline_list_contextual_menu, menu);
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
			if (item.getItemId() == R.id.delete_group) {
				DataBasesAccess.getInstance(activityParent)
						.TimelinesDataBaseWriteDontShow(
								itemSelected.getParty().getId());

				deleteTimeline(itemSelected);

				for (int i = 0; i < mList.size(); i++) {

					if (mList.get(i).getParty().getId().toString()
							.equals(itemSelected.getParty().getId().toString())) {
						mList.remove(i);
					} else if (mList.get(i).getId().toString()
							.equals(itemSelected.getId().toString())) {
						mList.remove(i);
					}
				}
				notifyDataSetInvalidated();
				notifyDataSetChanged();
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
			if (mList.get(i).getParty().getId()
					.equals(itemSelected.getParty().getId())) {
				try {
					View v = listView.getChildAt(i
							- listView.getFirstVisiblePosition());
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

	private void deleteTimeline(final TimelineObject item) {
		new AsyncTask<Void, Void, String>() {

			private TimelineObject localItem = item;

			@Override
			protected String doInBackground(Void... params) {
				return AppUtils.actionOverTimeline(activityParent,
						Command.METHOD_DELETE_TIMELINE, localItem.getParty()
								.getId(), JsonKeys.GROUP, null, true);
			}

			@Override
			protected void onPostExecute(String result) {
				if (result.equals(ConstantKeys.SENDING_OK)) {

				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(activityParent,
							activityParent.getText(R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							activityParent,
							activityParent
									.getString(R.string.deleting_timeline_error),
							Toast.LENGTH_SHORT).show();

					DataBasesAccess.getInstance(activityParent)
							.TimelinesDataBaseIsRecoverTimeline(
									localItem.getParty().getId());

					mList.add(localItem);
					notifyDataSetInvalidated();
					notifyDataSetChanged();
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

}
