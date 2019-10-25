package tk.skmserver.microbitalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import nl.bravobit.ffmpeg.FFmpeg;

public class MainActivity extends AppCompatActivity {

    public TextView textView_btstate;
    public static BluetoothManager bluetoothManager;
    protected String targetMAC;
    private SimpleBluetoothDeviceInterface deviceInterface;

    private void onConnected(BluetoothSerialDevice connectedDevice) {
        textView_btstate.setText("Connected");
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        deviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);

        // Let's send a message:
        deviceInterface.sendMessage("Hello world!");
    }

    private void onMessageSent(String message) {
        // We sent a message! Handle it here.
        Toast.makeText(getApplicationContext(), "Sent a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.
    }

    private void onMessageReceived(String message) {
        // We received a message! Handle it here.
        Toast.makeText(getApplicationContext(), "Received a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.
    }

    private void onError(Throwable error) {
        // Handle the error
        Log.e("Microbit Alarm", "Error: " + error.getMessage());
        if(error.getMessage().equals("java.io.IOException: read failed, socket might closed or timeout, read ret: -1")) {
            Switch connect = findViewById(R.id.switch_connect);
            connect.setChecked(false);
            textView_btstate.setText("Disconnected");
        }
    }

    class colorSwitchListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                bluetoothManager.openSerialDevice(targetMAC)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(MainActivity.this::onConnected, MainActivity.this::onError);
                ProgressBar pbar = findViewById(R.id.progressBar);
            } else {
                bluetoothManager.closeDevice(targetMAC);
                textView_btstate.setText("Disconnected");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(getApplicationContext(), "Bluetooth not available.", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            finish();
        }

        if (!FFmpeg.getInstance(this).isSupported()) {
            Toast.makeText(getApplicationContext(), "FFmpeg not available.", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            finish();
        }

        Switch connect = findViewById(R.id.switch_connect);
        connect.setOnCheckedChangeListener(new colorSwitchListener());

        textView_btstate = findViewById(R.id.textView_btstate);

        List<BluetoothDevice> pairedDevices = MainActivity.bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("Microbit Alarm", "Device name: " + device.getName());
            Log.d("Microbit Alarm", "Device MAC Address: " + device.getAddress());
            if(device.getName().equals(getText(R.string.device_name).toString())){
                targetMAC = device.getAddress();
                Log.d("My Bluetooth App", "Target Device MAC Address: " + targetMAC);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.closeDevice(targetMAC);
    }
}
