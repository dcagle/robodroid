package robotics.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Turns on bluetooth if needed and initializes the Transmitter and Receiver.
 * Requires the following xml elements:
 *   - (Button) bt_toggle
 *   - (Button) debugMode
 *   - (Button) bt_action
 */
public class BluetoothWidget extends LinearLayout implements OnClickListener
{
	public static final String id = "Bluetooth";
	
	private static final int REQUEST_BLUETOOTH_EN = 0;
	
	//Define the states of bt_action
	private static final int ENABLE_BT = 0;
	private static final int SCAN = 1;
	private static final int CANCEL_SCAN = 2;
	private static final int CANCEL_CONN = 3;
	
	private Activity parent;
	
	private static BluetoothAdapter adapter;
	private static Button debugMode;
	private static TextView bt_info;
	private static ImageButton bt_action;
	private static ProgressBar bt_progress;
	private static boolean bt_enabled;
	
	public BluetoothWidget(Context context, android.util.AttributeSet attr)
	{
		super(context, attr);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.bt_widget, this);
		
		bt_enabled = false;
		adapter = BluetoothAdapter.getDefaultAdapter();
		
		if (adapter != null)
		{
			Transmitter.initialize(context, (Transmitter.DeviceList)findViewById(R.id.bt_devices));
			Receiver.initialize(context);
			
			debugMode = (Button)findViewById(R.id.debugMode);
			debugMode.setOnClickListener(this);
			
			bt_progress = (ProgressBar)findViewById(R.id.bt_progress);
			bt_info = (TextView)findViewById(R.id.bt_info);
			bt_action = (ImageButton)findViewById(R.id.bt_action);
			bt_action.setOnClickListener(this);
			
			if (adapter.isEnabled())
			{
				bluetoothTurnedOn();
			}
			else
			{
				bt_action.setTag(ENABLE_BT);
			}
		}
		else
		{
			Toast.makeText(context, "Phone doesn't support bluetooth", Toast.LENGTH_SHORT);
		}
	}
	
	public void setParentActivity(Activity parent)
	{
		this.parent = parent;
	}
	
	/* Should be called when the bluetooth module has been turned on */
	public static void bluetoothTurnedOn()
	{
		bt_action.setTag(SCAN);
		bt_info.setText("Paired Devices:");
		bt_progress.setVisibility(View.GONE);
		bt_action.setImageResource(R.drawable.ic_menu_search);
		
		// TODO: Unlock device connection elements
	}
	
	/* Should be called whenever a connection attempt to a device has begun */
	public static void connectionStarted()
	{
		bt_action.setTag(CANCEL_CONN);
		bt_info.setText("Connecting...");
		bt_progress.setVisibility(View.VISIBLE);
		bt_action.setImageResource(R.drawable.ic_menu_close_clear_cancel);
	}
	
	/* Should be called whenever a connection attempt to a device has finished */
	public static void connectionFinished()
	{
		bt_action.setTag(SCAN);
		bt_info.setText("Devices in Range:");
		bt_progress.setVisibility(View.GONE);
		bt_action.setImageResource(R.drawable.ic_menu_search);
	}
	
	/**
	 * Should be called whenever the BluetoothAdapter has finished discovering devices.
	 */
	public static void discoveryFinished()
	{
		bt_action.setTag(SCAN);
		bt_info.setText("Devices in range:");
		bt_progress.setVisibility(View.GONE);
		bt_action.setImageResource(R.drawable.ic_menu_search);
	}
	
	/**
	 * Should be called when the parent activity is paused.
	 * 
	 * @param context The context that has paused.
	 */
	public void onPause(Context context)
	{
		//TODO: Do stuff on pause.
	}
	
	/**
	 * Should be called when the parent activity is resumed.
	 * 
	 * @param context The context that has resumed.
	 */
	public void onResume(Context context)
	{
		//TODO: Do stuff on resume.
	}
	
	/**
	 * Should be called when the parent activity is about to be destroyed.
	 * 
	 * @param context The context that is about to be destroyed.
	 */
	public void onDestroy(Context context)
	{
		Transmitter.onDestroy(context);
		
		/* I don't think this will work, the os doesn't seem to like dialogs popping up upon exit
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Turn off Bluetooth?");
		
		android.app.Dialog.OnClickListener listener = new android.app.Dialog.OnClickListener()
		{
			public void onClick(android.content.DialogInterface dialog, int choice)
			{
				android.util.Log.i("Bluetooth Widget", "Button " + choice + " pressed.");
			}
		};
		
		builder.setPositiveButton("Yes", listener);
		builder.setNegativeButton("No", listener);
		AlertDialog dialog = builder.create();
		dialog.show();
		*/
	}
	
	/**
	 * Should be called whenever an intent returns a value to the parent activity.
	 * 
	 * @param requestCode The integer code associated with the returning intent.
	 * @param resultCode The result reported by the returning intent.
	 * @param context The context of the parent activity.
	 */
	public void onActivityResult(int requestCode, int resultCode, Context context)
	{
		if (requestCode == REQUEST_BLUETOOTH_EN)
		{
			bt_action.setEnabled(true);
			if (resultCode != Activity.RESULT_OK)
			{
				//User is not allowing this program to turn bluetooth on
				Toast.makeText(context, "Could not connect to a device!", Toast.LENGTH_LONG).show();
			}
			else
			{
				bluetoothTurnedOn();
			}
		}
	}
	
	@Override
	public void onClick(View view)
	{
		if (view.getId() == R.id.bt_action)
		{
			if (bt_action.getTag().equals(ENABLE_BT))
			{
				if (!bt_enabled)
				{
					if (parent != null)
					{
						bt_action.setEnabled(false);
						Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						parent.startActivityForResult(intent, REQUEST_BLUETOOTH_EN);
					}
					else
					{
						throw new IllegalArgumentException("Parent activity not defined! Must " +
								"use setParentActivity(Activity) during initialization process.");
					}
				}
			}
			else if (bt_action.getTag().equals(SCAN))
			{
				bt_action.setTag(CANCEL_SCAN);
				bt_info.setText("Scanning...");
				bt_progress.setVisibility(View.VISIBLE);
				bt_action.setImageResource(R.drawable.ic_menu_close_clear_cancel);
				Transmitter.startDiscovery();
			}
			else if (bt_action.getTag().equals(CANCEL_SCAN))
			{
				bt_action.setTag(SCAN);
				bt_info.setText("Devices in Range:");
				bt_progress.setVisibility(View.GONE);
				bt_action.setImageResource(R.drawable.ic_menu_search);
				Transmitter.cancelDiscovery();
			}
		}
		else if (view.getId() == R.id.debugMode)
		{
			//TODO: Unlock other tabs
		}
	}
}
