package com.wwh.radarscanview;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import com.makeramen.roundedimageview.RoundedImageView;
import com.wwh.radarscanview.radarscan.RadarScanView;

import java.util.Random;

/**
 * Created by wwh on 2016/11/1.
 */

public class MainActivity extends Activity{
    private static final int SCALE_ANIM = 300;//头像缩放动画显示时间

    Random random = new Random();
    RadarScanView radar;
    RoundedImageView avatar;
    int[] colors = new int[]{R.color.yellow, R.color.blue, R.color.pink, R.color.green};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    private void initViews() {
        radar = (RadarScanView) findViewById(R.id.radar);
        avatar = (RoundedImageView) findViewById(R.id.avatar);

        radar.startAnim();
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int color = random.nextInt(colors.length - 1);
                radar.addCircle(getResources().getColor(colors[color]));
                startScaleAnim(v);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (radar != null) {
            radar.destroy();
        }
    }

    /**
     * 缩放动画
     *
     * @param view 要进行缩放的View
     */
    private void startScaleAnim(View view) {
        if (view == null) {
            return;
        }
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("ScaleX", 1f, 1.1f, 1f);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("ScaleY", 1f, 1.1f, 1f);
        ObjectAnimator scale = ObjectAnimator.ofPropertyValuesHolder(view, pvhX, pvhY);
        scale.setDuration(SCALE_ANIM);
        scale.start();
    }
}
