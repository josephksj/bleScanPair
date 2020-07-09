
Android BluetoothLeGatt Sample
==============================

This repo has been migrated to [github.com/android/connectivity][1]. Please check that repo for future updates. Thank you!

[1]: https://github.com/android/connectivity

KSJ cloned it from https://github.com/googlearchive/android-BluetoothLeGatt.git:
adb commands
Command from adb shell used for the testing, should see the response on the adb shell, can filter the message using TAG=BLE_TEST
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'start_scan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'stop_scan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'lsscan'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'lspair'"

adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'connect <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'disconnect'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'indication <UUID>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'pair N <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'pair A <address from the scan result>'"

adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'unpair N <name from the scan result>'"
adb shell "am broadcast -a com.example.android.bluetoothlegatt.TEST_ACTION --es com.example.android.bluetoothlegatt.EXTRA_TEXT 'unpair A <address from the scan result>'"
