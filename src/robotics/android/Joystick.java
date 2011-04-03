package robotics.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.ImageView;

public class Joystick extends RelativeLayout implements SensorEventListener
{
	private KnobBackground back;
	private Knob knob;
	
	private class KnobBackground extends ImageView
	{
		public KnobBackground(Context context, AttributeSet attrs)
		{
			super(context, attrs);
			setImageResource(R.drawable.joystick_back);
		}
		
		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			//Pass this information to the knob so that it always remains the same size.
			if (knob != null)
				knob.setMeasureSpec(widthMeasureSpec, heightMeasureSpec);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			android.util.Log.v("KnobBackground", "new width = " + getMeasuredWidth() + ": new height = " + getMeasuredHeight());
		}
	}
	
	private class Knob extends ImageView
	{
		//private int width = 70;
		//private int height = 70;
		//private int width_spec;
		//private int height_spec;
		private int knob_x = 23;
		private int knob_y = 23;
		private int widthMeasureSpec;
		private int heightMeasureSpec;
		private boolean validMeasureSpec = false;
		
		public Knob(Context context, AttributeSet attrs)
		{
			super(context, attrs);
			setImageResource(R.drawable.joystick);
		}
		
		public void setMeasureSpec(int widthMeasureSpec, int heightMeasureSpec)
		{
			this.widthMeasureSpec = widthMeasureSpec;
			this.heightMeasureSpec = heightMeasureSpec;
			validMeasureSpec = true;
			
			setMaxHeight(back.getHeight());
			setMaxWidth(back.getWidth());
			requestLayout();
		}
		
		/*
		public void updatePos(int knobX, int knobY)
		{
			double dist = Math.sqrt(Math.pow(knobX, 2) + Math.pow(knobY, 2));
			if (dist < width / 2)
			{
				knobX = (int)(width / 2 * Math.cos(Math.atan2(knobX, knobY)));
				knobY = (int)(height / 2 * Math.sin(Math.atan2(knobX, knobY)));
			}
			knob_x = knobX;
			knob_y = knobY;
		}
		
		public void updateSize(int width, int height, int widthSpec, int heightSpec)
		{
			this.width = width;
			this.height = height;
			this.width_spec = widthSpec;
			this.height_spec = heightSpec;
		}
		*/
		
		@Override
		public void onDraw(Canvas canvas)
		{
			canvas.save();
			canvas.translate(knob_x, knob_y);
			super.onDraw(canvas);
			canvas.restore();
			
			Paint paint = new Paint();
			paint.setColor(Color.RED);
			paint.setStyle(Style.STROKE);
			canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
		}
		
		@Override
		//Sets this image to be the same size as the background image
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			if (validMeasureSpec)
				super.onMeasure(this.widthMeasureSpec, this.heightMeasureSpec);
			else
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			android.util.Log.v("Knob", "new width = " + getMeasuredWidth() + ": new height = " + getMeasuredHeight());
			
			/*
			android.util.Log.v("onMeasure", "onMeasure() [Knob] called: Dimensions = " + back.getWidth() + ", " + back.getHeight());
			if (back != null)
				setMeasuredDimension(back.getWidth(), back.getHeight());
			else
				setMeasuredDimension(70, 70);
			 */
			//super.onMeasure(width_spec, height_spec);
		}
	}
	
	public Joystick(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		//setBackgroundResource(R.drawable.joystick_back);
		
		back = new KnobBackground(context, attrs);
		knob = new Knob(context, attrs);
		addView(back);
		addView(knob);
	}
	
	public void pause(boolean paused)
	{
		//TODO: Center the joystick and unregister any listeners
	}
	
	/*
	@Override
	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		back.onDraw(canvas);
		knob.onDraw(canvas);
	}
	*/
	
	/*
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		knob.updateSize(getWidth() - 1, getHeight() - 1, widthMeasureSpec, heightMeasureSpec);
	}
	*/
	
	/*
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		//knob.updatePos((int)(event.getX() - (float)getWidth() / 2),
		//		(int)(event.getY() - (float)getHeight() / 2));
		knob.updatePos((int)event.getX(), (int)event.getY());
		knob.invalidate();
		return super.onTouchEvent(event);
	}
	*/
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) 
	{
		
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		
	}
}
