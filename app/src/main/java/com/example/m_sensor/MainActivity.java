package com.example.m_sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private RelativeLayout layout;
    private TextView warningTextView;
    private Vibrator vibrator;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashOn = false;
    private Handler handler = new Handler();
    private Runnable flashRunnable;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi komponen UI dan layanan sistem
        layout = findViewById(R.id.layout);
        warningTextView = findViewById(R.id.warningTextView);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cameraManager = (CameraManager)
                getSystemService(Context.CAMERA_SERVICE);
        mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.beep);
        // Mendapatkan ID kamera dan menangani kemungkinan eksepsi
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Inisialisasi sensor kedekatan
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor =
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        // Memeriksa ketersediaan sensor kedekatan
        if (proximitySensor == null) {
            warningTextView.setText("Proximity sensor not available");
        }
    }

    // Metode yang dipanggil saat aktivitas dilanjutkan
    @Override
    protected void onResume() {
        super.onResume();
        // Mendaftarkan listener sensor kedekatan
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // Metode yang dipanggil saat aktivitas dijeda
    @Override
    protected void onPause() {
        super.onPause();
        // Membatalkan pendaftaran listener sensor kedekatan
        if (proximitySensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Memeriksa jenis sensor
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            // Jika objek terlalu dekat
            if (distance < proximitySensor.getMaximumRange()) {
                layout.setBackgroundColor(Color.RED);
                warningTextView.setText("Object terlalu dekat!");
                // Getar dan bunyi peringatan
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (mediaPlayer != null) {
                        mediaPlayer.start();
                    }
// Getaran untuk versi Android O ke atas
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500,
                                VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        // Getaran untuk versi Android sebelum O
                        vibrator.vibrate(500);
                    }
                }
                startFlash(); // Menyalakan senter
            } else {
                // Jika objek jauh
                layout.setBackgroundColor(Color.WHITE);
                warningTextView.setText("Proximity Sensor Demo \n Dekatkan objek");
                stopFlash(); // Mematikan senter
            }
        }
    }

    // Metode untuk menyalakan senter
    private void startFlash() {
        flashRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    // Menyalakan senter untuk Android M ke atas
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraManager.setTorchMode(cameraId, true);
                        flashOn = true;
                        handler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.post(flashRunnable);
    }

    private void stopFlash() {
        handler.removeCallbacks(flashRunnable);
        try {
            // Mematikan senter untuk Android M ke atas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
                flashOn = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metode yang dipanggil saat aktivitas berhenti
    @Override
    protected void onStop() {
        super.onStop();
        // Mematikan senter jika masih menyala
        if (flashOn) {
            stopFlash();
        }
    }
    // Metode yang dipanggil saat aktivitas dihancurkan
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mematikan senter dan melepaskan media player jika masih aktif
        if (flashOn) {
            stopFlash();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}