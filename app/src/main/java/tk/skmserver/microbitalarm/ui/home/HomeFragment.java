package tk.skmserver.microbitalarm.ui.home;

import android.bluetooth.*;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.BinderThread;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import tk.skmserver.microbitalarm.MainActivity;
import tk.skmserver.microbitalarm.R;

import static tk.skmserver.microbitalarm.MainActivity.REQUEST_ENABLE_BT;

public class HomeFragment extends Fragment {

    public TextView BTstate;

    class colorSwitchListener implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked)
                BTstate.setTextColor(Color.RED);
            else
                BTstate.setTextColor(Color.BLACK);
        }
    }

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
        BTstate = root.findViewById(R.id.textView_BTstate);

        connect.setOnCheckedChangeListener(new colorSwitchListener());
// 블루투스 활성화하기

        MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터를 디폴트 어댑터로 설정



        if(MainActivity.bluetoothAdapter == null) { // 디바이스가 블루투스를 지원하지 않을 때

            // 여기에 처리 할 코드를 작성하세요.

        }

        else { // 디바이스가 블루투스를 지원 할 때

            if(MainActivity.bluetoothAdapter.isEnabled()) { // 블루투스가 활성화 상태 (기기에 블루투스가 켜져있음)

                MainActivity.selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출

            }

            else { // 블루투스가 비 활성화 상태 (기기에 블루투스가 꺼져있음)

                // 블루투스를 활성화 하기 위한 다이얼로그 출력

                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                // 선택한 값이 onActivityResult 함수에서 콜백된다.

                startActivityForResult(intent, REQUEST_ENABLE_BT);

            }

        }

        return root;
    }
}