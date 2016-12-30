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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.kurento.agenda.services.pojo.MessageApp;
//import com.kurento.agenda.services.pojo.NubomediaMessagePayload;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.MassiveTestActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.activity.MessagesActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.serialization.JacksonManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.FileUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ImageDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kurento.agenda.services.pojo.ContentReadResponse;

public class MessageListAdapter extends BaseAdapter {

	private static final Logger log = LoggerFactory
			.getLogger(MessageListAdapter.class.getSimpleName());

	private Context mContext;
	public List<MessageObject> mMessageList;
	private ImageView mArrowImage = null;
	private TimelineObject mTimeline = null;

	private int selectedPosition = -1;
	private View selectedView = null;

	private final ImageDownloader downloader = new ImageDownloader();

	public MessageListAdapter(Context context, List<MessageObject> messageList,
			ImageView arrowImage, TimelineObject timeline) {
		mMessageList = messageList;
		mContext = context;
		mArrowImage = arrowImage;
		mTimeline = timeline;
	}

	private class ViewHolder {

		private TextView from;
		private TextView date;
		private TextView size;
		private TextView body;
		private TextView status;

		private ImageView avatar;
		private ImageView avatarShadow;
		private ImageView avatarBg;
		private ImageView thumbnail;
		private ImageView icon;
		private ImageView cancel;
		private ImageView statusIcon;

		private RelativeLayout layout;
		private RelativeLayout mediaLayout;
		private RelativeLayout rootLayout;
		private RelativeLayout callButtonLayout;

		private ProgressBar largebar;
		private ProgressBar spinner;

		private String mediaId;
		private String messageId;
		private String localId;
		private String attachmentSizeValue;
	}

	public void refreshMessageStatustoSent(Long messageLocalId) {
		for (int i = mMessageList.size() - 1; i >= 0; i--) {
			MessageObject mo = mMessageList.get(i);
			if (!mo.getLocalId().equals(messageLocalId)) {
				continue;
			}

			mo.incStatusAndUpdate(MessageObject.Status.SENT, mContext);
			DataBasesAccess.getInstance(mContext)
					.MessagesDataBaseSetMessageStatus(mo.getLocalId(),
							mo.getStatus());

			View v = ((MessagesActivity) mContext).listView.getChildAt(i
					- ((MessagesActivity) mContext).listView
							.getFirstVisiblePosition());

			if (v != null) {
				((ImageView) v.findViewById(R.id.status_icon))
						.setBackgroundResource(mo.getStatusIcon());
			}

			return;
		}
	}

	private void updateView(int index, int total, boolean closeAt100,
			boolean download) {
		View v = ((MessagesActivity) mContext).listView.getChildAt(index
				- ((MessagesActivity) mContext).listView
						.getFirstVisiblePosition());

		if (v == null) {
			log.trace("The item is no longer in screen");
			return;
		}

		ProgressBar pb = (ProgressBar) v.findViewById(R.id.message_largebar);
		ImageView cancelView = (ImageView) v
				.findViewById(R.id.message_cancel_icon);
		ProgressBar spinner = (ProgressBar) v
				.findViewById(R.id.message_spinner);

		if ((closeAt100) && (total == 100)) {
			pb.setVisibility(View.GONE);
			if (download) {
				cancelView.setVisibility(View.GONE);
			}
			spinner.setVisibility(View.GONE);
		} else {
			pb.setVisibility(View.VISIBLE);
			if (download) {
				cancelView.setVisibility(View.VISIBLE);
			}
			spinner.setVisibility(View.VISIBLE);
		}
		pb.setProgress(total);
	}

	public void refreshProgress(Long id, int total, boolean closeAt100) {
		for (int i = mMessageList.size() - 1; i >= 0; i--) {
			MessageObject mo = mMessageList.get(i);
			if (mo.getLocalId().equals(id)) {
				mo.setTotal(total);
				boolean download = (mo.getId() != ConstantKeys.LONG_DEFAULT);
				updateView(i, total, closeAt100, download);

				return;
			}
		}
	}

	@Override
	public int getCount() {
		return mMessageList.size();
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
	public View getView(final int position, View convertView,
			final ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;

		if (convertView == null) {
			view = ((Activity) mContext).getLayoutInflater().inflate(
					R.layout.simple_message, null);

			holder = new ViewHolder();
			holder.from = (TextView) view.findViewById(R.id.message_from);
			holder.date = (TextView) view.findViewById(R.id.message_date);
			holder.size = (TextView) view
					.findViewById(R.id.message_size_attach);
			holder.body = (TextView) view.findViewById(R.id.message_body);
			holder.status = (TextView) view.findViewById(R.id.message_status);

			holder.avatar = (ImageView) view.findViewById(R.id.message_avatar);
			holder.statusIcon = (ImageView) view.findViewById(R.id.status_icon);
			holder.avatarShadow = (ImageView) view
					.findViewById(R.id.message_avatar_shadow);
			holder.avatarBg = (ImageView) view
					.findViewById(R.id.message_avatar_bg);
			holder.thumbnail = (ImageView) view
					.findViewById(R.id.media_thumbnail);
			holder.icon = (ImageView) view
					.findViewById(R.id.message_attach_icon);
			holder.cancel = (ImageView) view
					.findViewById(R.id.message_cancel_icon);

			holder.rootLayout = (RelativeLayout) view
					.findViewById(R.id.message_root_layout);
			holder.layout = (RelativeLayout) view
					.findViewById(R.id.message_layout);
			holder.mediaLayout = (RelativeLayout) view
					.findViewById(R.id.media_layer);
			holder.largebar = (ProgressBar) view
					.findViewById(R.id.message_largebar);
			holder.spinner = (ProgressBar) view
					.findViewById(R.id.message_spinner);

			holder.callButtonLayout = (RelativeLayout) view
					.findViewById(R.id.message_call_button);

			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}

		if (position == mMessageList.size() - 1) {
			mArrowImage.clearAnimation();
			mArrowImage.setVisibility(View.GONE);
		}

		final MessageObject mObject = mMessageList.get(position);
		final Long mMessageId = mObject.getId();
		final ContentReadResponse mMessageContent = mObject.getContent();

		// From name and surname
		holder.from.setText(mObject.getFrom().getName() + " "
				+ mObject.getFrom().getSurname());

		Date date = new Date(mObject.getTimestamp());
		DateFormat timeFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy");
		holder.date.setText(timeFormat.format(date));

		holder.body.setText(mObject.getBody());
		holder.size.setText(mObject.getHumanSize());

		// Set large bar status
		if ((mObject.getTotal() == 100)
				|| (mMessageContent.getContentSize() == 0)
				|| (mObject.getTotal() == 0)) {
			holder.largebar.setVisibility(View.GONE);
			holder.cancel.setVisibility(View.GONE);
			holder.spinner.setVisibility(View.GONE);
			holder.largebar.setProgress(100);
		} else {
			holder.largebar.setVisibility(View.VISIBLE);
			if (!(String.valueOf(mMessageId)).equalsIgnoreCase("0")) {
				holder.cancel.setVisibility(View.VISIBLE);
			} else {
				holder.cancel.setVisibility(View.GONE);
			}
			holder.spinner.setVisibility(View.VISIBLE);
			holder.largebar.setProgress(mObject.getTotal());
		}

		holder.statusIcon.setBackgroundResource(mObject.getStatusIcon());

		holder.icon.setBackgroundResource(mObject.getMediaTypeIconResource());

		holder.mediaId = String.valueOf(mMessageContent.getId());
		holder.messageId = String.valueOf(mMessageId);
		holder.localId = String.valueOf(mObject.getLocalId());
		holder.attachmentSizeValue = String.valueOf(mMessageContent
				.getContentSize());
		holder.thumbnail.setTag(holder.mediaId + ":" + holder.messageId + ":"
				+ holder.localId + ":" + holder.attachmentSizeValue);
		holder.thumbnail.setImageBitmap(null);
		holder.thumbnail.setBackgroundColor(Color.GRAY);
		holder.thumbnail.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// set the data
				String data = (String) v.getTag();
				String contentId = data.split(":")[0];
				String messageId = data.split(":")[1];
				String localId = data.split(":")[2];
				int size = Integer.valueOf(data.split(":")[3]);

				boolean isDialogSummon = false;

				File file1 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_JPG);
				File file2 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_3GP);
				if ((!file1.exists()) && (!file2.exists())) {
					Intent intent = new Intent();
					intent.setAction(ConstantKeys.BROADCAST_DIALOG_PROGRESSBAR);
					intent.putExtra(ConstantKeys.TOTAL, 1);
					intent.putExtra(ConstantKeys.LOCALID, localId);
					mContext.sendBroadcast(intent);
					isDialogSummon = true;
				}

				file1 = null;
				file2 = null;
				System.gc();

				boolean findIt = false;
				for (int i = 0; i < AppUtils.getlistOfDownload().size(); i++) {
					if (AppUtils.getlistOfDownload().get(i).equals(localId)) {
						findIt = true;
						if (!isDialogSummon) {
							Intent intent = new Intent();
							intent.setAction(ConstantKeys.BROADCAST_DIALOG_PROGRESSBAR);
							intent.putExtra(ConstantKeys.TOTAL, 1);
							intent.putExtra(ConstantKeys.LOCALID, localId);
							mContext.sendBroadcast(intent);
						}
					}
				}

				if (!findIt) {
					AppUtils.getlistOfDownload().add(localId);
					FileUtils.DownloadFromUrl(contentId, messageId, mContext,
							null, null, ((MessagesActivity) mContext).timeline
									.getId().toString(), localId, Long
									.valueOf(size));
				}

			}
		});

		holder.cancel.setTag(String.valueOf(mObject.getLocalId()));
		holder.cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(ConstantKeys.BROADCAST_CANCEL_PROCESS);
				intent.putExtra(ConstantKeys.LOCALID, (String) v.getTag());
				mContext.sendBroadcast(intent);
				v.setVisibility(View.GONE);
			}
		});

		holder.callButtonLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Object app = mObject.getApp();
				MessageApp msgApp = null;
				if (app != null) {
					try {
						msgApp = JacksonManager.parseJsonObject(app,
								MessageApp.class);
					} catch (Exception e) {
					}
				}

				if (msgApp != null) {
					String name = msgApp.getName();
					if (name != null) {
						/*if (name.equalsIgnoreCase(NubomediaMessagePayload.APP_NAME_NUBOMEDIA)) {
							NubomediaMessagePayload payload;
							try {
								payload = JacksonManager.parseJsonObject(
										msgApp.getPayload(), NubomediaMessagePayload.class);
							} catch (Exception e) {
							}
						} else {
							UserObject userSelected = DataBasesAccess.getInstance(mContext)
									.getUserDataBase(mObject.getFrom().getId().toString());

							if (userSelected == null) {
								log.warn("User not set, cannot call");
								return;
							}
						}*/
					}
				}
			}
		});

		log.debug("getCanCall(): " + mObject.getCanCall());
		// Call button
		if (mObject.getCanCall() == null) {
			holder.callButtonLayout.setVisibility(View.GONE);
		} else {
			if (mObject.getCanCall()) {
				holder.callButtonLayout.findViewById(
						R.id.image_call_done_button).setVisibility(View.GONE);
				holder.callButtonLayout.findViewById(R.id.image_call_button)
						.setVisibility(View.VISIBLE);
			} else {
				holder.callButtonLayout.findViewById(R.id.image_call_button)
						.setVisibility(View.GONE);
				holder.callButtonLayout.findViewById(
						R.id.image_call_done_button)
						.setVisibility(View.VISIBLE);
			}
			holder.callButtonLayout.setVisibility(View.VISIBLE);
		}

		// Avatar image loader
		if (mObject.getAvatarSize() != 0) {
			downloader.downloadMessageAvatar(mContext.getApplicationContext(),
					mMessageId.toString(), mObject.getFrom().getPicture()
							.toString(), ((MessagesActivity) mContext).timeline
							.getId().toString(), holder.avatar, mObject
							.getFrom().getId().toString());
		}

		if ((mMessageContent.getId() != 0) && (mMessageId != 0)) {
			downloader.downloadThumbnail(mContext.getApplicationContext(),
					mMessageContent.getId(), mMessageId, holder.thumbnail,
					((MessagesActivity) mContext).timeline.getId().toString(),
					mObject.getLocalId().toString(), mObject.getContent()
							.getContentSize());
		}

		// Main layer params
		holder.layout.setBackgroundResource(mObject.getBackgroundColor());

		if (position == selectedPosition) {
			view.setBackgroundColor(Color.GRAY);
			selectedView = view;
		} else {
			view.setBackgroundColor(Color.TRANSPARENT);
		}

		RelativeLayout.LayoutParams layerParams = (RelativeLayout.LayoutParams) holder.rootLayout
				.getLayoutParams();
		layerParams.setMargins(mObject.getMarginLeft(), 0,
				mObject.getMarginRight(), 0);
		holder.rootLayout.setLayoutParams(layerParams);

		// Thumbnail params
		RelativeLayout.LayoutParams thumbnailLayerParams = (RelativeLayout.LayoutParams) holder.mediaLayout
				.getLayoutParams();
		thumbnailLayerParams.width = mObject.getMediaThumbnailSize();
		thumbnailLayerParams.height = mObject.getMediaThumbnailSize();
		holder.mediaLayout.setLayoutParams(thumbnailLayerParams);

		RelativeLayout.LayoutParams thumbnailParams = (RelativeLayout.LayoutParams) holder.thumbnail
				.getLayoutParams();
		thumbnailParams.width = mObject.getMediaThumbnailSize();
		holder.thumbnail.setLayoutParams(thumbnailParams);

		// Thumbnail icon type params
		RelativeLayout.LayoutParams thumbnailIconTypeParams = (RelativeLayout.LayoutParams) holder.icon
				.getLayoutParams();
		thumbnailIconTypeParams.height = mObject.getMediaTypeIconSize();
		thumbnailIconTypeParams.width = mObject.getMediaTypeIconSize();
		holder.icon.setLayoutParams(thumbnailIconTypeParams);

		// Avatar params
		RelativeLayout.LayoutParams avatarParams = (RelativeLayout.LayoutParams) holder.avatar
				.getLayoutParams();
		avatarParams.height = mObject.getAvatarSize();
		avatarParams.width = mObject.getAvatarSize();
		holder.avatar.setLayoutParams(avatarParams);

		RelativeLayout.LayoutParams avatarShadowParams = (RelativeLayout.LayoutParams) holder.avatarShadow
				.getLayoutParams();
		avatarShadowParams.height = mObject.getAvatarSize();
		avatarShadowParams.width = mObject.getAvatarSize();
		holder.avatarShadow.setLayoutParams(avatarShadowParams);

		RelativeLayout.LayoutParams avatarBgParams = (RelativeLayout.LayoutParams) holder.avatarBg
				.getLayoutParams();
		avatarBgParams.height = mObject.getAvatarSize();
		avatarBgParams.width = mObject.getAvatarSize();
		holder.avatarBg.setLayoutParams(avatarBgParams);

		RelativeLayout.LayoutParams messageFromParams = (RelativeLayout.LayoutParams) holder.from
				.getLayoutParams();
		messageFromParams.height = mObject.getMessageFromSize();
		holder.from.setLayoutParams(messageFromParams);

		RelativeLayout.LayoutParams messageStatusIconParams = (RelativeLayout.LayoutParams) holder.statusIcon
				.getLayoutParams();
		messageStatusIconParams.width = mObject.getMessageStatusSize();
		holder.statusIcon.setLayoutParams(messageStatusIconParams);

		holder.body.setTag(position);

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ViewHolder holder = (ViewHolder) v.getTag();
				String body = holder.body.getText().toString();
				String testingMsg = mContext
						.getString(R.string.message_for_testing);

				if (!testingMsg.equals(body)) {
					return;
				}

				Intent i = new Intent();
				i.setClass(mContext.getApplicationContext(),
						MassiveTestActivity.class);
				i.putExtra(ConstantKeys.TIMELINE, mTimeline);
				((Activity) mContext).startActivityForResult(i,
						AppUtils.ACTION_RESPONSE);
				((Activity) mContext).overridePendingTransition(
						R.anim.righttoleft_rightactivity,
						R.anim.righttoleft_leftactivity);
			}
		});

		return view;
	}

}
