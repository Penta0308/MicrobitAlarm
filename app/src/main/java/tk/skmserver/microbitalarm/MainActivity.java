package tk.skmserver.microbitalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import org.jetbrains.annotations.NotNull;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import nl.bravobit.ffmpeg.ExecuteBinaryResponseHandler;
import nl.bravobit.ffmpeg.FFmpeg;
import nl.bravobit.ffmpeg.exceptions.FFmpegCommandAlreadyRunningException;

public class MainActivity extends AppCompatActivity {
    private TextView textView_btstate;
    private static BluetoothManager bluetoothManager;
    protected String targetMAC;
    private static SimpleBluetoothDeviceInterface deviceInterface;
    final static int UPLOADFILEOPEN_REQUESTCODE = 1;
    final static int BLOCKSIZE = 4096;
    private TransmitTask tt;
    protected boolean isconnected = false;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private class TransmitTask extends AsyncTask<Intent, Integer, Integer> {
        FileInputStream fp;
        BufferedInputStream bi;
        int filesize;
        boolean ready = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            File fop = new File(getCacheDir().getAbsolutePath() + "/encoded.raw");
            if(!fop.exists()) return;
            filesize = (int)fop.length();
            Log.d("Microbit Alarm", String.valueOf(filesize));
            try {
                fp = new FileInputStream(getCacheDir().getAbsolutePath() + "/encoded.raw");
            } catch(IOException e) {
                e.printStackTrace();
            }
            bi = new BufferedInputStream(fp);
            ProgressBar pbar = findViewById(R.id.progressBar_upload);
            pbar.setMax(filesize);
        }

        @Override
        protected Integer doInBackground(Intent... data) {
            // 전달된 URL 사용 작업
                for(int n = 0; n * BLOCKSIZE < filesize; n++) {
                    byte[] readraw = new byte[BLOCKSIZE];
                    try {
                        bi.read(readraw);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    String readydata = Base64.encodeToString(readraw, Base64.DEFAULT);
                    Log.d("Microbit Alarm", String.valueOf(n * BLOCKSIZE));
                    setpbar(n * BLOCKSIZE);
                    if(n == 0)
                        deviceInterface.sendMessage(";m " + String.valueOf((int)Math.ceil(filesize / BLOCKSIZE) * BLOCKSIZE) + " " + String.valueOf((int)Math.ceil(filesize / BLOCKSIZE)) +"\r\n");
                    deviceInterface.sendMessage(";d " + String.valueOf(n) + " " + readydata + "\r\n");
                    publishProgress(n);
                    deviceInterface.setMessageSentListener(message -> onFileMessageSent(message));
                    while(!ready);
                    ready = false;
                }
            return 0;
        }

        @Override
        protected void onProgressUpdate(@NotNull Integer... progress) { setpbar((progress[0]) * BLOCKSIZE);  }

        @Override
        protected void onPostExecute(Integer result) {
            try {
                bi.close();
                fp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            deviceInterface.setMessageSentListener(message -> MainActivity.this.onMessageSent(message));
        }

        protected void onFileMessageSent(String message) {
            ready = true;
        }
    }

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

    private void onConnected(@NotNull BluetoothSerialDevice connectedDevice) {
        textView_btstate.setText("Connected");
        isconnected = true;
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        deviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);

        // Let's send a message:
        //deviceInterface.sendMessage("Hello world!\r\n");
    }

    private void onMessageSent(String message) {
        // We sent a message! Handle it here.
        //Toast.makeText(getApplicationContext(), "Sent a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.
        Toast.makeText(getApplicationContext(), "Sent a message!", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
    }

    private void onMessageReceived(String message) {
        // We received a message! Handle it here.
        Toast.makeText(getApplicationContext(), "Received a message! Message was: " + message, Toast.LENGTH_LONG).show(); // Replace context with your context instance.
    }

    private void onError(@NotNull Throwable error) {
        // Handle the error
        Log.e("Microbit Alarm", "Error: " + error.getMessage());
        if(error.getMessage().equals("java.io.IOException: read failed, socket might closed or timeout, read ret: -1")) {
            Switch connect = findViewById(R.id.switch_connect);
            connect.setChecked(false);
            textView_btstate.setText("Disconnected");
            isconnected = false;
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
                isconnected = false;
            }
        }
    }

    class uploadButtonListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            if(!isconnected) {
                Toast.makeText(getApplicationContext(), "Turn On Bluetooth First", Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("audio/*");
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivityForResult(Intent.createChooser(i, getString(R.string.music_opentitle)), UPLOADFILEOPEN_REQUESTCODE);
        }
    }

    class lightButtonListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            if(R.id.button_on == view.getId()) {
                if(!isconnected) {
                    Toast.makeText(getApplicationContext(), "Turn On Bluetooth First", Toast.LENGTH_LONG).show();
                    return;
                }
                deviceInterface.sendMessage(";n\r\n");
            } else if(R.id.button_off == view.getId()) {
                if(!isconnected) {
                    Toast.makeText(getApplicationContext(), "Turn On Bluetooth First", Toast.LENGTH_LONG).show();
                    return;
                }
                deviceInterface.sendMessage(";f\r\n");
            }
        }
    }

    class alarmButtonListener implements Button.OnClickListener {
        @Override
        public void onClick(View view) {
            if(!isconnected) {
                Toast.makeText(getApplicationContext(), "Turn On Bluetooth First", Toast.LENGTH_LONG).show();
                return;
            }
            showTime();
        }
    }

    void showTime() {
        Calendar nday = Calendar.getInstance();
        nday.setTimeInMillis(System.currentTimeMillis());
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int h, int m) {
                Calendar day = Calendar.getInstance();
                nday.setTimeInMillis(System.currentTimeMillis());
                day.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DATE), h, m);
                if(day.getTimeInMillis() < nday.getTimeInMillis()) day.set(Calendar.DAY_OF_YEAR, day.get(Calendar.DAY_OF_YEAR) + 1);
                deviceInterface.sendMessage(";a " + String.valueOf((day.getTimeInMillis() - nday.getTimeInMillis()) / 1000) + "\r\n");
            }
        }, nday.get(Calendar.HOUR_OF_DAY), nday.get(Calendar.MINUTE), true);
        timePickerDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
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

        Button on = findViewById(R.id.button_on);
        on.setOnClickListener(new lightButtonListener());

        Button off = findViewById(R.id.button_off);
        off.setOnClickListener(new lightButtonListener());

        Button alarm = findViewById(R.id.button_alarm);
        alarm.setOnClickListener(new alarmButtonListener());

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

    protected void onPause() {
        super.onPause();
//        bluetoothManager.closeDevice(targetMAC);
    }

    protected void setpbar(int a) {
        ProgressBar pbar = findViewById(R.id.progressBar_upload);
        pbar.setProgress(a);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Microbit Alarm", "requestCode: " + requestCode);
        Log.d("Microbit Alarm", "resultCode: " + resultCode);
        if(requestCode == UPLOADFILEOPEN_REQUESTCODE) {
            if(resultCode == -1) {
                Log.d("Microbit Alarm", "Path: " + data.getDataString());
                String [] command = {"-i", URLFilepath.getPath(getApplicationContext(), data.getData()), "-c:a", "pcm_u8", "-ac", "1", "-f", "u8", "-ar", "32500", "-y", getCacheDir().getAbsolutePath() + "/encoded.raw"};
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
                            tt = new TransmitTask();
                            tt.execute(data);
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

    protected void onResume() {
        super.onResume();
/*        bluetoothManager.openSerialDevice(targetMAC)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this::onConnected, MainActivity.this::onError);*/
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.closeDevice(targetMAC);
    }
}

