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

public class MainActivity extends AppCompatActivity {

    public TextView textBTstate;
    public static BluetoothManager bluetoothManager;

    class colorSwitchListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                textBTstate.setText("Connected");
            } else {
                textBTstate.setText("Disconnected");
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

        textBTstate = findViewById(R.id.textView_btstate);

        List<BluetoothDevice> pairedDevices = MainActivity.bluetoothManager.getPairedDevicesList();
        for (BluetoothDevice device : pairedDevices) {
            Log.d("My Bluetooth App", "Device name: " + device.getName());
            Log.d("My Bluetooth App", "Device MAC Address: " + device.getAddress());
        }
    }
}
