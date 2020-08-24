package com.range.task;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity {

    private static final int REQUEST_SCREENSHOT = 59706;
    private MediaProjectionManager mgr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        requestPermissions();
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                1
        );
        startActivityForResult(mgr.createScreenCaptureIntent(),
                REQUEST_SCREENSHOT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==REQUEST_SCREENSHOT) {
            if (resultCode==RESULT_OK) {
                Intent i=
                        new Intent(this, ScreenshotService.class)
                                .putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(ScreenshotService.EXTRA_RESULT_INTENT, data);

                startService(i);
            }
        }

        finish();
    }
}