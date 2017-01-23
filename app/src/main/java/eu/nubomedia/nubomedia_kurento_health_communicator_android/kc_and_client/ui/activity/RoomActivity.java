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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;

import org.java_websocket.WebSocketImpl;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ble.BLEManager;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ble.BLEService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.room.RoomPeer;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.AnalyticsBaseActivity;
import org.webrtc.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fi.vtt.nubomedia.kurentoroomclientandroid.KurentoRoomAPI;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomError;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomListener;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomNotification;
import fi.vtt.nubomedia.kurentoroomclientandroid.RoomResponse;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;

public class RoomActivity extends AnalyticsBaseActivity implements RoomListener, NBMWebRTCPeer.Observer, View.OnClickListener {

    protected Menu mMenu = null;

    public static String EXTRA_USER_NAME = "UserNameExtra";
    public static String EXTRA_ROOM_NAME = "RoomNameExtra";

    private String username, roomname;
    private String wsUri = "wss://q630f3c9c.apps.nubomedia-paas.eu:443/room";

    private LooperExecutor executor;
    private static KurentoRoomAPI kurentoRoomAPI;
    Handler mHandler;
    private int roomId = 0;
    private int constant = 0;

    private ConcurrentHashMap<Integer, Object> concurrentHashMap = new ConcurrentHashMap<Integer, Object>();

    private EglBase rootEglBase;

    private LinearLayout peersHorizontalLayout;
    private SurfaceViewRenderer mainRender;
    private NBMMediaConfiguration peerConnectionParameters;
    private NBMWebRTCPeer nbmWebRTCPeer;
    private DataChannel localDatachannel;
    private Boolean isLocalStreamInitiated;

    private HashMap<String, RoomPeer> peerDictionary = new HashMap<String, RoomPeer>();

    private RoomPeer selectedPeer;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private LinearLayout bleView;
    private ListView bleList;
    private TextView bleText;
    private BLEManager bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        username = getIntent().getStringExtra(EXTRA_USER_NAME);
        roomname = getIntent().getStringExtra(EXTRA_ROOM_NAME);

        WebSocketImpl.DEBUG = true;

        if(executor==null) {
            executor = new LooperExecutor();
            executor.requestStart();
        }

        isLocalStreamInitiated = false;

        mHandler = new Handler();

        mainRender = (SurfaceViewRenderer) findViewById(R.id.main_video_surface_container);
        peersHorizontalLayout = (LinearLayout) findViewById(R.id.remote_renderers_layout);

        // Create video renderers.
        rootEglBase = EglBase.create();
        mainRender.init(rootEglBase.getEglBaseContext(), null);
        mainRender.setZOrderMediaOverlay(true);
        mainRender.setMirror(false);
        mainRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        mainRender.requestLayout();

        final RoomPeer localPeer = addPeer(username, true);

        NBMMediaConfiguration.NBMVideoFormat receiverVideoFormat = new NBMMediaConfiguration.NBMVideoFormat(352, 288, PixelFormat.RGB_888, 20);
        peerConnectionParameters = new NBMMediaConfiguration(   NBMMediaConfiguration.NBMRendererType.OPENGLES,
                NBMMediaConfiguration.NBMAudioCodec.OPUS, 0,
                NBMMediaConfiguration.NBMVideoCodec.VP8, 0,
                receiverVideoFormat,
                NBMMediaConfiguration.NBMCameraPosition.FRONT);

        nbmWebRTCPeer = new NBMWebRTCPeer(peerConnectionParameters, RoomActivity.this, localPeer.getPeerViewRenderer(), RoomActivity.this);
        nbmWebRTCPeer.initialize();
        nbmWebRTCPeer.registerMasterRenderer(mainRender);

        selectedPeer = localPeer;

        if(kurentoRoomAPI==null) {
            kurentoRoomAPI = new KurentoRoomAPI(executor, wsUri, this);
        }

        registerReceiver(mBLEReceiver, bleFilter);
        bleView = (LinearLayout)findViewById(R.id.ble_view);
        bleText = (TextView)findViewById(R.id.ble_text);
        bleList = (ListView)findViewById(R.id.ble_list);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        bleList.setAdapter(mLeDeviceListAdapter);
        bleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bleText.setText("Connecting...");
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(i);
                bleManager.connectBLE(device);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myOnCreateOptionsMenu(menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected void myOnCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.room_menu, menu);
        mMenu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.add_peripheral) {
            if (localDatachannel != null) {
                bleManager = new BLEManager(this);
            } else {
                showToast("No datachannel available");
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        kurentoRoomAPI.sendLeaveRoom(roomId);

        nbmWebRTCPeer.stopLocalMedia();
        if ((nbmWebRTCPeer.isInitialized()) && (isLocalStreamInitiated)) {
            nbmWebRTCPeer.close();
        }

        nbmWebRTCPeer = null;

        kurentoRoomAPI.disconnectWebSocket();
        kurentoRoomAPI = null;

        unregisterReceiver(mBLEReceiver);
        if (bleManager != null) {

            bleManager.stopBLE();
        }
    }

    public void showToast(String string) {
        try {
            CharSequence text = string;
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(this, text, duration);
            toast.show();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void joinRoom () {
        if (kurentoRoomAPI != null) {
            constant = constant+1;
            roomId = constant;

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("type", "joinRoom");

            concurrentHashMap.put(constant, map);

            if (kurentoRoomAPI.isWebSocketConnected()) {
                kurentoRoomAPI.sendJoinRoom(this.username, this.roomname, true, roomId);
            }
        }
    }

    private RoomPeer addPeer(final String user, final Boolean localPeer) {
        final RelativeLayout peerLayout = new RelativeLayout(RoomActivity.this);
        peerLayout.setPadding(2, 0, 2, 0);
        peerLayout.setOnClickListener(RoomActivity.this);
        peerLayout.setTag(user);

        SurfaceViewRenderer peerRender = new SurfaceViewRenderer(RoomActivity.this);
        peerRender.init(rootEglBase.getEglBaseContext(), null);
        peerRender.setZOrderMediaOverlay(false);
        peerRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);

        peerRender.setMirror(false);

        peerRender.requestLayout();

        peerLayout.addView(peerRender, new RelativeLayout.LayoutParams(Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics())), RelativeLayout.LayoutParams.MATCH_PARENT));

        TextView nameView = new TextView(RoomActivity.this);
        nameView.setText(user);
        nameView.setBackgroundColor(getResources().getColor(R.color.light_grey));
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        peerLayout.addView(nameView, params);

        RoomPeer newPeer = new RoomPeer(peerRender, user, localPeer, peerLayout);
        peerDictionary.put(user, newPeer);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peersHorizontalLayout.addView(peerLayout, new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.MATCH_PARENT, HorizontalScrollView.LayoutParams.MATCH_PARENT));
            }
        });

        return newPeer;
    }

    //RoomListener
    @Override
    public void onRoomResponse(final RoomResponse response) {
        final HashMap <String, String> map = (HashMap<String, String>) concurrentHashMap.get(response.getId());
        if (map == null) {
            return;
        }
        switch (map.get("type")) {
            case "joinRoom":
                nbmWebRTCPeer.generateOffer(username, true);

                if (response.getValues() != null) {
                    for (int i = 0; i < response.getValues().size(); i++) {
                        String part = response.getValues().get(i).get("id");
                        addPeer(part, false);
                        nbmWebRTCPeer.generateOffer(part, false);
                    }
                }

                break;
            case "publishVideoRequest":
                if (response.getValues() == null) {
                    return;
                }
                List<String> ls = response.getValue("sdpAnswer");

                if ((ls == null) || (ls.size() == 0)) {
                    return;
                }

                SessionDescription sd = new SessionDescription(SessionDescription.Type.ANSWER,
                        response.getValue("sdpAnswer").get(0));
                nbmWebRTCPeer.processAnswer(sd, map.get("user"));

                break;
            case "sendIceCandidate":
                break;
        }

        concurrentHashMap.remove(response.getId());
    }

    @Override
    public void onRoomError(RoomError error) {
        Toast.makeText(this, "Room error", Toast.LENGTH_SHORT).show();
        this.finish();
    }

    @Override
    public void onRoomNotification(RoomNotification notification) {
        if(notification.getMethod().equals("participantJoined")) {
            Map<String, Object> map = notification.getParams();
            final String participantName = map.get("id").toString();

            addPeer(participantName, false);
        }

        if(notification.getMethod().equals("participantLeft")) {
            Map<String, Object> map = notification.getParams();
            final String participantName = map.get("name").toString();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RoomPeer peer = peerDictionary.remove(participantName);
                    peersHorizontalLayout.removeView(peer.getPeerLayout());
                }
            });

            if (participantName.equals(selectedPeer.getUser())) {
                RoomPeer localPeer = peerDictionary.get(username);

                nbmWebRTCPeer.setActiveMasterStream(localPeer.getStream());

                selectedPeer = localPeer;
            }
        }

        if(notification.getMethod().equals("participantPublished")) {
            Map<String, Object> map = notification.getParams();
            final String participantName = map.get("id").toString();

            nbmWebRTCPeer.generateOffer(participantName, false);
        }

        if(notification.getMethod().equals("iceCandidate"))
        {
            final Map<String, Object> map = notification.getParams();
            final String participantName = map.get("endpointName").toString();

            String sdpMid = map.get("sdpMid").toString();
            int sdpMLineIndex = Integer.valueOf(map.get("sdpMLineIndex").toString());
            String sdp = map.get("candidate").toString();

            IceCandidate ic = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
            nbmWebRTCPeer.addRemoteIceCandidate(ic, participantName);
        }
    }

    @Override
    public void onRoomConnected() {
        joinRoom();
    }

    @Override
    public void onRoomDisconnected() {
        finish();
    }

    @Override
    public void onInitialize() {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(getAssets().open("certificate.cer"));
            Certificate ca = cf.generateCertificate(caInput);
            kurentoRoomAPI.addTrustedCertificate("ca", ca);
        } catch (CertificateException|IOException e) {
            Log.e("Error", "Certificate error: " + e.toString());
        }

        kurentoRoomAPI.useSelfSignedCertificate(true);

        if (!kurentoRoomAPI.isWebSocketConnected()) {
            kurentoRoomAPI.connectWebSocket();
        }
    }

    //NBMWebRTCPeer.Observer
    @Override
    public void onLocalSdpOfferGenerated(SessionDescription sdpOffer, final NBMPeerConnection connection) {
        constant = constant + 1;

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("type", "publishVideoRequest");
        map.put("user", connection.getConnectionId());

        concurrentHashMap.put(constant, map);

        if (connection.getConnectionId().equals(username)) {
            localDatachannel = connection.getDataChannel("default");

            isLocalStreamInitiated = true;

            kurentoRoomAPI.sendPublishVideo(sdpOffer.description, true, constant);
        } else {
            final SessionDescription remoteSdp = sdpOffer;

            if (kurentoRoomAPI != null) {
                String sender = connection.getConnectionId() + "_webcam";
                kurentoRoomAPI.sendReceiveVideoFrom(sender, connection.getConnectionId(), remoteSdp.description, constant);
            }
        }
    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {
        //NADA
    }

    @Override
    public void onIceCandidate(final IceCandidate iceCandidate, final NBMPeerConnection connection) {
        constant = constant +1;

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("type", "sendIceCandidate");
        map.put("user", connection.getConnectionId());

        concurrentHashMap.put(constant, map);

        kurentoRoomAPI.sendOnIceCandidate(connection.getConnectionId(), iceCandidate.sdp,
                iceCandidate.sdpMid, Integer.toString(iceCandidate.sdpMLineIndex), constant);
    }

    @Override
    public void onIceStatusChanged(PeerConnection.IceConnectionState state, NBMPeerConnection connection) {
        if (connection.getConnectionId().equals(username)) {
            if (state.equals(PeerConnection.IceConnectionState.FAILED)) {
                //this.finish();
            }
        }
    }

    @Override
    public void onRemoteStreamAdded(final MediaStream stream, NBMPeerConnection connection) {
        String connectionId = connection.getConnectionId();
        final RoomPeer rp = peerDictionary.get(connectionId);
        rp.setStream(stream);

        if (rp.isMine()) {
            nbmWebRTCPeer.setActiveMasterStream(stream);
        } else {
            nbmWebRTCPeer.attachRendererToRemoteStream(rp.getPeerViewRenderer(), stream);
        }
    }

    @Override
    public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {
    }

    @Override
    public void onPeerConnectionError(String error) {
    }

    @Override
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {
    }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection nbmPeerConnection, DataChannel dataChannel) {
    }

    @Override
    public void onStateChange(NBMPeerConnection nbmPeerConnection, DataChannel dataChannel) {
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection nbmPeerConnection, DataChannel dataChannel) {
    }

    public void cancelBLE(View loginButton) {
        bleManager.stopBLE();
        bleView.setVisibility(View.GONE);
        mLeDeviceListAdapter.clear();
    }

    private IntentFilter bleFilter = new IntentFilter(
            BLEService.BLE_BROADCAST);
    private BroadcastReceiver mBLEReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getExtras().getBoolean(BLEService.BLE_CONNECTED)) {
                showToast("BLE Connected");
                bleView.setVisibility(View.GONE);
                mLeDeviceListAdapter.clear();
            }

            if (intent.getExtras().getBoolean(BLEService.BLE_DISCONNECTED)) {
                showToast("BLE Disconnected");
                bleManager.stopBLE();
                bleView.setVisibility(View.GONE);
                mLeDeviceListAdapter.clear();
            }

            if (intent.getExtras().getBoolean("BLE_ENABLED")) {
                bleText.setText("Searching peripherals...");
                bleView.setVisibility(View.VISIBLE);
                bleManager.initBLE();
            }

            if (intent.getExtras().getBoolean("BLE_NOT_ENABLED")) {
                showToast("Bluetooth NOT on");
            }

            if (intent.getExtras().getBoolean("BLE_NEW_DEVICE")) {
                BluetoothDevice device = (BluetoothDevice) intent.getExtras().get("BLE_DEVICE");

                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }

            if (intent.getExtras().getBoolean(BLEService.BLE_DATA)) {
                String ox = intent.getExtras().getString(BLEService.OXYGEN_DATA);
                String puls = intent.getExtras().getString(BLEService.PULSE_DATA);

                String strToSend = "Oxygen: " + ox + " Pulse: " + puls;

                //final ByteBuffer buffer = ByteBuffer.wrap(strToSend.getBytes());
                final ByteBuffer buffer = ByteBuffer.wrap(puls.getBytes());

                if (localDatachannel == null) {
                    return;
                }
                localDatachannel.send(new DataChannel.Buffer(buffer, false));
            }
        }
    };

    @Override
    public void onClick(View view) {
        RoomPeer p = peerDictionary.get(view.getTag().toString());
        MediaStream ms = p.getStream();

        if (ms != null) {
            nbmWebRTCPeer.setActiveMasterStream(ms);

            selectedPeer = p;
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = RoomActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown device");

            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}
