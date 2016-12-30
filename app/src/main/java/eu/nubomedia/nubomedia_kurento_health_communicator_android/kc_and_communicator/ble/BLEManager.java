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

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

public class BLEManager {

    private Activity activity;
    private static BluetoothAdapter mBluetoothAdapter;
    private BLEService mBluetoothLeService;

    private BtBroadcastReceiver mReceiver;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public BLEManager(Activity activity) {
        this.activity = activity;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //BLE not supported
        }

        if (mBluetoothAdapter == null) {
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent("BROADCAST_BLE");
            intent.putExtra("BLE_NOT_ENABLED", true);

            activity.sendBroadcast(intent);
        } else {
            Intent gattServiceIntent = new Intent(activity, BLEService.class);
            activity.bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);

            mReceiver = new BtBroadcastReceiver();
            IntentFilter ifilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            activity.registerReceiver(mReceiver, ifilter);
        }
    }

    public void initBLE() {
        mBluetoothAdapter.startDiscovery();
    }

    public void stopBLE() {
        try {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            activity.unbindService(mServiceConnection);
            mBluetoothAdapter = null;

            activity.unregisterReceiver(mReceiver);
        } catch (Exception e) {
        }
    }

    public void connectBLE(BluetoothDevice device) {
        mBluetoothLeService.connect(device.getAddress());
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                //Unable to initialize Bluetooth
            } else {
                Intent intent = new Intent("BROADCAST_BLE");
                intent.putExtra("BLE_ENABLED", true);

                activity.sendBroadcast(intent);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private class BtBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //may need to chain this to a recognizing function
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    Intent i = new Intent("BROADCAST_BLE");
                    i.putExtra("BLE_NEW_DEVICE", true);
                    i.putExtra("BLE_DEVICE", device);

                    activity.sendBroadcast(i);
                }
            }
        }
    }
}
