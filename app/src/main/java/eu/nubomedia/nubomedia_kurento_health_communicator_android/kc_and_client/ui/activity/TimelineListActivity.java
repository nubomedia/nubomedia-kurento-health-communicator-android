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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.adapter.TimelineListAdapter;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_client.ui.util.MyLifecycleHandler;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.TimelineObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.qos.QoScontroller;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandGetService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.QoSservice;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.JsonKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.UserReadContactResponse;

public class TimelineListActivity extends AnalyticsBaseActivity {

	protected static final Logger log = LoggerFactory
			.getLogger(TimelineListActivity.class.getSimpleName());

	public ArrayList<UserReadContactResponse> users;
	public String userName = ConstantKeys.STRING_DEFAULT;
	public Account account;

	protected TextView messageTextView;
	protected Menu mMenu = null;
	protected MenuItem serverStatusItem = null;
	protected MenuItem agendaMenuItem = null;
	protected MenuItem selectAccountItem = null;
	protected MenuItem preferencesItem = null;

	protected boolean receiversAreRegistered;

	protected TimelineListActivity timelineListActivity = null;

	protected Map<MenuItem, Boolean> menuItemInitailVisibleStates = new HashMap<MenuItem, Boolean>();

	protected boolean serverStatusCanBeShown = true;

	protected static boolean isAuthenticatorDisplayed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timeline_list);
		((ImageView) this.findViewById(R.id.bg_screen))
				.setBackgroundDrawable(new BitmapDrawable(getResources(),
						BitmapFactory.decodeStream(getResources()
								.openRawResource(R.drawable.bg))));

		messageTextView = (TextView) findViewById(R.id.message);
		receiversAreRegistered = false;

		timelineListActivity = this;

		Long timelineId = (Long)getIntent().getSerializableExtra(
				ConstantKeys.NOTIFICATION);

		TimelineObject timeline = DataBasesAccess.getInstance(this).TimelinesDataBaseReadTimelineIdSelected(timelineId);

		if (timeline != null) {
			Class<?> className = AppUtils.getClassByName(timelineListActivity,
					R.string.messages_activity_package);
			Intent i = new Intent();
			if (className != null)
				i.setClass(timelineListActivity, className);
			else
				i.setClass(timelineListActivity, MessagesActivity.class);
			i.putExtra(ConstantKeys.TIMELINE, timeline);
			timelineListActivity.startActivity(i);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!receiversAreRegistered) {
			registerReceiver(mReceiverGCM, intentFilter);
			registerReceiver(mReceiverGettingCommand,
					intentFilterFinishGettingCommand);
			registerReceiver(mReceiverQoSActiveChange,
					intentFilterQoSActiveChange);
			registerReceiver(mReceiverQoSStateChange,
					intentFilterQoSStateChange);
			receiversAreRegistered = true;
		}

		myOnResume();
	}

	protected void myOnResume() {
		AppUtils.CancelNotification(getApplicationContext());
		Account ac = AccountUtils.getAccount(timelineListActivity, true);
		if (ac != null) {
			account = ac;
			AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
			userName = am.getUserData(ac, JsonKeys.NAME) + " "
					+ am.getUserData(ac, JsonKeys.SURNAME);
			getActionBar().setSubtitle(userName);
			updateQoSService();
		} else {
			//Check if AuthenticatorActivity is already displayed
			AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.TRUE,
					timelineListActivity);
			if (!isAuthenticatorDisplayed) {
				isAuthenticatorDisplayed = true;
				Context ctx = getApplicationContext();
				Intent intent = new Intent(ctx, AuthenticatorActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				ctx.startActivity(intent);
			}
		}

		createList();
	}

	public static void setIsAuthenticatorDisplayed (boolean isDisplayed) {
		isAuthenticatorDisplayed = isDisplayed;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		myOnCreateOptionsMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}

	protected void myOnCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.timeline_menu, menu);
		mMenu = menu;
		saveInitialMenuItemStates(menu);
		changeServerState(serverStatusItem);
		showServerState(serverStatusItem);
	}

	protected void saveInitialMenuItemStates(Menu menu) {
		serverStatusItem = menu.findItem(R.id.server_status);
		if (serverStatusItem != null)
			menuItemInitailVisibleStates.put(serverStatusItem,
					serverStatusItem.isVisible());

		agendaMenuItem = menu.findItem(R.id.agenda_menu);
		if (agendaMenuItem != null)
			menuItemInitailVisibleStates.put(agendaMenuItem,
					agendaMenuItem.isVisible());

		preferencesItem = menu.findItem(R.id.main_menu_preferences);
		if (preferencesItem != null)
			menuItemInitailVisibleStates.put(preferencesItem,
					preferencesItem.isVisible());

		selectAccountItem = menu.findItem(R.id.main_menu_select_account);
		if (selectAccountItem != null)
			menuItemInitailVisibleStates.put(selectAccountItem,
					selectAccountItem.isVisible());
	}

	protected void loadInitialMenuItemStates(Menu menu) {
		if (menu == null || menuItemInitailVisibleStates == null)
			return;

		Boolean visible = null;
		if (menu.findItem(R.id.server_status) != null) {
			visible = menuItemInitailVisibleStates.get(serverStatusItem);
			if (visible != null)
				menu.findItem(R.id.server_status).setVisible(
						visible && QoScontroller.isQosActive());
		}

		if (menu.findItem(R.id.agenda_menu) != null) {
			visible = menuItemInitailVisibleStates.get(agendaMenuItem);
			if (visible != null)
				menu.findItem(R.id.agenda_menu).setVisible(visible);
		}

		if (menu.findItem(R.id.main_menu_preferences) != null) {
			visible = menuItemInitailVisibleStates.get(preferencesItem);
			if (visible != null)
				menu.findItem(R.id.main_menu_preferences).setVisible(visible);
		}

		if (menu.findItem(R.id.main_menu_select_account) != null) {
			visible = menuItemInitailVisibleStates.get(selectAccountItem);
			if (visible != null)
				menu.findItem(R.id.main_menu_select_account)
						.setVisible(visible);
		}
	}

	protected void changeServerState(MenuItem serverStatusItem) {
		if (serverStatusItem != null) {
			try {
				switch (QoScontroller.getQosState()) {
				case CONN_OK:
					serverStatusItem.setIcon(R.drawable.ic_server_conn_ok);
					break;
				case CONN_DEG:
					serverStatusItem.setIcon(R.drawable.ic_server_conn_deg);
					break;
				default:
					serverStatusItem.setIcon(R.drawable.ic_server_conn_nok);
					onConnectionError();
					break;
				}
			} catch (Exception e) {
				log.warn("Error setting server status icon.", e);
			}
		}
	}

	protected void onConnectionError() {
		// do nothing for generic android client
	}

	protected void showServerState(MenuItem serverStatusItem) {
		if (serverStatusItem != null) {
			log.trace("showServerState(): QosActive:"
					+ QoScontroller.isQosActive() + ", canBeShown:"
					+ serverStatusCanBeShown);
			try {
				if (QoScontroller.isQosActive() && serverStatusCanBeShown)
					serverStatusItem.setVisible(true);
				else
					serverStatusItem.setVisible(false);
			} catch (Exception e) {
				log.warn("Error setting server status visibility.", e);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE,
				timelineListActivity);

		int itemId = item.getItemId();
		if (itemId == R.id.server_status) {
			if (QoScontroller.isQosActive()) {
				switch (QoScontroller.getQosState()) {
				case CONN_OK:
					showAlertDialog(getString(R.string.qos_dialog_title),
							getString(R.string.qos_connection_ok),
							getString(R.string.qos_dialog_button_text));
					break;
				case CONN_DEG:
					showAlertDialog(getString(R.string.qos_dialog_title),
							getString(R.string.qos_connection_deg),
							getString(R.string.qos_dialog_button_text));
					break;
				default:
					showAlertDialog(
							getString(R.string.qos_dialog_title_conn_nok),
							getString(R.string.qos_connection_nok),
							getString(R.string.qos_dialog_button_text));
					break;
				}
			} else {
				// Do nothing
			}
		} else if (itemId == R.id.agenda_menu) {
			Intent addTimeline = new Intent(timelineListActivity,
					ItemListActivity.class);
			startActivityForResult(addTimeline, AppUtils.RETURN_ITEM_SELECTED);
		} else if (itemId == R.id.main_menu_select_account) {
			Intent editUserIntent = new Intent(timelineListActivity,
					EditUserActivity.class);
			startActivity(editUserIntent);
		} else if (itemId == R.id.main_menu_preferences) {
			Intent preferencesIntent = new Intent(timelineListActivity,
					Preferences.class);
			startActivityForResult(preferencesIntent, AppUtils.RETURN_SETUP);
		}

		return super.onOptionsItemSelected(item);
	}

	protected void showAlertDialog(String title, String message,
			String buttonText) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				timelineListActivity);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setNeutralButton(buttonText,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void createList() {
		messageTextView.setVisibility(View.GONE);
		if (mMenu != null) {
			serverStatusCanBeShown = true;
			mMenu.setGroupVisible(0, true);
			loadInitialMenuItemStates(mMenu);
			showServerState(serverStatusItem);
			if (availableButtonListener != null) {
				availableButtonListener.availableButton();
			}
			availableButtonListener = null;
		}

		ArrayList<TimelineObject> tl = DataBasesAccess.getInstance(
				getApplicationContext()).TimelinesDataBaseRead();

		ArrayList<TimelineObject> aux = new ArrayList<TimelineObject>();
		for (TimelineObject tlo : tl) {
			if ((tlo.getState().equals(State.HIDDEN))
					|| (tlo.getLastMessageId()
							.equals(ConstantKeys.LONG_DEFAULT))) {
				continue;
			}

			if (tlo.isNewMessages()) {
				tlo.setNewIconSize(getResources().getDimensionPixelSize(
						R.dimen.timeline_new_status));
				tlo.setTagResourceId(R.drawable.bg_status_green);

				if (receiveMsgListener != null) {
					receiveMsgListener.receiveMsg();
				}
			} else {
				tlo.setNewIconSize(0);
				tlo.setTagResourceId(R.drawable.bg_status_grey);
			}

			long lastMsgTimestamp = tlo.getLastMessageTimestamp();
			Date date = new Date(lastMsgTimestamp);

			DateFormat timeFormat;
			if (DateUtils.isToday(date.getTime())) {
				timeFormat = new SimpleDateFormat("HH:mm");
			} else {
				timeFormat = new SimpleDateFormat("dd/MM/yyyy");
			}

			tlo.setStatusToPaint(timeFormat.format(date));
			aux.add(tlo);
		}

		ListView listView = (ListView) findViewById(R.id.list_view);
		TimelineListAdapter adapter = new TimelineListAdapter(
				timelineListActivity, listView, aux);
		listView.setAdapter(adapter);

		if (aux.isEmpty()) {
			messageTextView.setVisibility(View.VISIBLE);
			if (mMenu != null) {
				serverStatusCanBeShown = false;
				mMenu.setGroupVisible(0, false);
			}
		}

		log.warn("Launching CommandGetService");
		startService(new Intent(getApplicationContext(),
				CommandGetService.class));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		if (requestCode == AppUtils.RETURN_SETUP) {
			if (data.getExtras().getBoolean(ConstantKeys.RESET)) {
				AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
				Account ac = AccountUtils
						.getAccount(timelineListActivity, true);

				if (ac == null) {
					log.error("Error trying to reset app. Account is null");
					return;
				}

				am.removeAccount(ac, null, null);
				AppUtils.purgeApp(timelineListActivity);
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (receiversAreRegistered) {
			unregisterReceiver(mReceiverGCM);
			unregisterReceiver(mReceiverGettingCommand);
			unregisterReceiver(mReceiverQoSActiveChange);
			unregisterReceiver(mReceiverQoSStateChange);
			receiversAreRegistered = false;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.timeline_list);
	}

	/* Broadcast receivers */
	protected IntentFilter intentFilter = new IntentFilter(
			ConstantKeys.BROADCAST_GCM);
	protected BroadcastReceiver mReceiverGCM = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_TIMELINE)) {
				createList();
			}

			if (intent.getExtras().getBoolean(Command.METHOD_UPDATE_USER)) {
				if (account != null) {
					AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
					userName = am.getUserData(account, JsonKeys.NAME) + " "
							+ am.getUserData(account, JsonKeys.SURNAME);
					getActionBar().setSubtitle(userName);
					updateQoSService();

					if (updateUserListener != null) {
						updateUserListener.userEdited();
					}
					updateUserListener = null;
				}
			}
		}
	};

	protected IntentFilter intentFilterFinishGettingCommand = new IntentFilter(
			ConstantKeys.BROADCAST_GET_COMMANDS_FINISH);
	protected BroadcastReceiver mReceiverGettingCommand = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			AppUtils.setDefaults(ConstantKeys.FROMLOGIN, ConstantKeys.FALSE,
					timelineListActivity);
			messageTextView.setVisibility(View.GONE);

			if (mMenu != null) {
				serverStatusCanBeShown = true;
				mMenu.setGroupVisible(0, true);
				loadInitialMenuItemStates(mMenu);
				showServerState(serverStatusItem);
			}

			if (availableButtonListener != null) {
				availableButtonListener.availableButton();
			}
			availableButtonListener = null;

			MyLifecycleHandler.init();
		}
	};

	protected IntentFilter intentFilterQoSActiveChange = new IntentFilter(
			QoSservice.BROADCAST_QOS_ACTIVE_CHANGE);
	protected BroadcastReceiver mReceiverQoSActiveChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			log.debug("Broadcast qos active change received.");
			showServerState(serverStatusItem);
		}
	};

	protected IntentFilter intentFilterQoSStateChange = new IntentFilter(
			QoSservice.BROADCAST_QOS_STATE_CHANGE);
	protected BroadcastReceiver mReceiverQoSStateChange = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			log.debug("Broadcast qos state change received.");
			changeServerState(serverStatusItem);
		}
	};

	/* Test Utilities */

	public static Long getPartyId(String name, Context ctx) {
		ArrayList<TimelineObject> tl = DataBasesAccess.getInstance(ctx)
				.TimelinesDataBaseRead();
		for (TimelineObject time : tl) {
			if (time.getParty().getName().equals(name)) {
				return time.getParty().getId();
			}
		}

		return 0L;
	}

	public static String getPartyType(String name, Context ctx) {
		ArrayList<TimelineObject> tl = DataBasesAccess.getInstance(ctx)
				.TimelinesDataBaseRead();
		for (TimelineObject time : tl) {
			if (time.getParty().getName().equals(name)) {
				return time.getParty().getType();
			}
		}

		return ConstantKeys.STRING_DEFAULT;
	}

	protected AvailableButtonListener availableButtonListener;

	public interface AvailableButtonListener {
		void availableButton();
	}

	public void setAvailableButtonListener(AvailableButtonListener l) {
		timelineListActivity.availableButtonListener = l;
	}

	protected ReceiveMsgListener receiveMsgListener;

	public interface ReceiveMsgListener {
		void receiveMsg();
	}

	public void setReceiveMsgListener(ReceiveMsgListener l) {
		timelineListActivity.receiveMsgListener = l;
	}

	protected EditUserTimelineListListener updateUserListener;

	public interface EditUserTimelineListListener {
		void userEdited();
	}

	public void setEditUserListener(EditUserTimelineListListener l) {
		timelineListActivity.updateUserListener = l;
	}

	protected void updateQoSService() {
		boolean qosActive = AccountUtils.getQosFlag(getApplicationContext());
		QoScontroller.setQosActive(qosActive, getApplicationContext());
	}
}
