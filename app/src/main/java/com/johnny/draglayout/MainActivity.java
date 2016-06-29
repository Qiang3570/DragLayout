package com.johnny.draglayout;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.johnny.draglayout.view.DragLayout;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class MainActivity extends AppCompatActivity {
    private ListView lv_left;
    private ListView lv_main;
    private View iv_header;
    private DragLayout dl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_header = findViewById(R.id.iv_header);
        lv_left = (ListView) findViewById(R.id.lv_left);
        lv_left.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView)view).setTextColor(Color.WHITE);
                return view;
            }
        });
        lv_main = (ListView) findViewById(R.id.lv_main);
        lv_main.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.NAMES));
        dl = (DragLayout) findViewById(R.id.dl);
        dl.setOnDragUpdateListener(new DragLayout.OnDragUpdateListener() {
            @Override
            public void onOpen() {
            }
            @Override
            public void onDraging(float percent) {
                ViewHelper.setAlpha(iv_header, 1 - percent);
            }
            @Override
            public void onClose() {
                ObjectAnimator animator = ObjectAnimator.ofFloat(iv_header, "translationX", 15f);
                animator.setInterpolator(new CycleInterpolator(4));
                animator.setDuration(500);
                animator.start();
            }
        });
        iv_header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dl.open(true);
            }
        });
    }
}
