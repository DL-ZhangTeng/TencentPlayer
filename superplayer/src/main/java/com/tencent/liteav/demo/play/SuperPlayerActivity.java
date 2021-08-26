package com.tencent.liteav.demo.play;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.tencent.liteav.demo.play.v3.SuperPlayerVideoId;
import com.tencent.rtmp.TXLiveConstants;

import java.util.ArrayList;

/**
 * Created by liyuejiao on 2018/7/3.
 * 超级播放器主Activity
 */

public class SuperPlayerActivity extends Activity implements SuperPlayerView.OnSuperPlayerViewCallback {
    private SuperPlayerView mSuperPlayerView;
    private VideoModel videoModel;
    private boolean onlyPlay;

    /**
     * @param view view
     * @param url  视频数据
     */
    public static void startVideoActivity(View view, String url) {
        view.getContext().startActivity(
                new Intent(view.getContext(), SuperPlayerActivity.class).putExtra("videoUrl", url).putExtra("onlyPlay", true),
                ActivityOptions.makeSceneTransitionAnimation((Activity) view.getContext(), view, "myvideo").toBundle());
    }

    /**
     * @param view       view
     * @param videoModel 视频数据
     */
    public static void startVideoActivity(View view, VideoModel videoModel) {
        view.getContext().startActivity(
                new Intent(view.getContext(), SuperPlayerActivity.class).putExtra("videoModel", videoModel).putExtra("onlyPlay", false),
                ActivityOptions.makeSceneTransitionAnimation((Activity) view.getContext(), view, "myvideo").toBundle());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 延伸显示区域到刘海
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
            // 设置页面全屏显示
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_supervod_player);
        Intent intent = getIntent();
        if (intent.hasExtra("videoModel")) {
            videoModel = (VideoModel) intent.getSerializableExtra("videoModel");
        } else if (intent.hasExtra("videoUrl")) {
            videoModel = new VideoModel();
            videoModel.title = " ";
            videoModel.appid = (int) System.currentTimeMillis();
            videoModel.videoURL = intent.getStringExtra("videoUrl");
        }
        if (intent.hasExtra("onlyPlay")) {
            onlyPlay = intent.getBooleanExtra("onlyPlay", true);
        }
        mSuperPlayerView = findViewById(R.id.superVodPlayerView);
        if (onlyPlay) {
            mSuperPlayerView.setOnlyPlay(view -> finish());
        }
        mSuperPlayerView.setShowBackWhenSmall(true);
        mSuperPlayerView.setDanmu(false);
        mSuperPlayerView.setPlayerViewCallback(this);
        SuperPlayerGlobalConfig prefs = SuperPlayerGlobalConfig.getInstance();
        prefs.enableFloatWindow = false;
        // 播放器默认缓存个数
        prefs.maxCacheItem = 5;
        // 设置播放器渲染模式
        prefs.enableHWAcceleration = true;
        prefs.renderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mSuperPlayerView.getPlayState() == SuperPlayerConst.PLAYSTATE_PLAY) {
            Log.i("SuperPlayerActivity", "onResume state :" + mSuperPlayerView.getPlayState());
            mSuperPlayerView.onResume();
            if (mSuperPlayerView.getPlayMode() == SuperPlayerConst.PLAYMODE_FLOAT) {
                mSuperPlayerView.requestPlayMode(SuperPlayerConst.PLAYMODE_WINDOW);
            }
        }
        playVideoModel(videoModel);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("SuperPlayerActivity", "onPause state :" + mSuperPlayerView.getPlayState());
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSuperPlayerView.release();
        if (mSuperPlayerView.getPlayMode() != SuperPlayerConst.PLAYMODE_FLOAT) {
            mSuperPlayerView.resetPlayer();
        }
    }

    private void playVideoModel(VideoModel videoModel) {
        final SuperPlayerModel superPlayerModelV3 = new SuperPlayerModel();
        superPlayerModelV3.appId = videoModel.appid;

        if (!TextUtils.isEmpty(videoModel.videoURL)) {
            superPlayerModelV3.title = videoModel.title;
            superPlayerModelV3.url = videoModel.videoURL;
            superPlayerModelV3.qualityName = "原画";

            superPlayerModelV3.multiURLs = new ArrayList<>();
            if (videoModel.multiVideoURLs != null) {
                for (VideoModel.VideoPlayerURL modelURL : videoModel.multiVideoURLs) {
                    superPlayerModelV3.multiURLs.add(new SuperPlayerModel.SuperPlayerURL(modelURL.url, modelURL.title));
                }
            }
        } else if (!TextUtils.isEmpty(videoModel.fileid)) {
            superPlayerModelV3.videoId = new SuperPlayerVideoId();
            superPlayerModelV3.videoId.fileId = videoModel.fileid;
        }
        try {
            int brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            Window window = this.getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            if (brightness == -1) {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            } else {
                lp.screenBrightness = (brightness <= 0 ? 1 : brightness) / 255f;
            }
            window.setAttributes(lp);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        mSuperPlayerView.playWithModel(superPlayerModelV3);
    }

    private boolean isLivePlay(VideoModel videoModel) {
        String videoURL = videoModel.videoURL;
        if (TextUtils.isEmpty(videoModel.videoURL)) {
            return false;
        }
        if (videoURL.startsWith("rtmp://")) {
            return true;
        } else
            return (videoURL.startsWith("http://") || videoURL.startsWith("https://")) && videoURL.contains(".flv");
    }

    @Override
    public void onStartFullScreenPlay() {

    }

    @Override
    public void onStopFullScreenPlay() {

    }

    @Override
    public void onClickFloatCloseBtn() {
        mSuperPlayerView.resetPlayer();
        finish();
    }

    @Override
    public void onClickSmallReturnBtn() {
        finish();
    }

    @Override
    public void onStartFloatWindowPlay() {
        // 开始悬浮播放后，直接返回到桌面，进行悬浮播放
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    @Override
    public void onPlayTime(long time) {

    }

    @Override
    public void onDanMuCheck(boolean check) {

    }
}
