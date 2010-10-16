package dennis.robotics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.view.MotionEvent;

public class MotorDevice extends LinearLayout implements SensorEventListener
{
	private float[] accelerometer = new float[3];
	private float[] geomagnetic = new float[3];
	private MotorWidget widget;
	
	private static class MotorButton extends ImageButton
	{
		private int rotation;
		
		public MotorButton(Context context, AttributeSet attr, int rotation)
		{
			super(context, attr);
			
			this.rotation = rotation;
			setImageResource(R.drawable.motor_ctrl);
			setBackgroundColor(Color.argb(0, 0, 0, 0));
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			return super.onTouchEvent(event);
		}
		
		@Override
		protected void onDraw(Canvas canvas)
		{
			canvas.save();
			canvas.rotate(rotation, getWidth() / 2, getHeight() / 2);
			super.onDraw(canvas);
			canvas.restore();
		}
		
		@Override
		protected void onMeasure(int measureWidthSpec, int measureHeightSpec)
		{
			Drawable d = getResources().getDrawable(R.drawable.motor_ctrl);
			if (rotation == -90 || rotation == 90)
			{
				setMeasuredDimension(d.getIntrinsicHeight(), d.getIntrinsicWidth());
			}
			else
			{
				setMeasuredDimension(d.getIntrinsicWidth(), d.getIntrinsicHeight());
			}
		}
	}
	
	//TODO: Replace ImageButton with something less complex
	private static class Filler extends ImageButton
	{
		public Filler(Context context, AttributeSet attr)
		{
			super(context, attr);
			setBackgroundColor(Color.argb(0, 0, 0, 0));
		}
	}
	
	private static class MotorWidget extends TableLayout
	{
		private static final int UP = 0;
		private static final int LEFT = 1;
		private static final int RIGHT = 2;
		private static final int DOWN = 3;
		
		private ImageButton[] buttons = new ImageButton[4];
		
		public MotorWidget(Context context, AttributeSet attr)
		{
			super(context, attr);
			buttons[UP] = new MotorButton(context, attr, 0);
			buttons[LEFT] = new MotorButton(context, attr, -90);
			buttons[RIGHT] = new MotorButton(context, attr, 90);
			buttons[DOWN] = new MotorButton(context, attr, 180);
			
			TableRow cur = new TableRow(context, attr);
			cur.addView(new Filler(context, attr));
			cur.addView(buttons[UP]);
			cur.addView(new Filler(context, attr));
			addView(cur);
			
			cur = new TableRow(context, attr);
			cur.addView(buttons[LEFT]);
			cur.addView(new Filler(context, attr));
			cur.addView(buttons[RIGHT]);
			addView(cur);
			
			cur = new TableRow(context, attr);
			cur.addView(new Filler(context, attr));
			cur.addView(buttons[DOWN]);
			cur.addView(new Filler(context, attr));
			addView(cur);
		}
		
		public void interpretSensorData(int pitch, int roll)
		{
			buttons[LEFT].setSelected(pitch > 20);
			buttons[RIGHT].setSelected(pitch < -20);
			buttons[UP].setSelected(roll > 20);
			buttons[DOWN].setSelected(roll < -20);
			Transmitter.sendMessage("Pitch: " + pitch + ", Roll: " + roll + "\n");
		}
	}
	
	public MotorDevice(Context context, AttributeSet attr)
	{
		super(context, attr);
		
		widget = new MotorWidget(context, attr);
		addView(widget);
	}
	
	/**
	 * Should be called when the parent activity is resumed (or started for the first time).
	 * 
	 * @param context The context that is resuming.
	 */
	public void onResume(Context context)
	{
		SensorManager manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
		java.util.List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (sensors.size() > 0)
		{
			manager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
		}
    	sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
    	if (sensors.size() > 0)
    	{
    		manager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
    	}
	}
	
	/**
	 * Should be called when the parent activity is paused.
	 * 
	 * @param context The context that paused.
	 */
	public void onPause(Context context)
	{
    	//Stop updating the information if this activity is no longer in use.
    	SensorManager manager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    	manager.unregisterListener(this);
	}
	
	/**
	 * Should be manually called by a parent activity every time the accelerometer or 
	 *   magnetic field sensors give data to the parent activity.
	 * 
	 * @param event The SensorEvent containing the data from the sensors.
	 * Used to return The calculated azimuth, pitch, roll, and accuracy (in that order) if a reliable 
	 *   reading is obtained. Otherwise returns a blank array of size 0.
	 */
	public void onSensorChanged(SensorEvent event)
	{
		if (event.accuracy != SensorManager.SENSOR_STATUS_UNRELIABLE)
		{
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			{
				accelerometer = event.values.clone();
			}
			else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			{
				geomagnetic = event.values.clone();
			}
			
			if (accelerometer != null && geomagnetic != null)
			{
				float[] r = new float[9];
				float[] i = new float[9];
				
				if (SensorManager.getRotationMatrix(r, i, accelerometer, geomagnetic))
				{
					float[] remap = new float[9];
					SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Y, remap);
					float[] actualOrientation = new float[3];
					SensorManager.getOrientation(remap, actualOrientation);	
					
					//int[] debugVars = { 
					//		(int)Math.toDegrees(actualOrientation[0]), 
					//		(int)Math.toDegrees(actualOrientation[1]),
					//		(int)Math.toDegrees(actualOrientation[2]), 
					//		event.accuracy };
					
					widget.interpretSensorData((int)Math.toDegrees(actualOrientation[1]), 
							(int)Math.toDegrees(actualOrientation[2]));
					//return debugVars;
				}
			}
		}
		//return new int[0];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
