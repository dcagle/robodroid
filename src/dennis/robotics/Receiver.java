package dennis.robotics;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

public class Receiver
{
	private static BluetoothAdapter adapter;
	
	public static void initialize(Context context)
	{
		adapter = BluetoothAdapter.getDefaultAdapter();
		
		if (adapter != null)
		{
			/*
			Output.writeMessage("Activating bluetooth discovery mode..");
			if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
			{
				Intent discoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				context.startActivity(discoverable);
			}
			*/
		}
	}
}
