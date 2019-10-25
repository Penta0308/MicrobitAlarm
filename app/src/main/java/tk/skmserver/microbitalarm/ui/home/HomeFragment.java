package tk.skmserver.microbitalarm.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import java.util.ArrayList;

import tk.skmserver.microbitalarm.BluetoothSerial;
import tk.skmserver.microbitalarm.MainActivity;
import tk.skmserver.microbitalarm.R;

public class HomeFragment extends Fragment {

    public TextView textBTstate;
    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        Switch connect = root.findViewById(R.id.switch_connect);
        class colorSwitchListener implements CompoundButton.OnCheckedChangeListener{
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    MainActivity.bluetoothSerial.connect();
                    textBTstate.setText("Connected");
                } else {
                    MainActivity.bluetoothSerial.close();
                    textBTstate.setText("Disconnected");
                }
            }
        }

        textBTstate = root.findViewById(R.id.textView_BTstate);
        return root;
    }
}