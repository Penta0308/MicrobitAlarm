package tk.skmserver.microbitalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

public class MainActivity extends AppCompatActivity {

    public TextView textView_btstate;
    public static BluetoothManager bluetoothManager;
    protected String targetMAC;
    private SimpleBluetoothDeviceInterface deviceInterface;
    final static int UPLOADFILEOPEN_REQUESTCODE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

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

    class colorSwitchListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                bluetoothManager.openSerialDevice(targetMAC)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(MainActivity.this::onConnected, MainActivity.this::onError);
            } else {
                bluetoothManager.closeDevice(targetMAC);
                textView_btstate.setText("Disconnected");
            }
        }
    }

    class uploadButtonListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(Intent.createChooser(i, getString(R.string.music_opentitle)), UPLOADFILEOPEN_REQUESTCODE);
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

        verifyStoragePermissions(this);

        Switch connect = findViewById(R.id.switch_connect);
        connect.setOnCheckedChangeListener(new colorSwitchListener());

        Button upload = findViewById(R.id.button_upload);
        upload.setOnClickListener(new uploadButtonListener());

        textView_btstate = findViewById(R.id.textView_btstate);

        List<BluetoothDevice> pairedDevices = MainActivity.bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("Microbit Alarm", "Device name: " + device.getName());
            Log.d("Microbit Alarm", "Device MAC Address: " + device.getAddress());
            if(device.getName().equals(getText(R.string.device_name).toString())){
                targetMAC = device.getAddress();
                Log.d("Microbit Alarm", "Target Device MAC Address: " + targetMAC);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Microbit Alarm", "requestCode: " + requestCode);
        Log.d("Microbit Alarm", "resultCode: " + resultCode);
        if(requestCode == UPLOADFILEOPEN_REQUESTCODE) {
            if(resultCode == -1) {
                Log.d("Microbit Alarm", "Path: " + data.getDataString());
                String [] command = {"-i", URLFilepath.getPath(getApplicationContext(), data.getData()), "-c:a", "pcm_u8", "-ac", "1", "-ar", "32500", Environment.getExternalStorageDirectory().getAbsolutePath() + "/testfile.wav"};
                FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
                try {
                    // to execute "ffmpeg -version" command you just need to pass "-version"
                    ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {}

                        @Override
                        public void onProgress(String message) {
                            Log.d("Microbit Alarm", message);
                        }

                        @Override
                        public void onFailure(String message) {
                            Log.d("Microbit Alarm", message);
                        }

                        @Override
                        public void onSuccess(String message) {
                            Log.d("Microbit Alarm", message);
                        }

                        @Override
                        public void onFinish() {}

                    });
                } catch (FFmpegCommandAlreadyRunningException e) {
                    // Handle if FFmpeg is already running
                }
            } else return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.closeDevice(targetMAC);
    }
}

