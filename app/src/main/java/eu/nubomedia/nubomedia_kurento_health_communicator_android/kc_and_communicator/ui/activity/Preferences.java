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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity;

import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AccountUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.AppUtils;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util.ConstantKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import android.widget.Toast;
import android.content.pm.PackageManager;

import com.kurento.agenda.datamodel.pojo.Command;

public class Preferences extends PreferenceActivity {

	protected static final Logger log = LoggerFactory
			.getLogger(Preferences.class.getSimpleName());

	// Preferences keys
	public static final String SERVER_PROTOCOL_KEY = "SERVER_PROTOCOL_KEY";
	public static final String SERVER_ADDRESS_KEY = "SERVER_ADDRESS_KEY";
	public static final String SERVER_PORT_KEY = "SERVER_PORT_KEY";
	public static final String SERVER_ACCOUNT_KEY = "SERVER_ACCOUNT_KEY";
	public static final String NOTIFICATIONS_SOUNDS = "NOTIFICATIONS_SOUNDS";
	public static final String MAX_MESSAGES = "MAX_MESSAGES";
	public static final String MEDIA_TOTAL_SIZE = "MEDIA_TOTAL_SIZE";

	public static final String INIT_CALL_WITH_VIDEO_KEY = "INIT_CALL_WITH_VIDEO";
	public static final String DEFAULT_CAMERA_KEY = "DEFAULT_CAMERA";

	public static final String STUN_ALLOW_KEY = "STUN_ALLOW";
	public static final String STUN_ADDRESS_KEY = "STUN_ADDRESS";
	public static final String STUN_PORT_KEY = "STUN_PORT";
	public static final String STUN_USER_KEY = "STUN_USER";
	public static final String STUN_PASSWORD_KEY = "STUN_PASSWORD";
	public static final String TURN_ALLOW_KEY = "TURN_ALLOW";
	public static final String TURN_ADDRESS_KEY = "TURN_ADDRESS";
	public static final String TURN_PORT_KEY = "TURN_PORT";
	public static final String TURN_USER_KEY = "TURN_USER";
	public static final String TURN_PASSWORD_KEY = "TURN_PASSWORD";

	public static final String RESET_BUTTON = "RESET_BUTTON";
	public static final String CONTACTS_BUTTON = "CONTACTS_BUTTON";

	public static final String USER_AUTO_REGISTER = "USER_AUTO_REGISTER";
	public static final String GROUP_AUTO_REGISTER = "GROUP_AUTO_REGISTER";
	public static final String ACCOUNT_NAME = "ACCOUNT_NAME";

	public static final String GETTING_COMMAND_STATUS = "GETTING_COMMAND_STATUS";

	public static final String INSTANCE_ID = "INSTANCE_ID";

	protected static boolean reset = false;

	protected static boolean active_preference_protocol = true;
	protected static boolean active_preference_host = true;
	protected static boolean active_preference_account = true;
	protected static boolean active_preference_port = true;
	protected static boolean active_preference_alerts = true;
	protected static boolean active_preference_message_num = true;
	protected static boolean active_preference_storage_size = true;
	protected static boolean active_preference_init_call_with_video = true;
	protected static boolean active_preference_default_camera = true;
	protected static boolean active_preference_stun = true;
	protected static boolean active_preference_turn = true;
	protected static boolean active_preference_stun_configuration = true;
	protected static boolean active_preference_turn_configuration = true;
	protected static boolean active_preference_sync = true;
	protected static boolean active_preference_find_users = true;
	protected static boolean active_preference_version = true;
	protected static boolean active_preference_buil_num = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadPreferencesProperties();
		setPreferenceScreen(createPreferenceHierarchy());
		this.getPreferenceManager()
				.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(
						onSharedPreferenceChangeListener);

		setTitle(getString(R.string.main_menu_preferences));
		getActionBar().setIcon(R.drawable.ic_settings_light);
	}

	protected void loadPreferencesProperties() {
		active_preference_protocol = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_protocol,
				true);
		active_preference_host = AppUtils.getResource(getApplicationContext(),
				R.bool.active_preference_host, true);
		active_preference_account = AppUtils
				.getResource(getApplicationContext(),
						R.bool.active_preference_account, true);
		active_preference_port = AppUtils.getResource(getApplicationContext(),
				R.bool.active_preference_port, true);
		active_preference_alerts = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_alerts, true);
		active_preference_message_num = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_message_num,
				true);
		active_preference_storage_size = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_storage_size,
				true);
		active_preference_default_camera = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_default_camera,
				true);
		active_preference_init_call_with_video = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_init_call_with_video,
				true);
		active_preference_stun = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_stun,
				true);
		active_preference_turn = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_turn,
				true);
		active_preference_sync = AppUtils.getResource(getApplicationContext(),
				R.bool.active_preference_sync, true);
		active_preference_find_users = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_find_users,
				true);
		active_preference_version = AppUtils
				.getResource(getApplicationContext(),
						R.bool.active_preference_version, true);
		active_preference_buil_num = AppUtils.getResource(
				getApplicationContext(), R.bool.active_preference_buil_num,
				true);
	}

	@Override
	protected void onDestroy() {
		this.getPreferenceManager()
				.getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(
						onSharedPreferenceChangeListener);
		super.onDestroy();
	}

	protected PreferenceScreen createPreferenceHierarchy() {
		PreferenceScreen root = getPreferenceManager().createPreferenceScreen(
				this);

		// Server
		if (active_preference_protocol) {
			EditTextPreference serverProtocolText = new EditTextPreference(this);
			serverProtocolText.setKey(SERVER_PROTOCOL_KEY);
			serverProtocolText
					.setTitle(getString(R.string.preference_server_protocol));
			serverProtocolText.setDefaultValue(getString(R.string.protocol));
			root.addPreference(serverProtocolText);
		}

		if (active_preference_host) {
			EditTextPreference serverAddessText = new EditTextPreference(this);
			serverAddessText.setKey(SERVER_ADDRESS_KEY);
			serverAddessText
					.setTitle(getString(R.string.preference_server_address));
			serverAddessText.setDefaultValue(getString(R.string.host));
			root.addPreference(serverAddessText);
		}

		if (active_preference_account) {
			EditTextPreference serverAccountText = new EditTextPreference(this);
			serverAccountText.setKey(SERVER_ACCOUNT_KEY);
			serverAccountText
					.setTitle(getString(R.string.preference_server_account));
			serverAccountText.setDefaultValue(getString(R.string.account_id));
			root.addPreference(serverAccountText);
		}

		if (active_preference_port) {
			EditTextPreference serverPortText = new EditTextPreference(this);
			serverPortText.setKey(SERVER_PORT_KEY);
			serverPortText.setTitle(getString(R.string.preference_server_port));
			serverPortText.getEditText().setInputType(
					InputType.TYPE_CLASS_NUMBER);
			serverPortText.setDefaultValue(getString(R.string.service_port));
			root.addPreference(serverPortText);
		}

		if (active_preference_alerts) {
			CheckBoxPreference notification = new CheckBoxPreference(this);
			notification.setKey(NOTIFICATIONS_SOUNDS);
			notification.setTitle(getString(R.string.set_notification));
			notification.setDefaultValue(true);
			root.addPreference(notification);
		}

		if (active_preference_message_num) {
			EditTextPreference maxMessageText = new EditTextPreference(this);
			maxMessageText.setKey(MAX_MESSAGES);
			maxMessageText.setTitle(getString(R.string.max_messages));
			maxMessageText.getEditText().setInputType(
					InputType.TYPE_CLASS_NUMBER);
			maxMessageText.setDefaultValue(String.valueOf(getResources().getInteger(R.integer.max_messages)));
			root.addPreference(maxMessageText);
		}

		if (active_preference_storage_size) {
			EditTextPreference mediaTotalSizeText = new EditTextPreference(this);
			mediaTotalSizeText.setKey(MEDIA_TOTAL_SIZE);
			mediaTotalSizeText.setTitle(getString(R.string.media_total_size));
			mediaTotalSizeText.getEditText().setInputType(
					InputType.TYPE_CLASS_NUMBER);
			mediaTotalSizeText
					.setDefaultValue(String.valueOf(getResources().getInteger(R.integer.media_total_size)));
			root.addPreference(mediaTotalSizeText);
		}

		if (active_preference_init_call_with_video) {
			SwitchPreference initVideo = new SwitchPreference(this);
			initVideo.setKey(INIT_CALL_WITH_VIDEO_KEY);
			initVideo.setTitle(getString(R.string.init_call_with_video));
			initVideo.setSummaryOff(getString(R.string.init_video_summary_off));
			initVideo.setSummaryOn(getString(R.string.init_video_summary_on));
			initVideo.setDefaultValue(getResources().getBoolean(R.bool.videocall_init_video_off));

			root.addPreference(initVideo);
		}

		if (active_preference_default_camera) {
			ListPreference defaultCamera = new ListPreference(this);
			defaultCamera.setKey(DEFAULT_CAMERA_KEY);
			defaultCamera.setTitle(getString(R.string.default_camera));

			if ((this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) &&
					(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))) {
				CharSequence[] entries = {getString(R.string.front_camera), getString(R.string.back_camera)};
				CharSequence[] entriesValues = {"front", "back"};
				defaultCamera.setEntries(entries);
				defaultCamera.setEntryValues(entriesValues);
			} else if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
				CharSequence[] entries = {getString(R.string.back_camera)};
				CharSequence[] entriesValues = {"back"};
				defaultCamera.setEntries(entries);
				defaultCamera.setEntryValues(entriesValues);
			} else if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
				CharSequence []entries = {getString(R.string.front_camera)};
				CharSequence[] entriesValues = {"front"};
				defaultCamera.setEntries(entries);
				defaultCamera.setEntryValues(entriesValues);
			}else {
				CharSequence []entries = {getString(R.string.none_camera)};
				CharSequence[] entriesValues = {"none"};
				defaultCamera.setEntries(entries);
				defaultCamera.setEntryValues(entriesValues);
			}
			defaultCamera.setDefaultValue(defaultCamera.getEntryValues()[0]);
			root.addPreference(defaultCamera);
		}

		if (active_preference_stun) {
			CheckBoxPreference stunCheckBox = new CheckBoxPreference(this);
			stunCheckBox.setKey(STUN_ALLOW_KEY);
			stunCheckBox.setTitle(getString(R.string.stun_enable));
			final boolean stunActivated = AppUtils.getResource(
					getApplicationContext(), R.bool.active_stun,
					true);
			stunCheckBox.setDefaultValue(stunActivated);
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			active_preference_stun_configuration = pref.getBoolean(STUN_ALLOW_KEY, stunActivated);
			stunCheckBox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences pref = PreferenceManager
							.getDefaultSharedPreferences(preference.getContext());
					active_preference_stun_configuration = pref.getBoolean(STUN_ALLOW_KEY, stunActivated);
					setPreferenceScreen(createPreferenceHierarchy());
					return true;
				}
			});
			root.addPreference(stunCheckBox);
		} else
			active_preference_stun_configuration = false;

		EditTextPreference stunAddressText = new EditTextPreference(this);
		if (active_preference_stun_configuration) {
			stunAddressText.setKey(STUN_ADDRESS_KEY);
			stunAddressText.setTitle(getString(R.string.stun_address));
			stunAddressText
					.setDefaultValue(getString(R.string.stunHost));
			root.addPreference(stunAddressText);
		}

		if (active_preference_stun_configuration) {
			EditTextPreference stunPortText = new EditTextPreference(this);
			stunPortText.setKey(STUN_PORT_KEY);
			stunPortText.setTitle(getString(R.string.stun_port));
			stunPortText.getEditText().setInputType(
					InputType.TYPE_CLASS_NUMBER);
			stunPortText.setDefaultValue(getString(R.string.stunPort));
			root.addPreference(stunPortText);
		}

		if (active_preference_stun_configuration) {
			EditTextPreference stunUserText = new EditTextPreference(this);
			stunUserText.setKey(STUN_USER_KEY);
			stunUserText.setTitle(getString(R.string.stun_user));
			stunUserText
					.setDefaultValue(getString(R.string.stunUser));
			root.addPreference(stunUserText);
		}

		if (active_preference_stun_configuration) {
			EditTextPreference stunPasswordText = new EditTextPreference(this);
			stunPasswordText.setKey(STUN_PASSWORD_KEY);
			stunPasswordText.setTitle(getString(R.string.stun_password));
			stunPasswordText.getEditText().setInputType(
					InputType.TYPE_TEXT_VARIATION_PASSWORD);
			stunPasswordText
					.setDefaultValue(getString(R.string.stunPassword));
			root.addPreference(stunPasswordText);
		}

		if (active_preference_turn) {
			CheckBoxPreference turnCheckBox = new CheckBoxPreference(this);
			turnCheckBox.setKey(TURN_ALLOW_KEY);
			turnCheckBox.setTitle(getString(R.string.turn_enable));
			final boolean turnActivated = AppUtils.getResource(
					getApplicationContext(), R.bool.active_turn,
					true);
			turnCheckBox.setDefaultValue(turnActivated);
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			active_preference_turn_configuration = pref.getBoolean(TURN_ALLOW_KEY, turnActivated);
			turnCheckBox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences pref = PreferenceManager
							.getDefaultSharedPreferences(preference.getContext());
					active_preference_turn_configuration = pref.getBoolean(TURN_ALLOW_KEY, turnActivated);
					setPreferenceScreen(createPreferenceHierarchy());
					return true;
				}
			});
			root.addPreference(turnCheckBox);
		} else
			active_preference_turn_configuration = false;

		if (active_preference_turn_configuration) {
			EditTextPreference turnAddressText = new EditTextPreference(this);
			turnAddressText.setKey(TURN_ADDRESS_KEY);
			turnAddressText.setTitle(getString(R.string.turn_address));
			turnAddressText
					.setDefaultValue(getString(R.string.turnHost));
			root.addPreference(turnAddressText);
		}

		if (active_preference_turn_configuration) {
			EditTextPreference turnPortText = new EditTextPreference(this);
			turnPortText.setKey(TURN_PORT_KEY);
			turnPortText.setTitle(getString(R.string.turn_port));
			turnPortText.getEditText().setInputType(
					InputType.TYPE_CLASS_NUMBER);
			turnPortText.setDefaultValue(getString(R.string.turnPort));
			root.addPreference(turnPortText);
		}

		if (active_preference_turn_configuration) {
			EditTextPreference turnUserText = new EditTextPreference(this);
			turnUserText.setKey(TURN_USER_KEY);
			turnUserText.setTitle(getString(R.string.turn_user));
			turnUserText
					.setDefaultValue(getString(R.string.turnUser));
			root.addPreference(turnUserText);
		}

		if (active_preference_turn_configuration) {
			EditTextPreference turnPasswordText = new EditTextPreference(this);
			turnPasswordText.setKey(TURN_PASSWORD_KEY);
			turnPasswordText.setTitle(getString(R.string.turn_password));
			turnPasswordText.getEditText().setInputType(
					InputType.TYPE_TEXT_VARIATION_PASSWORD);
			turnPasswordText
					.setDefaultValue(getString(R.string.turnPassword));
			root.addPreference(turnPasswordText);
		}

		if (active_preference_sync) {
			Preference resetButton = new Preference(this);
			resetButton.setKey(RESET_BUTTON);
			resetButton.setTitle(getString(R.string.preferences_sync));
			resetButton
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference arg0) {
							sendReset();
							return true;
						}
					});
			root.addPreference(resetButton);
		}

		if (active_preference_find_users) {
			Preference contactsButton = new Preference(this);
			contactsButton.setKey(CONTACTS_BUTTON);
			contactsButton.setTitle(getString(R.string.preferences_account));
			contactsButton
					.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference arg0) {
							sendContactsRequest();
							return true;
						}
					});
			root.addPreference(contactsButton);
		}

		if (active_preference_version) {
			EditTextPreference versionText = new EditTextPreference(this);
			versionText.setEnabled(false);
			versionText.setTitle(getString(R.string.preferences_version));
			versionText.setSummary(getString(R.string.version));
			root.addPreference(versionText);
		}

		if (active_preference_buil_num) {
			EditTextPreference buildNumberText = new EditTextPreference(this);
			buildNumberText.setEnabled(false);
			buildNumberText
					.setTitle(getString(R.string.preferences_build_number));
			buildNumberText.setSummary(getString(R.string.build_number));
			root.addPreference(buildNumberText);
		}

		return root;
	}

	protected void sendContactsRequest() {
		AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
		builder.setTitle(R.string.get_contacts_title);
		builder.setMessage(getString(R.string.get_contacts_text));
		builder.setPositiveButton(getString(R.string.get_contacts_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						AccountUtils.requestContacts(getApplicationContext());
					}
				});

		builder.setNegativeButton(getString(R.string.get_contacts_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
		builder.show();
	}

	protected void sendReset() {

		AlertDialog.Builder builder = new AlertDialog.Builder(Preferences.this);
		builder.setTitle(R.string.re_sync_title);
		builder.setMessage(getString(R.string.re_sync_text));
		builder.setPositiveButton(getString(R.string.re_sync_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						launchSync();
					}
				});

		builder.setNegativeButton(getString(R.string.re_sync_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Do nothing
					}
				});
		builder.show();
	}

	public void launchSync() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				CommandStoreService cs = new CommandStoreService(
						Preferences.this);
				try {
					boolean result = cs.createCommandReset("{}",
							Command.METHOD_FACTORY_RESET);

					if (result) {
						return ConstantKeys.SENDING_OK;
					} else {
						return ConstantKeys.SENDING_OFFLINE;
					}
				} catch (Exception e) {
					log.error("Error with command", e);
					return ConstantKeys.SENDING_FAIL;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				if (result.equals(ConstantKeys.SENDING_OK)) {
					AppUtils.purgeApp(Preferences.this);
					Preferences.this.finish();
				} else if (result.equals(ConstantKeys.SENDING_OFFLINE)) {
					Toast.makeText(
							Preferences.this.getApplicationContext(),
							Preferences.this.getApplicationContext().getText(
									R.string.upload_offline),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(
							Preferences.this.getApplicationContext(),
							Preferences.this.getApplicationContext().getText(
									R.string.re_sync_error), Toast.LENGTH_SHORT)
							.show();
				}
			}
		}.execute();
	}

	public static String getAccountId(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		String toReturn = pref.getString(SERVER_ACCOUNT_KEY,
				context.getString(R.string.account_id)).trim();

		if (ConstantKeys.STRING_DEFAULT.equals(toReturn)) {
			toReturn = context.getString(R.string.account_id);
		}
		return toReturn;
	}

	public static String getMediaTotalSize(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		String toReturn = pref.getString(MEDIA_TOTAL_SIZE,
				String.valueOf(context.getResources().getInteger(R.integer.media_total_size))).trim();

		if (ConstantKeys.STRING_DEFAULT.equals(toReturn)) {
			toReturn = String.valueOf(context.getResources().getInteger(R.integer.media_total_size));
		}
		return toReturn;
	}

	public static String getMaxMessages(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		String toReturn = pref.getString(MAX_MESSAGES,
				String.valueOf(context.getResources().getInteger(R.integer.max_messages))).trim();

		if (ConstantKeys.STRING_DEFAULT.equals(toReturn)) {
			toReturn = String.valueOf(context.getResources().getInteger(R.integer.max_messages));
		}
		return toReturn;
	}

	public static Boolean getInitCallWithVideo(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		return pref.getBoolean(INIT_CALL_WITH_VIDEO_KEY, !context.getResources().getBoolean(R.bool.videocall_init_video_off));
	}

	public static String getDefaultCamera(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		return pref.getString(DEFAULT_CAMERA_KEY, "front");
	}

	public static String getServerProtocol(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		return pref.getString(SERVER_PROTOCOL_KEY,
				context.getString(R.string.protocol)).trim();
	}

	public static String getServerAddress(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		return pref.getString(SERVER_ADDRESS_KEY,
				context.getString(R.string.host)).trim();
	}

	public static boolean isNotificationActivated(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getBoolean(NOTIFICATIONS_SOUNDS, true);
	}

	public static int getServerPort(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		String port = pref.getString(SERVER_PORT_KEY,
				context.getString(R.string.service_port));

		if (ConstantKeys.STRING_DEFAULT.equals(port)) {
			port = context.getString(R.string.service_port);
		}

		return Integer.parseInt(port);
	}

	// Get Account values
	public static boolean isUserAutoRegister(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);

		boolean result = pref.getBoolean(USER_AUTO_REGISTER, false);
		return result;
	}

	public static boolean isGroupAutoRegister(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean result = pref.getBoolean(GROUP_AUTO_REGISTER, false);
		return result;
	}

	public static String getAccountName(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(ACCOUNT_NAME,
				ConstantKeys.STRING_DEFAULT);
		return result;
	}

	public static String getInstanceId(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref
				.getString(INSTANCE_ID, ConstantKeys.STRING_DEFAULT);
		if (result.equals(ConstantKeys.STRING_DEFAULT)) {
			result = AppUtils.generateInstanceId(context);
			setInstanceId(context, result);
		}

		return result;
	}

	//Stun & Turn
	public static boolean getStunActivated(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean result = pref.getBoolean(STUN_ALLOW_KEY,
				AppUtils.getResource(context, R.bool.active_stun, true));

		return result;
	}

	public static String getStunAddress(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(STUN_ADDRESS_KEY,
				context.getResources().getString(R.string.stunHost));

		return result;
	}

	public static String getStunPort(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(STUN_PORT_KEY,
				context.getResources().getString(R.string.stunPort));

		return result;
	}

	public static String getStunUser(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(STUN_USER_KEY,
				context.getResources().getString(R.string.stunUser));

		return result;
	}

	public static String getStunPassword(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(STUN_PASSWORD_KEY,
				context.getResources().getString(R.string.stunPassword));

		return result;
	}

	public static boolean getTurnActivated(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean result = pref.getBoolean(TURN_ALLOW_KEY,
				AppUtils.getResource(context, R.bool.active_turn, true));

		return result;
	}

	public static String getTurnAddress(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(TURN_ADDRESS_KEY,
				context.getResources().getString(R.string.turnHost));

		return result;
	}

	public static String getTurnPort(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(TURN_PORT_KEY,
				context.getResources().getString(R.string.turnPort));

		return result;
	}

	public static String getTurnUser(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(TURN_USER_KEY,
				context.getResources().getString(R.string.turnUser));

		return result;
	}

	public static String getTurnPassword(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String result = pref.getString(TURN_PASSWORD_KEY,
				context.getResources().getString(R.string.turnPassword));

		return result;
	}

	public static boolean isGettingsCommandFail(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		return pref.getBoolean(GETTING_COMMAND_STATUS, true);
	}

	// Set account values
	public static void setUserAutoRegister(Context context, boolean value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putBoolean(USER_AUTO_REGISTER, value).commit();
	}

	public static void setGroupAutoRegister(Context context, boolean value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putBoolean(GROUP_AUTO_REGISTER, value).commit();
	}

	public static void setAccountName(Context context, String value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putString(ACCOUNT_NAME, value).commit();
	}

	public static void setInstanceId(Context context, String value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putString(INSTANCE_ID, value).commit();
	}

	public static void setAccountId(Context context, String value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putString(SERVER_ACCOUNT_KEY, value).commit();
	}

	public static void setGettingsCommandStatus(Context context, boolean value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		pref.edit().putBoolean(GETTING_COMMAND_STATUS, value).commit();
	}

	protected final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if ((key.equals(SERVER_ADDRESS_KEY))
					|| (key.equals(SERVER_PORT_KEY))
					|| (key.equals(SERVER_PROTOCOL_KEY))
					|| (key.equals(SERVER_ACCOUNT_KEY))) {
				reset = true;
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		AppUtils.CancelNotification(getApplicationContext());
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}
}
