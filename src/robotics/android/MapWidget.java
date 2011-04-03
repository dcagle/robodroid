package robotics.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

public class MapWidget extends View
{
	private int rotation = 0;
	private Location target;
	private Location current;
	
	private int width = 0;
	private int height = 0;
	
	private static final int LOC_RAD = 30;
	private static final int PADDING = 5;
	
	public MapWidget(Context context)
	{
		super(context);
	}
	
	public MapWidget(Context context, AttributeSet attr)
	{
		super(context, attr);
	}
	
	public void setRotation(int rotation)
	{
		this.rotation = rotation;
		invalidate();
	}
	
	public void setTarget(Location target)
	{
		this.target = target;
		
		//Don't rely on this...
		double[] coords = new double[2];
		getTargetScreenCoords(coords);
		invalidate(new android.graphics.Rect((int)(coords[0] - LOC_RAD), 
				(int)(coords[1] - LOC_RAD), LOC_RAD * 2, LOC_RAD * 2));
	}
	
	public void setLocation(Location current)
	{
		this.current = current;
		
		//Don't rely on this...
		double[] coords = new double[2];
		getTargetScreenCoords(coords);
		invalidate(new android.graphics.Rect((int)(coords[0] - LOC_RAD), 
				(int)(coords[1] - LOC_RAD), LOC_RAD * 2, LOC_RAD * 2));
	}
	
	private void getTargetScreenCoords(double[] coords)
	{
		if (current != null && target != null)
		{
			double bearing = current.bearingTo(target);
			double distance = current.distanceTo(target);
			coords[0] = Math.sinh(bearing) * distance;
			coords[1] = Math.cosh(bearing) * distance;
		}
	}
	
	@Override
	public boolean isOpaque()
	{
		return true;
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);		
		width = w;
		height = h;
	}
    
	@Override
	public void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		
		//Draw a background (transparency isn't really needed)
		Paint background = new Paint();
		background.setColor(0xFFFF0000);
		canvas.drawRect(PADDING, 0, width - PADDING, height - 2 * PADDING, background);
		background.setColor(0xFF000000);
		canvas.drawRect(PADDING + 2, 2, width - PADDING - 2, height - 2 * PADDING - 2, background);
		background.setColor(0xFFFFFFFF);
		
		// Draw the robot position (should be the center)
		Paint paint = new Paint();
		paint.setColor(0xFFFFFFFF);
		canvas.drawCircle(width / 2, height - PADDING - 2 * LOC_RAD, 10, paint);
		
		// Draw the position of the waypoint (if any)
		if (current != null && target != null)
		{
			double[] coords = new double[2];
			getTargetScreenCoords(coords);
			if (coords[0] < 0)
				coords[0] = 0;
			if (coords[0] > width)
				coords[0] = width;
			if (coords[1] < 0)
				coords[1] = 0;
			if (coords[1] > height)
				coords[1] = height;
			paint.setColor(0xFFFF0000);
			canvas.drawCircle((float)coords[0], (float)coords[1], LOC_RAD, paint);
		}
	}
}
