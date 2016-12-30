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

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.GroupObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.UserObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;

public class SearchUserListAdapter extends BaseAdapter implements
		OnClickListener {

	private Context mContext;
	private ArrayList<UserObject> mList = new ArrayList<UserObject>();
	private Animation myFadeInAnimation;
	public GroupObject mGroup = null;
	private ImageDownloader downloader = null;

	public SearchUserListAdapter(Context context, ArrayList<UserObject> list,
			GroupObject group) {
		mList = list;
		mContext = context;
		mGroup = group;
		myFadeInAnimation = AnimationUtils.loadAnimation(
				mContext.getApplicationContext(), R.anim.tween);
		downloader = new ImageDownloader();
	}

	private class ViewHolder {
		private ImageView image;
		private TextView name;
		private CheckBox checkbox;
	}

	public ArrayList<UserObject> getList() {
		return mList;
	}

	public void refreshList(ArrayList<UserObject> list) {
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
					R.layout.add_user_item_list, null);

			holder = new ViewHolder();
			holder.name = (TextView) view.findViewById(R.id.user_name);
			holder.image = (ImageView) view.findViewById(R.id.user_image);
			holder.checkbox = (CheckBox) view.findViewById(R.id.checkbox);

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
					holder.image, mList.get(position).getId().toString(), mList
							.get(position).getPicture().toString());
		} catch (Exception e) {
			Log.d(mContext.getPackageName(), "no avatar find");
		}

		holder.name.setTag(position);
		holder.name.setText(mList.get(position).getName() + " " + mList.get(position).getSurname());

		holder.checkbox.setChecked(mList.get(position).isChecked());
		holder.checkbox.setClickable(false);
		holder.checkbox.setFocusable(false);
		holder.checkbox.setFocusableInTouchMode(false);

		view.setOnClickListener(this);

		return view;
	}

	@Override
	public void onClick(View v) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, "false", mContext);

		ViewHolder holder = (ViewHolder) v.getTag();
		int position = (Integer) holder.name.getTag();
		if (holder.checkbox.isChecked()) {
			mList.get(position).setChecked(false);
			holder.checkbox.setChecked(false);
		} else {
			mList.get(position).setChecked(true);
			holder.checkbox.setChecked(true);
		}
	}
}
