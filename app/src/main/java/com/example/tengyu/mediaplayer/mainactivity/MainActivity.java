package com.example.tengyu.mediaplayer.mainactivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.tengyu.mediaplayer.R;
import com.example.tengyu.mediaplayer.listviewactivity.ListviewActivity;
import com.example.tengyu.mediaplayer.listviewactivity.MusicData;
import com.example.tengyu.mediaplayer.listviewactivity.ReaderMusic;

import java.io.IOException;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private ImageView startl,Mode,mView;
    private List<MusicData> listDatas;
    private SeekBar mSeekBar;
    private TextView mTitle,mArtist;
    private Visualizer mVisualizer;//
    private LinearLayout mLinearLayout;

    public static MyVisualizerView mVisualizerView;
    boolean isChanging=false;
    int mpostion;
    int mStop = 0;//停止按钮的判断
    int mMode = 0;//播放方式

    //进度条
    class MySeekBar implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            isChanging = true;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            mPlayer.seekTo(mSeekBar.getProgress());
            isChanging = false;
        }

    }

    //新线程调节进度条
    private Handler mhandler = new Handler(){
        public void handleMessage (Message msg)
        {
            if (msg.what == 0){

            }else if (msg.what == 1){
                if(mPlayer == null){
                    return;
                }
                mSeekBar.setMax(mPlayer.getDuration());//设置进度条
                mSeekBar.setProgress(mPlayer.getCurrentPosition());
                mhandler.sendEmptyMessageDelayed(1, 1000);
            }
        }

    };

    //新线程刷新波形图
    private Handler setupVisualizer = new Handler() {
        public void handleMessage (Message msg){
            if (msg.what == 0){

            }else if (msg.what == 1){
                // 以MediaPlayer的AudioSessionId创建Visualizer
                // 相当于设置Visualizer负责显示该MediaPlayer的音频数据
                mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
                mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                // 为mVisualizer设置监听器
                mVisualizer.setDataCaptureListener(
                        new Visualizer.OnDataCaptureListener()
                        {
                            @Override
                            public void onFftDataCapture(Visualizer visualizer,
                                                         byte[] fft, int samplingRate)
                            {
                            }
                            @Override
                            public void onWaveFormDataCapture(Visualizer visualizer,
                                                              byte[] waveform, int samplingRate)
                            {
                                // 用waveform波形数据更新mVisualizerView组件
                                mVisualizerView.updateVisualizer(waveform);
                            }
                        },Visualizer.getMaxCaptureRate() / 2, true, false);
                mVisualizer.setEnabled(true);
            }

        }
    };

    private static class MyVisualizerView extends View
    {
        //bytes数组保存了波形抽样点的值
        private byte[] bytes;
        private float[] points;
        private Paint paint = new Paint();
        private Rect rect = new Rect();
        private byte type = 0;
        public MyVisualizerView(Context context)
        {
            super(context);
            bytes= null;
            //设置画笔的属性
            paint.setStrokeWidth(5f);
            paint.setAntiAlias(true);
            paint.setColor(Color.argb(125,129,191,215));
            paint.setStyle(Paint.Style.FILL);
        }
        public void updateVisualizer(byte[] ftt)
        {
            bytes= ftt;
            //通知该组件重绘自己
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent me)
        {
            // 当用户触碰该组件时，切换波形类型
            if(me.getAction()!= MotionEvent.ACTION_DOWN)
            {
                return false;
            }
            type++;
            if(type>= 3)
            {
                type = 0;
            }
            return true;
        }
        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            if(bytes == null)
            {
                Log.i("-------------", "bytes没有调用");
                return;
            }
            //绘制白色背景（主要为了印刷时好看）
            canvas.drawColor(Color.argb(0,255,255,255));
            //使用rect对象记录该组件的宽度和高度
            rect.set(0,0,getWidth(),getHeight());
            switch(type)
            {
                // -------绘制块状的波形图-------
                case 0:
                    for (int i = 0; i < bytes.length - 1; i++)
                    {
                        float left = getWidth()* i / (bytes.length - 1);
                        // 根据波形值计算该矩形的高度
                        float top =rect.height()-(byte)(bytes[i+1]+128)
                                * rect.height() / 128;
                        float right = left + 1;
                        float bottom =rect.height();
                        canvas.drawRect(left,top, right, bottom, paint);
                    }
                    break;
                // -------绘制柱状的波形图（每隔18个抽样点绘制一个矩形）-------
                case 1:
                    for (int i = 0; i < bytes.length - 1; i += 18)
                    {
                        float left =rect.width()*i/(bytes.length - 1);
                        // 根据波形值计算该矩形的高度
                        float top =rect.height()-(byte)(bytes[i+1]+128)
                                * rect.height() /128;
                        float right = left + 6;
                        float bottom =rect.height();
                        canvas.drawRect(left, top, right,bottom, paint);
                    }
                    break;
                // -------绘制曲线波形图-------
                case 2:
                    // 如果points数组还未初始化
                    if (points == null || points.length < bytes.length * 4)
                    {
                        points = new float[bytes.length * 4];
                    }
                    for (int i = 0; i < bytes.length - 1; i++)
                    {
                        // 计算第i个点的x坐标
                        points[i * 4] =rect.width()*i/(bytes.length - 1);
                        // 根据bytes的值（波形点的值）计算第i个点的y坐标
                        points[i * 4 + 1] =(rect.height() / 2)
                                + ((byte) (bytes[i]+ 128)) * 128
                                / (rect.height() /2);
                        // 计算第i+1个点的x坐标
                        points[i * 4 + 2] =rect.width() * (i + 1)
                                / (bytes.length -1);
                        // 根据bytes[i+1]的值（波形点的值）计算第i+1个点的y坐标
                        points[i * 4 + 3] =(rect.height() / 2)
                                + ((byte) (bytes[i+ 1] + 128)) * 128
                                / (rect.height() /2);
                    }
                    // 绘制波形曲线
                    canvas.drawLines(points, paint);
                    break;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayer = new MediaPlayer();
        mTitle = (TextView) findViewById(R.id.textViewSong);
        mArtist = (TextView) findViewById(R.id.textViewSinger);
        startl =  (ImageView) findViewById(R.id.start);
        Mode = (ImageView) findViewById(R.id.btWays);
        mView = (ImageView) findViewById(R.id.mImageView);
        mSeekBar = (SeekBar) findViewById(R.id.pgbTime);
        mSeekBar.setOnSeekBarChangeListener(new MySeekBar());
        mLinearLayout = (LinearLayout) findViewById(R.id.layout);
        // 创建MyVisualizerView组件，用于显示波形图
        mVisualizerView = new MyVisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int)(120f * getResources().getDisplayMetrics().density)));
        // 将MyVisualizerView组件添加到layout容器中
        mLinearLayout.addView(mVisualizerView);

        listDatas = ReaderMusic.getInstance(this).readerLocalMusic();



        //完成歌曲监听

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                if(mMode == 0){//顺序播放
                    mpostion++;
                    if(mpostion>=listDatas.size()){
                        mpostion = 0;
                    }
                    mMusic(mpostion);

                }else if (mMode == 1){//随机播放

                    mpostion = (int)(Math.random()*listDatas.size());
                    mMusic(mpostion);

                }else if (mMode == 2){//循环播放
                    mMusic(mpostion);
                }
            }
        });

        //Eoor监听
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                mPlayer.stop();

                Log.i("---------1", "mMusic: 跳歌了= =");
                Log.i("---------2", "Url: "+listDatas.get(mpostion).getUrl());
                return false;
            }
        });

        //prepaer监听
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mhandler.sendEmptyMessage(0);
                mhandler.sendEmptyMessageDelayed(1,1000);
                setupVisualizer.sendEmptyMessage(0);
                setupVisualizer.sendEmptyMessageDelayed(1,1000);
                mPlayer.start();
            }
        });

    }

    //播放暂停
    public void Start (View v) {

        switch (mStop) {
            case 0:
                break;
            case 1:
                if (mPlayer.isPlaying()) {
                    startl.setBackgroundResource(R.drawable.start);
                    mPlayer.pause();
                } else {
                    startl.setBackgroundResource(R.drawable.pause);
                    mPlayer.start();
                }
                break;

        }
    }

    //下一首
    public void Next (View v){

        switch (mMode){
            case 1:
                mpostion = (int)(Math.random()*listDatas.size());
                mMusic(mpostion);
                break;
            case 0:
                mpostion = mpostion+1;
                if(mpostion >= listDatas.size()){
                    mpostion = 0;
                }
                mMusic(mpostion);
                break;
            case 2:
                mpostion = mpostion+1;
                if(mpostion >= listDatas.size()){
                    mpostion = 0;
                }
                mMusic(mpostion);
                break;
        }
        startl.setBackgroundResource(R.drawable.pause);
    }

    //上一首
    public void Last (View v){
        if(mpostion < 1){
            mpostion = listDatas.size()-1;
            mMusic(mpostion);
        }else{
            mpostion = mpostion-1;
            if (mpostion < 0 ){
                mpostion = listDatas.size();
            }
            mMusic(mpostion);
            startl.setBackgroundResource(R.drawable.pause);
        }
    }

    //播放模式
    public void MusicMode(View v){

        switch (mMode){
            case 0:
                Mode.setBackgroundResource(R.drawable.suiji);//随机
                mMode = mMode + 1;
                break;
            case 1:
                Mode.setBackgroundResource(R.drawable.xuanhuan);//单曲循环
                mMode = mMode + 1;
                break;
            case 2:
                Mode.setBackgroundResource(R.drawable.shunxu);//列表循环
                mMode = mMode - 2;
                break;
        }
    }

    //播放列表
    public void Listview (View v){
        Intent intent = new Intent(MainActivity.this,ListviewActivity.class);;
        startActivityForResult(intent,1);
    }

    //换图
    public void ChangeView (View v){
        int mimageview[] = {R.drawable.mimageview1,R.drawable.mimageview2,R.drawable.mimageview3,
                R.drawable.mimageview4,R.drawable.mimageview5,R.drawable.mimageview6,R.drawable.mimageview7};
        int m = (int)(Math.random()*7);
        mView.setBackgroundResource(mimageview[m]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        mpostion = data.getIntExtra("id",1);

        mMusic(mpostion);
        startl.setBackgroundResource(R.drawable.pause);
        mStop = 1;
    }

    //音乐播放
    private void mMusic(int mpostion){


        MusicData data = listDatas.get(mpostion);
        String mUrl = data.getUrl();
        String mtitle = data.getTitle();
        String martist = data.getArtist();



        if("<unknown>".equals(martist)){
            martist = "未知艺术家";
        }

        Log.i("-----------", "mMusic: "+mpostion);
        mTitle.setText(mtitle);
        mArtist.setText(martist);

        mPlayer.stop();
        if(mVisualizer != null){
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            Log.i("-----------", "Visualizer: "+mVisualizer);
        }


        try {
            mPlayer.reset();
            mPlayer.setDataSource(mUrl);
            mPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }


    protected void onPause(){
        super.onPause();
        if (isFinishing() && mPlayer != null){
            mVisualizer.setEnabled(false);
            mVisualizer.release();
            mPlayer.release();
            mPlayer = null;
        }
    }

}
