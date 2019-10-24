package tk.skmserver.microbitalarm;

import android.bluetooth.*;
import android.content.DialogInterface;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_music)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }
    public static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태

    public static BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터

    public static Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋

    public static BluetoothDevice bluetoothDevice; // 블루투스 디바이스

    public static BluetoothSocket bluetoothSocket = null; // 블루투스 소켓

    public OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림

    public InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림

    public Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드

    public byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼

    public int readBufferPosition; // 버퍼 내 문자 저장 위치

    public void selectBluetoothDevice() {

        // 이미 페어링 되어있는 블루투스 기기를 찾습니다.

        devices = bluetoothAdapter.getBondedDevices();

        // 페어링 된 디바이스의 크기를 저장

        int pairedDeviceCount = devices.size();

        // 페어링 되어있는 장치가 없는 경우

        if(pairedDeviceCount == 0) {

            // 페어링을 하기위한 함수 호출

        }

        // 페어링 되어있는 장치가 있는 경우

        else {

            // 디바이스를 선택하기 위한 다이얼로그 생성

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");

            // 페어링 된 각각의 디바이스의 이름과 주소를 저장

            List<String> list = new ArrayList<>();

            // 모든 디바이스의 이름을 리스트에 추가

            for(BluetoothDevice bluetoothDevice : devices) {

                list.add(bluetoothDevice.getName());

            }

            list.add("취소");



            // List를 CharSequence 배열로 변경

            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);

            list.toArray(new CharSequence[list.size()]);



            // 해당 아이템을 눌렀을 때 호출 되는 이벤트 리스너

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {

                @Override

                public void onClick(DialogInterface dialog, int which) {

                    // 해당 디바이스와 연결하는 함수 호출

                    connectDevice(charSequences[which].toString());

                }

            });



            // 뒤로가기 버튼 누를 때 창이 안닫히도록 설정

            builder.setCancelable(false);

            // 다이얼로그 생성

            AlertDialog alertDialog = builder.create();

            alertDialog.show();

        }

    }

    public void connectDevice(String deviceName) {

        // 페어링 된 디바이스들을 모두 탐색

        for(BluetoothDevice tempDevice : devices) {

            // 사용자가 선택한 이름과 같은 디바이스로 설정하고 반복문 종료

            if(deviceName.equals(tempDevice.getName())) {

                bluetoothDevice = tempDevice;

                break;

            }

        }

        // UUID 생성

        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        try {

            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);

            bluetoothSocket.connect();

            // 데이터 송,수신 스트림을 얻어옵니다.

            outputStream = bluetoothSocket.getOutputStream();

            inputStream = bluetoothSocket.getInputStream();

            // 데이터 수신 함수 호출

            //receiveData();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}
