package com.example.tengyu.mediaplayer.listviewactivity;


import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.tengyu.mediaplayer.R;

import java.util.List;

/**
 * Created by tengyu on 2016/6/22.
 */
public class ListviewActivity extends AppCompatActivity {

    private ListView mListView;
    private List<MusicData> listDatas;
    private MusicAdapter mAdapter;
    int a=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);

        mListView = (ListView) findViewById(R.id.listView);

//        listDatas = ReaderMusic.getInstance(this).readerLocalMusic();
        if(listDatas != null){
            mAdapter = new MusicAdapter(this,listDatas);
            mListView.setAdapter(mAdapter);
        }else {
            listDatas = ReaderMusic.getInstance(this).readerLocalMusic();
            mAdapter = new MusicAdapter(this,listDatas);
            mListView.setAdapter(mAdapter);
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicData data = listDatas.get(position);
                Intent intent = getIntent();
                String mMusic = data.getUrl();
                intent.putExtra("id",position);
                intent.putExtra("Music",mMusic);
                setResult(RESULT_OK,intent);

                finish();
            }
        });
    }
}

