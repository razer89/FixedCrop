package com.soundcloud.android.crop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PickAvatarActivity extends Activity {
    public static final int REQUEST_CROP_IMAGE = 667;
    public static final int RESULT_NO_FIND_APPS = -321;
    private static final String EXTRA_MAX_WIDTH = "max_width";
    private static final String EXTRA_TITLE = "chooser_title";
    private static final String EXTRA_MAX_HEIGHT = "max_height";
    private static final String EXTRA_ASPECT_X = "aspect_x";
    private static final String EXTRA_ASPECT_Y = "aspect_y";
    public static final String EXTRA_DEST_DIR = "dest_uri";
    private File mTempCameraFile;
    private int mMaxWidth, mMaxHeight, mAspectX, mAspectY;

    /**
     * @param activity
     * @param maxSize      for avatar square side
     * @param chooserTitle
     */
    public static void pick(Activity activity, int maxSize, String chooserTitle) {
        pick(activity, maxSize, maxSize, 1, 1, chooserTitle);
    }


    /**
     * Start with default requestCode = REQUEST_CROP_IMAGE
     */
    public static void pick(Activity activity, int maxWidth, int maxHeight, int aspectX, int aspectY, String chooserTitle) {
        pick(REQUEST_CROP_IMAGE, activity, maxWidth, maxHeight, aspectX, aspectY, chooserTitle);
    }

    public static void pick(int requestCode, Activity activity, int maxWidth, int maxHeight, int aspectX, int aspectY, String chooserTitle) {
        Intent intent = createIntent(activity, maxWidth, maxHeight, aspectX, aspectY, chooserTitle);
        activity.startActivityForResult(intent, requestCode);
    }

    public static Intent createIntent(Context context, int maxWidth, int maxHeight, int aspectX, int aspectY, String chooserTitle) {
        Intent intent = new Intent(context, PickAvatarActivity.class);
        intent.putExtra(EXTRA_MAX_WIDTH, maxWidth);
        intent.putExtra(EXTRA_MAX_HEIGHT, maxHeight);
        intent.putExtra(EXTRA_ASPECT_X, aspectX);
        intent.putExtra(EXTRA_ASPECT_Y, aspectY);
        intent.putExtra(EXTRA_TITLE, chooserTitle);
        return intent;
    }

    public static void addDestDir(Intent intent, String path) {
        if (!PickAvatarActivity.class.getCanonicalName().equals(intent.getComponent().getClassName())) {
            throw new IllegalArgumentException("Intent's target must be PickAvatarActivity");
        }
        intent.putExtra(EXTRA_DEST_DIR, path);
    }

    public List<Intent> addIntentsToList(List<Intent> list, Intent intent) {
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo) {
            String packageName = resolveInfo.activityInfo.packageName;
            Intent targetedIntent = new Intent(intent);
            targetedIntent.setPackage(packageName);
            list.add(targetedIntent);
        }
        return list;
    }

    private void startPickChooser(String title) {
        Intent chooserIntent;

        List<Intent> intentList = new ArrayList<>();

        Intent pickIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempCameraFile));
        intentList = addIntentsToList(intentList, takePhotoIntent);
        intentList = addIntentsToList(intentList, pickIntent);

        if (intentList.size() > 0) {
            chooserIntent = Intent.createChooser(intentList.remove(intentList.size() - 1),
                    title);
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Parcelable[]{}));
            startActivityForResult(chooserIntent, Crop.REQUEST_PICK);
        } else {
            setResult(RESULT_NO_FIND_APPS);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMaxWidth = getIntent().getIntExtra(EXTRA_MAX_WIDTH, 0);
        mMaxHeight = getIntent().getIntExtra(EXTRA_MAX_HEIGHT, 0);
        mAspectX = getIntent().getIntExtra(EXTRA_ASPECT_X, 0);
        mAspectY = getIntent().getIntExtra(EXTRA_ASPECT_Y, 0);
        getExternalCacheDir().mkdirs();
        mTempCameraFile = new File(getExternalCacheDir(), "image.jpg");

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (savedInstanceState == null) {
            mTempCameraFile.delete();
            startPickChooser(title);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode != RESULT_OK) {
            finish();
            return;
        }
        switch (requestCode) {
            case Crop.REQUEST_PICK:
                if (mTempCameraFile.exists() && mTempCameraFile.length() > 0) {
                    //from camera
                    beginCrop(Uri.fromFile(mTempCameraFile));
                } else {
                    beginCrop(result.getData());
                }
                break;
            case Crop.REQUEST_CROP:
                handleCrop(result);
                break;
        }
    }

    private void beginCrop(Uri source) {
        String destDir = getIntent().getStringExtra(EXTRA_DEST_DIR);
        Uri destination;
        if (destDir != null) {
            destination = Uri.fromFile(new File(destDir));
        } else {
            destination = Uri.fromFile(new File(getCacheDir(), String.valueOf(System.currentTimeMillis())));
        }
        Crop crop = Crop.of(source, destination);
        if (mAspectX > 0 && mAspectY > 0) {
            crop.withAspect(mAspectX, mAspectY);
        }
        if (mMaxWidth > 0 && mMaxHeight > 0) {
            crop.withMaxSize(mMaxWidth, mMaxHeight);
        }
        crop.start(this);
    }

    private void handleCrop(Intent result) {
        Intent intent = new Intent();
        intent.setData(Crop.getOutput(result));
        setResult(RESULT_OK, intent);
        finish();
    }
}
