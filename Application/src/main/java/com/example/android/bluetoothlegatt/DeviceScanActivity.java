/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private boolean mConnectionInProgress;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    final String TAG = "BLE_TEST";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "DeviceScanActivity  onCreate");
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        dispPairedDevice();
        registerReceiver(adbCmdReceiver, adbCmdReceiverIntentFilter());

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(adbCmdReceiver, filter);

        final IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(adbCmdReceiver, bondFilter);

        mConnectionInProgress = false;
    }

    public void dispPairedDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        Log.d(TAG, "======== Paired Devices Start ===========");
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceHardwareAddress+ "  "+ deviceName);
            }
        }
        Log.d(TAG, "======== Paired Devices End ===========");

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DeviceScanActivity  onDestroy");
        unregisterReceiver(adbCmdReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
        mConnectionInProgress = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        int devType = device.getType();
        Log.d(TAG, "========= BluetoothType: "+devType); //1-Classic, 2-LE, 3-Dual
/*
        Boolean isBonded = false;
        isBonded = device.createBond();
        if(isBonded) {
            Log.d(TAG, "========= createBond Retrun OK");
        } else {
            Log.d(TAG, "========= createBond Retrun FAILED");
        }
*/
       // pairDevice(device);


        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
    private void pairDevice_1(BluetoothDevice device) {
        String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
        Intent intent = new Intent(ACTION_PAIRING_REQUEST);
        String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
        intent.putExtra(EXTRA_DEVICE, device);
        String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
        int PAIRING_VARIANT_PIN = 0;
        intent.putExtra(EXTRA_PAIRING_VARIANT, PAIRING_VARIANT_PIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    private void pairDevice(BluetoothDevice device) {
        try {
            Log.d(TAG, "Start Pairing...");
            Method m = device.getClass()
                    .getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d(TAG, "Pairing finished.");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void scanLeDevice(final boolean enable) {
        Log.d(TAG, "In scanLeDevice");
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    displayScanResults();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private void displayScanResults() {
        Log.d("displayScanResults", "Joseph Kizhakkeparampil");
         int count = mLeDeviceListAdapter.getCount();
        for(int i=0; i<count; i++) {
             BluetoothDevice curDev = mLeDeviceListAdapter.getDevice(i);
            if(curDev.getName() != null) {
                Log.d(TAG, "Device-" + i + " " + curDev.getAddress() + " " + curDev.getName());
            } else {
                Log.d(TAG, "Device-" + i + " " + curDev.getAddress() + " Unknown Device");
            }
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
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
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private final BroadcastReceiver adbCmdReceiver = new BroadcastReceiver() {
         @Override
        public void onReceive(Context context, Intent intent) {
            //final String action = intent.getAction();
            Log.i(TAG, "KSJ In onReceive for Intent");
             String action = intent.getAction();
            if("com.example.android.bluetoothlegatt.TEST_ACTION".equals(action)){
                String rxText = intent.getStringExtra("com.example.android.bluetoothlegatt.EXTRA_TEXT");
                Log.i(TAG, "RxedCmdText: "+rxText);
                if("start_scan".equals(rxText)) {
                    scanLeDevice(true);
                } else if("stop_scan".equals(rxText)) {
                    scanLeDevice(false);
                } else if(!rxText.isEmpty()){
                    String delims = "[ ]+";
                    String[] tokens = rxText.split(delims);
                    //for (int i = 0; i < tokens.length; i++)
                    //    Log.i(TAG, tokens[i]);
                    if(tokens[0].equals("connect")) {
                        connectDevice(tokens);
                    } else if(tokens[0].equals("disconnect")) {
                        disconnectDevice(tokens);
                    } else if(tokens[0].equals("pair")) {
                        pairDeviceWithAddr(tokens);
                    }else if(tokens[0].equals("unpair")) {
                        unpairDeviceWithAddr(tokens);
                    }else if(tokens[0].equals("lspair")) {
                        dispPairedDevice();
                    }else if(tokens[0].equals("lsscan")) {
                        displayScanResults();
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int previousState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "<<<ACTION_BOND_STATE_CHANGED: state:>>> " + state + ", previous:" + previousState);
                //listener.onDevicePairingEnded();
                // BOND_BONDED = 12
                // BOND_BONDING = 11
                // BOND_NONE = 10
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_CONNECTED:  >>> " );
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_DISCONNECTED:  >>> " );
            }else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Log.d(TAG, "<<<ACTION_ACL_DISCONNECT_REQUESTED:  >>> " );
            }else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1);
                Log.d(TAG, "<<<ACTION_PAIRING_REQUEST:  >>> " + pairingKey); // 0-Pin, 2-PassKey
            }
        }
    };



    private void pairDeviceWithAddr(String[] tokens) {
        if(tokens.length < 2) {
            Log.i(TAG, "Invalid param for Pairing");
            return;
        }
        Log.i(TAG, "pairDeviceWithAddr: "+tokens[1]);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(tokens[1]);
        if (device == null) {
            Log.i(TAG, "Pairing Device Not Detected: "+tokens[1]);
            return;
        }
        pairDevice(device);
    }

    private void unpairDeviceWithAddr(String[] tokens) {
        if(tokens.length < 2) {
            Log.i(TAG, "Invalid param for Pairing");
            return;
        }
        Log.i(TAG, "unpairDeviceWithAddr: "+tokens[1]);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(tokens[1]);
        if (device == null) {
            Log.i(TAG, "Unpairing Device Not Detected: "+tokens[1]);
            return;
        }
        unpairDevice(device);
    }




    private void connectDevice(String[] tokens) {
        if(mConnectionInProgress) {
            Log.i(TAG, "Connected or Connection In Progress");
            return;
        }
        if(tokens.length < 2) {
            Log.i(TAG, "Invalid param for connect");
            return;
        }
        Log.i(TAG, "Connecting to: "+tokens[1]);

        final Intent intentController = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        if(tokens.length == 3) {
            intentController.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, tokens[2]);
        } else {
            intentController.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, "Unknown Device");
        }
        intentController.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, tokens[1]);
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intentController);
        mConnectionInProgress = true;

    }

    private void disconnectDevice(String[] tokens) {
        if(!mConnectionInProgress) {
            Log.i(TAG, "Not Connected or Not In Progress");
            return;
        }

    }

    private static IntentFilter adbCmdReceiverIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        //intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        //intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction("com.example.android.bluetoothlegatt.TEST_ACTION");
        return intentFilter;
    }

}