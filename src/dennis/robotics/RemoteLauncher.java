package dennis.robotics;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

/**
 * 
 * @author dpcagle
 * 
 * TODO: Task List
 *	/ Draw images that will be shown on screen.
 *  - Incorporate bluetooth support
 *  / Figure out best way to transfer information to/from external device.
 *  x Provide tab layout with 3 tabs - ??? (motors + sonars), Navigation (GPS + compass), Output
 *  	???
 *  		x Incorporate Accelerometer support
 *  		x Create functional d-pad that uses either button press or accelerometer + sends events
 *  		x Create sonar information and update using events
 *  	Navigation
 *  		- Interpret latitude/longitude from event
 *  		- Create Waypoint Editor dialog (similar to current one in C# code [BarbieCarGPS])
 *  		- Determine correct bearing from the initial location and target location (I think this doesn't work in C# code)
 *  		- Create working compass from event
 *  	Output
 *  		- Create filtering options (Motors/Sonars/GPS/Compass/Input/Output/All/etc...)
 *  		- Create an optimized way to display information on the screen.
 *	- Create connection dialog to display if auto-connection settings do not work properly.
 */
public class RemoteLauncher extends TabActivity
{
	private BluetoothWidget bluetooth;
	private MotorDevice motor;
	private WakeLock lock;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        new Output(this);
        motor = (MotorDevice)findViewById(R.id.MotorDevice);
        bluetooth = (BluetoothWidget)findViewById(R.id.bt_module);
        bluetooth.setParentActivity(this);
        
        //Initialize the tabs
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        
        spec = tabHost.newTabSpec(BluetoothWidget.id);
        spec.setIndicator("Initialization", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Init);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Movement.id);
        spec.setIndicator("Movement", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Movement);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Navigation.id);
        spec.setIndicator("Navigation", res.getDrawable(android.R.drawable.ic_dialog_map));
        spec.setContent(R.id.Navigation);
        tabHost.addTab(spec);
        
        spec = tabHost.newTabSpec(Output.id);
        spec.setIndicator("Output", res.getDrawable(R.drawable.ic_tab_artists));
        spec.setContent(R.id.Output);
        tabHost.addTab(spec);
        
        tabHost.setCurrentTab(0);
        
        //Initialize the wake lock
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        lock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Remote");
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	
    	//Don't allow the screen to turn off while broadcasting/receiving events.
        lock.acquire();
    	
        motor.onResume(this);
        bluetooth.onResume(this);
    }
    
    @Override
    public void onPause()
    {
    	motor.onPause(this);
    	bluetooth.onPause(this);
    	
    	//The activity is no longer sending events, so allow sleeping.
    	lock.release();
    	
    	super.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	bluetooth.onDestroy(this);
    	super.onDestroy();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	//Check if the user is not allowing bluetooth functionality
    	bluetooth.onActivityResult(requestCode, resultCode, this);
    }
}