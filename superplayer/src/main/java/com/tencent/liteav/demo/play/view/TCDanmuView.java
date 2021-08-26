package com.tencent.liteav.demo.play.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.tencent.liteav.basic.log.TXCLog;
import com.tencent.liteav.demo.play.R;
import com.tencent.liteav.demo.play.utils.DensityUtil;

import java.util.Random;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * Created by liyuejiao on 2018/1/29.
 * 全功能播放器中的弹幕View
 */
public class TCDanmuView extends DanmakuView {
    private Context mContext;
    private DanmakuContext mDanmakuContext;
    private boolean mShowDanmu;
    private HandlerThread mHandlerThread;
    private DanmuHandler mDanmuHandler;
    private boolean danmu = false;
    private BackgroundCacheStuffer mBackgroundCacheStuffer = new BackgroundCacheStuffer();

    public TCDanmuView(Context context) {
        super(context);
        init(context);
    }

    public TCDanmuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TCDanmuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {
        mContext = context;
        enableDanmakuDrawingCache(true);
        // 设置最大显示行数
//        HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
//        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 10); // 滚动弹幕最大显示5行
//        // 设置是否禁止重叠
//        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
//        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
//        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);
        setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                mShowDanmu = true;
                start();
                generateDanmaku();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });
        mDanmakuContext = DanmakuContext.create();
        mDanmakuContext
                .setCacheStuffer(mBackgroundCacheStuffer, null)
        // 绘制背景使用BackgroundCacheStuffer
        ;
        prepare(mParser, mDanmakuContext);
    }

    @Override
    public void release() {
        super.release();
        mShowDanmu = false;
        if (mDanmuHandler != null) {
            mDanmuHandler.removeCallbacksAndMessages(null);
            mDanmuHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }

    private BaseDanmakuParser mParser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };

    /**
     * 随机生成一些弹幕内容以供测试
     */
    private void generateDanmaku() {
        mHandlerThread = new HandlerThread("Danmu");
        mHandlerThread.start();
        mDanmuHandler = new DanmuHandler(mHandlerThread.getLooper());
    }

    /**
     * 向弹幕View中添加一条弹幕
     *
     * @param content    弹幕的具体内容
     * @param withBorder 弹幕是否有边框
     */
    public void addDanmaku(String content, boolean withBorder) {
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL);
        if (danmaku != null) {
            DanmakuTag danmakuTag = new DanmakuTag();
            danmakuTag.bitmapResId = R.drawable.bg_live_danmu;
            danmaku.setTag(danmakuTag);
            danmaku.text = content;
            danmaku.padding = DensityUtil.dip2px(mContext, 4);
            danmaku.textSize = DensityUtil.sp2px(mContext, 13.0f);
            danmaku.textColor = Color.WHITE;
            danmaku.isLive = true;
            danmaku.setTime(getCurrentTime());
            if (withBorder) {
                danmaku.borderColor = getResources().getColor(R.color.color_6);
            }
            addDanmaku(danmaku);
        }
    }

    public void toggle(boolean on) {
        if (mDanmuHandler == null) {
            generateDanmaku();
        }
        TXCLog.i(TAG, "onToggleControllerView on:" + on);
        danmu = on;
        if (on) {
            mDanmuHandler.sendEmptyMessageAtTime(DanmuHandler.MSG_SEND_DANMU, 100);
        } else {
            mDanmuHandler.removeMessages(DanmuHandler.MSG_SEND_DANMU);
        }
    }


    public void send(String content) {
        if (danmu) {
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("content", content);  //往Bundle中存放数据
            //  bundle.putString("time", danmutime);  //往Bundle中put数据
            message.setData(bundle);
            message.what = DanmuHandler.MSG_SEND_DANMU;
            int time = new Random().nextInt(2000);
            mDanmuHandler.sendMessageDelayed(message, time);
        } else {
            mDanmuHandler.removeMessages(DanmuHandler.MSG_SEND_DANMU);
        }
    }

    public class DanmuHandler extends Handler {
        public static final int MSG_SEND_DANMU = 1001;

        public DanmuHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SEND_DANMU:
                    if (msg.getData().getString("content") != null) {
                        sendDanmu(msg.getData().getString("content"));
                    }

                    break;

            }
        }

        private void sendDanmu(String content) {
            addDanmaku(content, false);
        }
    }

    private class DanmakuTag {
        public int bitmapResId;
    }

    private class BackgroundCacheStuffer extends SpannedCacheStuffer {
        @Override
        public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
//            danmaku.padding = 50; // 在背景绘制模式下增加padding
            super.measure(danmaku, paint, fromWorkerThread);
        }

        @Override
        public void drawBackground(BaseDanmaku danmaku, Canvas canvas, float left, float top) {
            Object object = danmaku.tag;
            if (object instanceof DanmakuTag) {
                DanmakuTag danmakuTag = (DanmakuTag) object;

                Drawable drawable = ContextCompat.getDrawable(mContext, danmakuTag.bitmapResId);

                float height = danmaku.paintHeight;
                float width = danmaku.paintWidth;

                Rect rect = new Rect(0, 0, (int) width, (int) height);
                drawable.setBounds(rect);
                drawable.draw(canvas);
            }
        }

        @Override
        public void drawStroke(BaseDanmaku danmaku, String lineText, Canvas canvas, float left,
                               float top, Paint paint) {
            // 禁用描边绘制
        }
    }

}
