package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Modified by Jay on 3/30/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    public Bitmap curBitmap;
    private Rect curDrawable;
    private long startTime;
    private int totalDegree = 360;
    private int splatterRadius = 5;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        // TODO
        if (_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();

        if (motionEvent.getAction() == motionEvent.ACTION_DOWN || motionEvent.getAction() == motionEvent.ACTION_MOVE) {
            long factor = SystemClock.elapsedRealtime() - startTime;
            // Log.d("factor: ", String.valueOf((double)factor));
            double denominator = (double) factor / 100 + 0.2;
            // Log.d("denominator is: ", String.valueOf(denominator));
            float radiusMagnifier = 1 / (float) denominator;

            float r = 2000 / factor;

            curDrawable = getBitmapPositionInsideImageView(_imageView);
            if (curDrawable.contains((int) touchX, (int) touchY)) {
                _paint.setColor(curBitmap.getPixel(matchX(curDrawable, touchX), matchY(curDrawable, touchY)));
                switch (_brushType) {
                    case Square:
                        _offScreenCanvas.drawRect(touchX - _defaultRadius, touchY - _defaultRadius, touchX + _defaultRadius, touchY + _defaultRadius, _paint);
                        break;
                    case Circle:
                        _offScreenCanvas.drawCircle(touchX, touchY, r, _paint);
                        break;
                    case CircleSplatter:
                        drawCircleSplatter(touchX, touchY, _defaultRadius, 5);
                        break;
                }

            }
            // update startTime
            startTime = SystemClock.elapsedRealtime();
        }

        invalidate();
        return true;
    }

    private int matchX (Rect curDrawable, float touchX) {
        float ratioX = (curDrawable.right - curDrawable.left) / (float) curBitmap.getWidth();
        float adjustedX = touchX - curDrawable.left;
        return (int) (adjustedX / ratioX);
    }

    private int matchY (Rect curDrawable, float touchY) {
        float ratioY = (curDrawable.bottom - curDrawable.top) / (float) curBitmap.getHeight();
        float adjustedY = touchY - curDrawable.top;
        return (int) (adjustedY / ratioY);
    }

    private void drawCircleSplatter(float touchX, float touchY, int radius, int amount) {
        Random random = new Random();

        for (int i = 0; i < amount; i++) {
            int value = random.nextInt(radius);
            int degree = random.nextInt(totalDegree);
            double radians = Math.toRadians(degree);

            int x = (int) (touchX + Math.tan(radians) * value);
            int y = (int) (touchY + Math.sin(radians) * value);

            _paint.setColor(curBitmap.getPixel(matchX(curDrawable, touchX), matchY(curDrawable, touchY)));
            _offScreenCanvas.drawCircle(x, y, splatterRadius, _paint);
        }
    }

    public Bitmap getOffScreenBitmap() {
        return _offScreenBitmap;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

