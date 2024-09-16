package com.doguarikan.ble_attendence;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private static final String SERVICE_UUID = "532d9284-c4b6-47f4-a90c-973df1aced44";
    private static final String baseURL = "https://dummyjson.com/c/9860-814b-4b3b-8308/";

    private BluetoothAdapter bl_adapter; // bluetooth ozelliklerine erişim için
    private BluetoothLeAdvertiser ble_advertiser; // ble yayını için kullanılır
    private BluetoothLeScanner ble_scanner; // ble taraması için kullanılır
    private ScanCallback scan_callback; //ble sonuclarını işlemek icin kullanılır
    private TextView device_listTextView; // ekrana basılan hash kodunun yazıldığı yer
    private String hash_code = "";// kullanılan hash kodu, json bu dışarıdan gelecek ve advertise edilip, ogrenciden sisteme yollanacak
    private Runnable runnable;
    private Handler handler = new Handler();
    private Retrofit retrofit;
    private HashApi hashApi;
    private Call<Hash> hashCall;
    private Hash hash_instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        device_listTextView = findViewById(R.id.deviceList);
        Button advertiseButton = findViewById(R.id.buttonAdvertise);
        Button scanButton = findViewById(R.id.buttonScan);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bl_adapter = bluetoothManager.getAdapter();

        if (bl_adapter == null || !bl_adapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not supported or not enabled", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        hash_code = get_request();
        check_permissions();
        set_retrofit_settings();

        advertiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                advertise();
            }
        });
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan();
            }
        });
        //update_hash();
    }

    private void check_permissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }

    private void set_retrofit_settings() {// ????!!!!!
        retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        hashApi = retrofit.create(HashApi.class);
        hashCall = hashApi.get_hash();

        hashCall.enqueue(new Callback<Hash>() {
            @Override
            public void onResponse(Call<Hash> call, Response<Hash> response) {
                if(response.isSuccessful()) {
                    hash_instance = response.body();
                    hash_code = hash_instance.getHash_code();
                    device_listTextView.setText(hash_instance.getHash_code());
                }
            }

            @Override
            public void onFailure(Call<Hash> call, Throwable t) {
                System.out.println(t.toString());
            }
        });
    }

    private String get_request() {
        return "abc";
    }

    private void advertise() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) // haklara sahip olma kontrolu olmadan en alttaki startadvertise calısmıyor
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ble_advertiser == null)
            ble_advertiser = bl_adapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder() // blenin nasıl calıstıgını ayarlamak icin, dusuk gecikme, yuksek sıklık
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        byte[] hash_byte = hash_code.getBytes(); // ble ham byte yayını yapar

        AdvertiseData data = new AdvertiseData.Builder() // bunu yeniden araştır uuid olmadan dene !!!!!!!!!!!!!!!!???????????
                .addServiceData(new ParcelUuid(UUID.fromString(SERVICE_UUID)), hash_byte)//!!!!!!!!!!!?????????
                .build();

        AdvertiseCallback callback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Toast.makeText(MainActivity.this, "Advertise started successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Toast.makeText(MainActivity.this, "Advertise failed: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        };

        ble_advertiser.startAdvertising(settings, data, callback);
    }

    private void scan()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ble_scanner == null)
            ble_scanner = bl_adapter.getBluetoothLeScanner();

        scan_callback = new ScanCallback() { // bunu araştır!!!!!!!!??????????
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                byte[] scanRecord = result.getScanRecord().getServiceData(new ParcelUuid(UUID.fromString(SERVICE_UUID)));

                if (scanRecord != null) {
                    String receivedHashCode = new String(scanRecord);
                    update_devicelist(receivedHashCode + " - " + device.getAddress());

                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Toast.makeText(MainActivity.this, "Scan failed: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        };

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        ble_scanner.startScan(null, settings, scan_callback);
        Toast.makeText(this, "Scan started", Toast.LENGTH_SHORT).show();

    }

    private void update_devicelist(String device_info)
    {
        //runOnUiThread(() -> { // ble çalıştığı için aynı zamanda textview guncellensin diye thread gerekli
        String currentText = device_listTextView.getText().toString();
        device_listTextView.setText(currentText + "\n" + device_info);
        //});
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                advertise();
                scan();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

}