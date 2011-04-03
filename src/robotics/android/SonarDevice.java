package robotics.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

//TODO: Allow for custom padding instead of just forcing a particular value.
public class SonarDevice extends LinearLayout implements WifiListener
{
	public static class SonarWidget extends ProgressBar
	{
		private static final int WIDTH = 30;
		
		//AttributeSet allows for xml definitions
		public SonarWidget(Context context, AttributeSet attr)
		{
			super(context, attr, android.R.attr.progressBarStyleHorizontal);
			setProgress(50);
			
			setPadding(0, 10, 0, 10);
		}
		
		@Override
		protected void onDraw(Canvas canvas)
		{
			//Rotate the progress bar so that it draws vertically instead.
			canvas.save();
			canvas.rotate(-90, WIDTH / 2, WIDTH / 2);
			canvas.translate(-getHeight() + WIDTH, 0);
			super.onDraw(canvas);
			canvas.restore();
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldW, int oldH)
		{
			super.onSizeChanged(h, w, oldW, oldH);
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int height = MeasureSpec.getSize(heightMeasureSpec);
			setMeasuredDimension(WIDTH + 20, height);
		}
	}
	
	public SonarDevice(Context context, AttributeSet attr)
	{
		super(context, attr);
		setBackgroundResource(R.drawable.fragment_background2);
		
		WifiWidget.addListener(this, (byte)'F', 2);
	}

	@Override
	public void messageReceived(byte[] args)
	{
		if (args.length != 2)
		{
			android.util.Log.e("Sonar", "Invalid message received!");
			return;
		}
		
		if (args[0] == 'L')
		{
			android.util.Log.v("Sonar", "Front-left sonar received request to change value to " + args[1]);
		}
		else if (args[0] == 'M')
		{
			android.util.Log.v("Sonar", "Front-middle sonar received request to change value to " + args[1]);
		}
		else if (args[0] == 'R')
		{
			android.util.Log.v("Sonar", "Front-middle sonar received request to change value to " + args[1]);
		}
	}
	
	@Override
	public void wifiConnected() {}
	@Override
	public void wifiDisconnected() {}
}
