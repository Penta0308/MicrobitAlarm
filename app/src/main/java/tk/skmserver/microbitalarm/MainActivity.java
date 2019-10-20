package tk.skmserver.microbitalarm;

import android.bluetooth.*;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import tk.skmserver.microbitalarm.ui.main.SectionsPagerAdapter;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private Thread workerThread = null;
    private byte[] readBuffer;
    private int readBufferPosition;
    private TextView textViewReceive;
    private EditText editTextSend;
    private Button buttonSend;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        textViewReceive = (TextView)findViewById(R.id.textView_receive);

        editTextSend = (EditText)findViewById(R.id.editText_send);

        buttonSend = (Button)findViewById(R.id.button_send);
    }
}