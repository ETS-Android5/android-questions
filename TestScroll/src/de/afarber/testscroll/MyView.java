package de.afarber.testscroll;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.widget.OverScroller;

public class MyView extends View {
    private Drawable 	gameBoard = getResources().getDrawable(R.drawable.game_board);
    private ArrayList<Drawable> tiles = new ArrayList<Drawable>();

    private int		mOffsetX = 0;
    private int     mOffsetY = 0;
    private float	mScale = 1.0f;
    private float	mMinZoom;
    private float	mMaxZoom;
    
    private OverScroller     	 mScroller;
    private GestureDetector		 mGestureDetector;
    private ScaleGestureDetector mScaleDetector;

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mScroller = new OverScroller(context);

        SimpleOnGestureListener gestureListener = new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            	//Log.d("onScroll", "distanceX=" + distanceX + ", distanceY=" + distanceY);
                scroll(distanceX, distanceY);
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            	//Log.d("onFling", "velocityX=" + velocityX + ", velocityY=" + velocityY);
                fling(velocityX, velocityY);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
            	adjustZoom();
            	invalidate();
                return true;
            }
        };
        
        SimpleOnScaleGestureListener scaleListener = new SimpleOnScaleGestureListener() {
        	@Override
        	public boolean onScale(ScaleGestureDetector detector) {
        		mScale *= detector.getScaleFactor();
        		// XXX how to adjust mOffsetX and mOffsetY ?
        		constrainZoom();
        		constrainOffsets();
        		
        		Log.d("onScale", "mScale=" + mScale + ", focusX=" + detector.getFocusX() + ", focusY=" + detector.getFocusY());
        		
        		invalidate();
        		return true;
        	}
        };
        
        mGestureDetector = new GestureDetector(context, gestureListener);
        mScaleDetector = new ScaleGestureDetector(context, scaleListener);
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean retVal = mScaleDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }
    
    @Override
    protected void onSizeChanged (int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        
    	mMinZoom = Math.min((float) getWidth() / (float) gameBoard.getIntrinsicWidth(), 
    					   (float) getHeight() / (float) gameBoard.getIntrinsicHeight());

    	mMaxZoom = 2 * mMinZoom;
    	
    	adjustZoom();
    }
    
    private void adjustZoom() {

    	//Log.d("adjustZoom", "getWidth()=" + getWidth() + ", getHeight()=" + getHeight());
    	//Log.d("adjustZoom", "getIntrinsicWidth()=" + gameBoard.getIntrinsicWidth() + ", getIntrinsicHeight()=" + gameBoard.getIntrinsicHeight());
    	//Log.d("adjustZoom", "mMinZoom=" + mMinZoom + ", mMaxZoom=" + mMaxZoom);

    	mScale = (mScale > mMinZoom ? mMinZoom : mMaxZoom);
    	mOffsetX = diffX() / 2;
    	mOffsetY = diffY() / 2;

    	Log.d("adjustZoom", "mScale=" + mScale + ", mOffsetX=" + mOffsetX + ", mOffsetY=" + mOffsetY);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // computeScrollOffset() returns true if a fling is in progress
        if (mScroller.computeScrollOffset()) {
            mOffsetX = mScroller.getCurrX();
            mOffsetY = mScroller.getCurrY();
            postInvalidateDelayed(50);
        }
        
        canvas.save();
        canvas.translate(mOffsetX, mOffsetY);
        canvas.scale(mScale, mScale);
        gameBoard.setBounds(
        	0, 
        	0, 
        	gameBoard.getIntrinsicWidth(),
        	gameBoard.getIntrinsicHeight()
        );
        gameBoard.draw(canvas);  
        canvas.restore();
    }

    // called when the GestureListener detects scroll
    public void scroll(float distanceX, float distanceY) {
        mScroller.forceFinished(true);
        mOffsetX -= (int) distanceX;
        mOffsetY -= (int) distanceY;
        constrainOffsets();
        invalidate();
    }

    // called when the GestureListener detects fling
    public void fling(float velocityX, float velocityY) {
    	int minX = diffX();
    	int maxX = 0;
    	
    	int minY = diffY();
    	int maxY = 0;
    	
    	if (minX > maxX)
    		minX = maxX = diffX() / 2;
    			
    	if (minY > maxY)
    		minY = maxY = diffY() / 2;
    	
        mScroller.forceFinished(true);
        mScroller.fling(
        	mOffsetX, 
        	mOffsetY, 
        	(int) velocityX, 
        	(int) velocityY,  
        	minX,
        	maxX,
        	minY, 
        	maxY,
        	50,
        	50
        );
        invalidate();
    }

    private int diffX() {
    	return (int) (getWidth() - mScale * gameBoard.getIntrinsicWidth());

    }

    private int diffY() {
    	return (int) (getHeight() - mScale * gameBoard.getIntrinsicHeight());
    }

    private void constrainZoom() {
		mScale = Math.max(mScale, mMinZoom);
		mScale = Math.min(mScale, mMaxZoom);
    }
    
    private void constrainOffsets() {
    	int minX = diffX();
    	int maxX = 0;
    	
    	int minY = diffY();
    	int maxY = 0;
    	
    	if (minX > maxX)
    		mOffsetX = diffX() / 2;
    	else {
    		mOffsetX = Math.max(mOffsetX, minX);
    		mOffsetX = Math.min(mOffsetX, maxX);
    	}
    	
    	if (minY > maxY)
    		mOffsetY = diffY() / 2;
    	else {
    		mOffsetY = Math.max(mOffsetY, minY);
    		mOffsetY = Math.min(mOffsetY, maxY);
    	}
    }
}
