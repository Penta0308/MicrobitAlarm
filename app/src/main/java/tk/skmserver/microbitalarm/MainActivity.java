package tk.skmserver.microbitalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public TextView textView_btstate;
    public static BluetoothManager bluetoothManager;
    protected String targetMAC;

    class colorSwitchListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                bluetoothManager.openSerialDevice(targetMAC);
                textView_btstate.setText("Connected");
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

        Switch connect = findViewById(R.id.switch_connect);
        connect.setOnCheckedChangeListener(new colorSwitchListener());

        textView_btstate = findViewById(R.id.textView_btstate);

        List<BluetoothDevice> pairedDevices = MainActivity.bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("My Bluetooth App", "Device name: " + device.getName());
            Log.d("My Bluetooth App", "Device MAC Address: " + device.getAddress());
            if(device.getName().compareTo(getText(R.string.device_name).toString()) == 0) targetMAC = device.getAddress();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothManager.closeDevice(targetMAC);
    }
}
