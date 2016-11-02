package com.wwh.radarscanview.radarscan;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import com.wwh.radarscanview.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 雷达扫描
 * Created by Wwh on 2016/11/1.
 */
public class RadarScanView extends View {
    private static int ANIM_REFRESH_RATE = 30;//绘制动画的频率, 单位:ms
    private static int CONTROL_RATE = 2;//绘制雷达动画往前移动的距离,单位:px,<0表示逆时针
    private static int CIRCLE_PAINT_BOLD = 3;//绘制圆形的画笔最大宽度,单位:dp

    private static final float PAINT_BOLD_OFFSET = 0.12f;//画笔的粗细步进
    private static final float CIRCLE_RADIUS_BOLD = 5f;//绘制圆形的画笔最大宽度,单位:dp

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    private Context mContext;
    private int defaultWidth;
    private int defaultHeight;
    private int start;
    private int centerX;
    private int centerY;
    private int radarRadius;
    private int radarColor = Color.parseColor("#99a2a2a2");//雷达指针初始颜色
    private int tailColor = Color.parseColor("#50aaaaaa");//雷达指针扫描踪迹颜色

    private List<PaintBean> circleList = new ArrayList();//总的列表
    private List<PaintBean> filterList = new ArrayList();//每次绘制的临时列表

    private Paint mPaintRadar;
    private Matrix matrix;

    boolean isRun = false;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            try {
                while (isRun) {
                    //雷达
                    start += CONTROL_RATE;
                    matrix = new Matrix();
                    matrix.postRotate(start, centerX, centerY);
                    //圆形
                    filterList.clear();
                    for (int i = 0; i < circleList.size(); i++) {
                        PaintBean bean = circleList.get(i);
                        float bold = bean.getPaintBold() - PAINT_BOLD_OFFSET;//画笔的粗细步进
                        float radius = bean.getPaintRadius() + CIRCLE_RADIUS_BOLD;//每次圆半径扩大的步进
                        bean.setPaintBold(bold);
                        bean.setPaintRadius(radius);
                        bean.getPaintCircle().setStrokeWidth(bold);
                        //过滤超出范围的圆
                        if (radius < radarRadius / 2 && bold > 0) {
                            filterList.add(bean);
                        }
                    }
                    circleList.clear();
                    circleList.addAll(filterList);
                    //重新绘制
                    postInvalidate();
                    Thread.sleep(ANIM_REFRESH_RATE);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public RadarScanView(Context context) {
        super(context);
        init(null, context);
    }

    public RadarScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, context);
    }

    public RadarScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, context);
    }

    @TargetApi(21)
    public RadarScanView(Context context, AttributeSet attrs, int defStyleAttr,
                         int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radarRadius = Math.min(w, h);
    }

    private void init(AttributeSet attrs, Context context) {
        //转换为px
        mContext = context;

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs,
                    R.styleable.RadarScanView);
            radarColor = ta.getColor(R.styleable.RadarScanView_radarColor, radarColor);
            tailColor = ta.getColor(R.styleable.RadarScanView_tailColor, tailColor);
            CIRCLE_PAINT_BOLD = (int) ta.getDimension(R.styleable.RadarScanView_circleBold, CIRCLE_PAINT_BOLD);
            ta.recycle();
        }
        initPaint();
        //得到当前屏幕的像素宽高
        defaultWidth = dip2px(DEFAULT_WIDTH);
        defaultHeight = dip2px(DEFAULT_HEIGHT);

        matrix = new Matrix();
        startAnim();
    }

    private void initPaint() {
        mPaintRadar = new Paint();
        mPaintRadar.setColor(radarColor);
        mPaintRadar.setAntiAlias(true);
    }

    //开启绘制雷达扫描和圆形波纹绘制
    public void startAnim() {
        clear();
        isRun = true;
        if (!executorService.isShutdown()){
            executorService.submit(run);
        }
    }

    //清理数据
    public void clear() {
        filterList.clear();
        circleList.clear();
    }

    public void destroy(){
        clear();
        isRun = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    //添加一个圆
    public void addCircle(int color) {
        PaintBean paintBean = new PaintBean();
        paintBean.setPaintBold(CIRCLE_PAINT_BOLD);
        paintBean.setPaintRadius(dip2px(50));//被头像遮住,不需要从0开始

        Paint paint = new Paint();
        paint.setColor(color);//画笔颜色
        paint.setAntiAlias(true);//抗锯齿
        paint.setStyle(Paint.Style.STROKE);//设置实心
        paint.setStrokeWidth(paintBean.getPaintBold());//画笔宽度
        paintBean.setPaintCircle(paint);

        circleList.add(paintBean);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int resultWidth = 0;
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);

        if (modeWidth == MeasureSpec.EXACTLY) {
            resultWidth = sizeWidth;
        } else {
            resultWidth = defaultWidth;
            if (modeWidth == MeasureSpec.AT_MOST) {
                resultWidth = Math.min(resultWidth, sizeWidth);
            }
        }

        int resultHeight = 0;
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (modeHeight == MeasureSpec.EXACTLY) {
            resultHeight = sizeHeight;
        } else {
            resultHeight = defaultHeight;
            if (modeHeight == MeasureSpec.AT_MOST) {
                resultHeight = Math.min(resultHeight, sizeHeight);
            }
        }

        setMeasuredDimension(resultWidth, resultHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < filterList.size(); i++) {
            PaintBean paintBean = filterList.get(i);
            canvas.drawCircle(centerX, centerY, paintBean.getPaintRadius(), paintBean.getPaintCircle());
        }

        //设置颜色渐变从透明到不透明
        //Shader shader = new SweepGradient(centerX, centerY, Color.TRANSPARENT, tailColor);
        Shader shader = new SweepGradient(centerX, centerY, tailColor, radarColor);
        mPaintRadar.setShader(shader);
        canvas.concat(matrix);
        canvas.drawCircle(centerX, centerY, radarRadius / 2, mPaintRadar);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    private int dip2px(float dipValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    private int px2dip(float pxValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    class PaintBean {
        private Paint paintCircle;//画笔
        private float paintRadius;//圆的半径
        private float paintBold;//画笔粗细

        public float getPaintRadius() {
            return paintRadius;
        }

        public void setPaintRadius(float paintRadius) {
            this.paintRadius = paintRadius;
        }

        public float getPaintBold() {
            return paintBold;
        }

        public void setPaintBold(float paintBold) {
            this.paintBold = paintBold;
        }

        public Paint getPaintCircle() {
            return paintCircle;
        }

        public void setPaintCircle(Paint paintCircle) {
            this.paintCircle = paintCircle;
        }
    }
}
