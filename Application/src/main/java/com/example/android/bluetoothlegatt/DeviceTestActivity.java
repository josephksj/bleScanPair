package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.reflect.Method;
import android.os.Handler;
import android.os.Message;

import java.util.Set;


//public class DeviceTestActivity extends AppCompatActivity {
public class DeviceTestActivity extends Activity {

    final String TAG = "BLE_TEST";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String MSG_TEST_COUNT_KEY = "KSJ_C";
    private static final String MSG_SUCCESS_COUNT_KEY = "KSJ_S";
    private static final String MSG_ERROR_COUNT_KEY = "KSJ_E";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothManager mBluetoothManager=null;
    private BluetoothAdapter mBluetoothAdapter=null;
    private String mBluetoothDeviceAddress = null;
    private BluetoothGatt mBluetoothGatt=null;
    private int mConnectionState = STATE_DISCONNECTED;
    private Handler mHandler;
    private TextView mPariedStatTxt; //test_device_status_pair
    private boolean mTestInProgress = false;
    private TextView mTestInProgressCountTb;
    private TextView mTestSuccessCountTb;
    private TextView mTestFailedCountTb;


    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private static final int STATE_CONNECT_ERROR = 3;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_ERROR";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String msgString;
            msgString = bundle.getString(MSG_TEST_COUNT_KEY);
            if(msgString != null)
                mTestInProgressCountTb.setText(msgString);

            msgString = bundle.getString(MSG_SUCCESS_COUNT_KEY);
            if(msgString != null)
                mTestSuccessCountTb.setText(msgString);

            msgString = bundle.getString(MSG_ERROR_COUNT_KEY);
            if(msgString != null)
                mTestFailedCountTb.setText(msgString);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "DeviceTestActivity OnCreate-1");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_test);
        Log.d(TAG, "DeviceTestActivity OnCreate-2");
        getActionBar().setDisplayHomeAsUpEnabled(true);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        getActionBar().setTitle(mDeviceName);
        ((TextView) findViewById(R.id.test_device_address)).setText(mDeviceAddress);
        mPariedStatTxt = (TextView)findViewById(R.id.test_device_status_pair); //test_device_status_pair
        bleDevInitialize();
        mTestInProgressCountTb = (TextView)findViewById(R.id.connect_cur_test_count);
        mTestSuccessCountTb = (TextView)findViewById(R.id.connect_success_test_count);
        mTestFailedCountTb = (TextView)findViewById(R.id.connect_failure_test_count);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "DeviceTestActivity onResume");
        if(isDeviceIsPaired(mDeviceAddress)) {
            mPariedStatTxt.setText("Paired");
        } else {
            mPariedStatTxt.setText("NOT Paired");
        }
        mTestInProgress = false;
        mBluetoothDeviceAddress = mDeviceAddress;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "DeviceTestActivity onPause");
        flushCurrentConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DeviceTestActivity onDestroy");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "DeviceTestActivity onOptionsItemSelected");
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onButtonClicked(View view) {
        if (mBluetoothAdapter == null)
            return;
        switch (view.getId()) {
            case R.id.start_connect_test_button:
                if (mTestInProgress) {
                    Log.d(TAG, "WARNING: A test is progress ");
                    return;
                }
                mTestInProgress = true;
                String countStr = ((EditText)findViewById(R.id.connect_test_count)).getText().toString();
                int testCount;
                if(countStr.isEmpty()) {
                    ((EditText)findViewById(R.id.connect_test_count)).setText("1");
                    testCount = 1;
                } else {
                    Log.d(TAG, "DeviceTestActivity start_connect_test_button " + countStr);
                    testCount = Integer.parseInt(countStr);
                }
                doConnectDisconnect(testCount);
                break;
        }
    }

    public void doConnectDisconnect(int count) {
        final int loopCount = count;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int failCount=0;
                int successCount = 0;
                boolean doneFlag = false;
                for (int i = 0; i < loopCount; i++) {
                    waitMs(2000);
                    Log.d(TAG, "++++++ ThreadCount " + (i+1));
                    sendMessage(MSG_TEST_COUNT_KEY, " "+(i+1));
                    mConnectionState = STATE_DISCONNECTED;
                    doneFlag=false;
                    if(connect() == false) {
                        failCount++;
                        sendMessage(MSG_ERROR_COUNT_KEY,  " "+failCount);
                        Log.d(TAG, "Connect Failed "+(i+1));
                        continue;
                    }
                    //sendMessage(MSG_SUCCESS_COUNT_KEY, " "+((i+1)*2));
                    //sendMessage(MSG_ERROR_COUNT_KEY,  " "+((i+1)*3));
                    //Wait for the mConnectionState changed from the call back
                    for(int j=0; j<30; j++) {
                        waitMs(100);
                        if(mConnectionState == STATE_CONNECTED) {
                            successCount++;
                            sendMessage(MSG_SUCCESS_COUNT_KEY,  " "+successCount);
                            doneFlag=true;
                            break;
                        }
                    }
                    if(doneFlag==false) {
                        failCount++;
                        sendMessage(MSG_ERROR_COUNT_KEY,  " "+failCount);
                        Log.d(TAG, "Connect Wait TimedOut" + (i+1));

                        continue;
                    }
                    //------- Issue Disconnect Now ----- wait for mConnectionState = STATE_DISCONNECTED
                    doneFlag=false;
                    disconnect();
                    waitMs(1000);
                    close();
                    for(int j=0; j<30; j++) {
                        waitMs(100);
                        if(mConnectionState == STATE_DISCONNECTED) {
                             doneFlag=true;
                            break;
                        }
                    }
                    if(doneFlag==false) {
                        Log.d(TAG, "DisConnect Wait TimedOut" + (i+1));
                    }
                }
                mTestInProgress=false;
           }
        };
        Thread testThread = new Thread(r);
        testThread.start();
    }

    //Wait time should be Greater than 20ms
    public void waitMs(int timeInMs) {
        if(timeInMs < 20)
            return;
        long futureTime = System.currentTimeMillis() + timeInMs;
        synchronized (this) {
            try {
                wait(futureTime - System.currentTimeMillis());
            } catch (Exception e) {
            }
        }
    }

    public void sendMessage(String tag, String strMsg) {
        Message msg = msgHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString(tag, strMsg);
        msg.setData(bundle);
        msgHandler.sendMessage(msg);
    }

    public boolean bleDevInitialize() {
         if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mHandler = new Handler();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean isDeviceIsPaired(String address) {
        if(mBluetoothAdapter == null) return false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
         if (pairedDevices.size() > 0) {
             for (BluetoothDevice device : pairedDevices) {
                 String deviceHardwareAddress = device.getAddress();
                  if(deviceHardwareAddress.equals(address)){
                     return true;
                 }
             }
        }
        return false;
    }

    public boolean connect() {
        if (mBluetoothAdapter == null || mBluetoothDeviceAddress == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
         mBluetoothGatt = device.connectGatt(this, false, mGattCallback,
                BluetoothDevice.TRANSPORT_LE);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void flushCurrentConnection() {
        if (mBluetoothGatt == null) return;

    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    int bondstate = mBluetoothAdapter.getRemoteDevice(mBluetoothDeviceAddress).getBondState();
                    if(bondstate ==BluetoothDevice.BOND_NONE || bondstate == BluetoothDevice.BOND_BONDED) {
                        // Connected to device, proceed to discover it's services but delay a bit if needed
                        //final int delay = 1000;
                        final int delay = 10;
                        Runnable discoverServicesRunnable = new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "discovering services of with delay of " + delay + " ms");
                                boolean result = mBluetoothGatt.discoverServices();
                                if (!result) {
                                    Log.e(TAG, "discoverServices failed to start");
                                }
                                //discoverServicesRunnable = null;
                            }
                        };
                        mHandler.postDelayed(discoverServicesRunnable, delay);


                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        // Attempts to discover services after successful connection.
                        Log.i(TAG, "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());
                    } else if (bondstate == BluetoothDevice.BOND_BONDING) {
                        // Bonding process in progress, let it complete
                        Log.i(TAG, "waiting for bonding to complete");
                    }

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // We successfully disconnected on our own request
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    Log.i(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction);
                    mBluetoothGatt.close();
                } else {
                    //STATE_CONNECTING or STATE_DISCONNECTING, ignore for now
                }
            } else {
                //status == BluetoothGatt.GATT_ERROR
                intentAction = ACTION_GATT_ERROR;
                mConnectionState = STATE_CONNECT_ERROR;
                broadcastUpdate(intentAction);
                Log.i(TAG, "BluetoothGatt.GATT_ERROR  from GATT server.");
                mBluetoothGatt.close();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }



        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            Log.d(TAG, "In Callback onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Callback onCharacteristicWrite GATT_SUCCESS");
            }
        }

    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //Use asynchronous method, so give it some time to complete!
    private boolean clearServicesCache()
    {
        boolean result = false;
        try {
            Method refreshMethod = mBluetoothGatt.getClass().getMethod("refresh");
            if(refreshMethod != null) {
                result = (boolean) refreshMethod.invoke(mBluetoothGatt);
            }
        } catch (Exception e) {
            //HBLogger.e(TAG, "ERROR: Could not invoke refresh method");
        }
        return result;
    }

}
/*
//============ Queue based implementation ============
//make a simple queue by using a Runnable for every command.
// We first declare the queue using a Queue object and
// also declare a ‘lock’ variable to keep track whether an operation is in progress or not:
// add a new Runnable to the queue when we do a command.

private Queue<Runnable> commandQueue;
private boolean commandQueueBusy;
//Here is an example for the readCharacteristic command:
public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
    if(bluetoothGatt == null)  return false;
    if(characteristic == null)   return false;
    if((characteristic.getProperties() & PROPERTY_READ) == 0 )  return false;

    // Enqueue the read command now that all checks have been passed
    boolean result = commandQueue.add(new Runnable() {
        @Override
        public void run() {
            if(!bluetoothGatt.readCharacteristic(characteristic)) {
                Log.e(TAG, String.format("ERROR: readCharacteristic failed for characteristic: %s", characteristic.getUuid()));
                completedCommand();
            } else {
                Log.d(TAG, String.format("reading characteristic <%s>", characteristic.getUuid()));
                nrTries++;
            }
        }
    });

    if(result) {
        nextCommand();
    } else {
        Log.e(TAG, "ERROR: Could not enqueue read characteristic command");
    }
    return result;
}

//using a peek() to obtain the Runnable from the queue.
// That leaves the Runnable on the queue so we can retry it later if we have to
private void nextCommand() {
        // If there is still a command being executed then bail out
        if(commandQueueBusy) {
            return;
        }

        // Check if we still have a valid gatt object
        if (bluetoothGatt == null) {
            Log.e(TAG, String.format("ERROR: GATT is 'null' for peripheral '%s', clearing command queue", getAddress()));
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }

        // Execute the next command in the queue
        if (commandQueue.size() > 0) {
            final Runnable bluetoothCommand = commandQueue.peek();
            commandQueueBusy = true;
            nrTries = 0;

            bleHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothCommand.run();
                    } catch (Exception ex) {
                        Log.e(TAG, String.format("ERROR: Command exception for device '%s'", getName()), ex);
                    }
                }
            });
        }
}

//After the read is complete the result will come in on your callback:
// Note that call completedCommand() only after we are done processing the new value!
// This will make sure there is no other command running while we process the value and helps to avoid race conditions.
@Override
public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
        // Perform some checks on the status field
        if (status != GATT_SUCCESS) {
        Log.e(TAG, String.format(Locale.ENGLISH,"ERROR: Read failed for characteristic: %s, status %d", characteristic.getUuid(), status));
        completedCommand();
        return;
        }

        // Characteristic has been read so processes it
        ...    // We done, complete the command
        completedCommand();
}

//Then we complete this command, take the Runnable off the queue by calling poll() and start the next command in the queue:
private void completedCommand() {
        commandQueueBusy = false;
        isRetrying = false;
        commandQueue.poll();
        nextCommand();
}
//In some cases you may have to retry a command.
// We can do that easily since the Runnable is still on the queue.
// In order to make sure we don’t endlessly retry commands we also check if we have reached the retry limit:
private void retryCommand() {
    commandQueueBusy = false;
    Runnable currentCommand = commandQueue.peek();
    if(currentCommand != null) {
        if (nrTries >= MAX_TRIES) {
            // Max retries reached, give up on this one and proceed
            Log.v(TAG, "Max number of tries reached");
            commandQueue.poll();
        } else {
            isRetrying = true;
        }
    }
    nextCommand();
}



//================ JAVA Implementation for making sure only one activity at a time =====
//uses a lock variable called mDeviceBusy to determine if an operation can be started and sets it to true before doing the read
    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            return false;
        }

        if (VDBG) Log.d(TAG, "readCharacteristic() - uuid: " + characteristic.getUuid());
        if (mService == null || mClientIf == 0) return false;

        BluetoothGattService service = characteristic.getService();
        if (service == null) return false;

        BluetoothDevice device = service.getDevice();
        if (device == null) return false;

        synchronized (mDeviceBusy) {
            if (mDeviceBusy) return false;
            mDeviceBusy = true;
        }

        try {
            mService.readCharacteristic(mClientIf, device.getAddress(),
                    characteristic.getInstanceId(), AUTHENTICATION_NONE);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
            mDeviceBusy = false;
            return false;
        }

        return true;
    }

    //When the result of the read/write operation comes in the mDeviceBusy lock is set to false again:
    public void onCharacteristicRead(String address, int status, int handle, byte[] value) {
        if (VDBG) {
        Log.d(TAG, "onCharacteristicRead() - Device=" + address
        + " handle=" + handle + " Status=" + status);
        }

        if (!address.equals(mDevice.getAddress())) {
        return;
        }

    synchronized (mDeviceBusy) {
        mDeviceBusy = false;
        }....
*/