/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package robotics.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class CommThread extends Thread 
{
	public static final int CON_STATUS = 0;
		public static final int CON_START = 0;
		public static final int CON_FINISH = 1;
		public static final int CON_ERROR = 0;
		public static final int CON_SUCCESS = 1;
	
    private BluetoothSocket socket;
    private InputStream istream;
    private OutputStream ostream;
    private Handler handler;
    private BluetoothDevice device;
    
    public CommThread(BluetoothDevice device, Handler handler)
    {
    	if (device == null)
    	{
    		throw new NullPointerException("Must have a valid Bluetooth Device!");
    	}
    	this.device = device;
    	this.handler = handler;
    	
    	Log.v("CommThread", "Initializing CommThread");
    }

    public void run() 
    {
    	if (device == null)
    		return;
    	
    	Log.v("CommThread", "Starting CommThread");

        try 
        {
        	//This is the recommended way to connect to a device
        	socket = device.createRfcommSocketToServiceRecord(UUID.fromString(
        			"00001101-0000-1000-8000-00805F9B34FB"));
        			
        	// This is a nasty workaround for some devices (like laptops) not connecting properly.
        	/*
        	try
        	{
        		java.lang.reflect.Method m = device.getClass().getMethod("createRfcommSocket", 
        				new Class[] { int.class });
        		socket = (BluetoothSocket)m.invoke(device, 1);
        	}
        	catch (Exception e) { }
        	*/
        	
        	handler.obtainMessage(0x0, CON_START, CON_SUCCESS).sendToTarget();
        	socket.connect();
        	handler.obtainMessage(0x0, CON_FINISH, CON_SUCCESS).sendToTarget();
        	Log.v("CommThread", "Successfully connected to " + device.getName());
        } 
        catch (IOException e) 
        {
        	socket = null;
        	handler.obtainMessage(0x0, CON_FINISH, CON_ERROR).sendToTarget();
        	Log.e("CommThread", "Could not connect to " + device.getName() + ": " + e.getMessage());
        }
        if (socket == null) return;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try 
        {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } 
        catch (IOException e) { }

        istream = tmpIn;
        ostream = tmpOut;
        
        Log.v("CommThread", "tmpIn: " + istream + ", tmpOut: " + ostream);

        StringBuffer sb = new StringBuffer();
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        String message;
        int idx;
        HashMap<String, String> hm;
        String[] chunks;
       
        while (true) 
        {
            try 
            {
                // Read from the InputStream
                bytes = istream.read(buffer);
                sb.append(new String(buffer, 0, bytes));
                while ((idx = sb.indexOf("\r\n\r\n")) > -1) 
                {
                    message = sb.substring(0, idx);
                    sb.replace(0, idx+4, "");
                    hm = new HashMap<String, String>();
                    for (String line : message.split("\n")) 
                    {
                    	chunks = line.trim().split("=", 2);
                    	if (chunks.length != 2) 
                    		continue;
                    	hm.put(chunks[0], chunks[1]);
                    }
                    handler.obtainMessage(0x2a, hm).sendToTarget();
                }
            } 
            catch (IOException e) 
            {
            	Log.e("CommThread", "Connection broken! " + e.getMessage());
                break;
            }
        }
    }

    /* Call this from the main Activity to send data to the remote device */
    public boolean write(byte[] bytes) 
    {
    	if (ostream != null)
    	{
	        try 
	        {
	        	Log.v("CommThread", ostream.toString() + " -> " + bytes.toString());
	            ostream.write(bytes);
	            return true;
	        } 
	        catch (IOException e) 
	        {
	        	Log.e("CommThread.write", "exception during write", e);
	        }
    	}
    	else
    	{
    		Log.e("CommThread.write", "Output Stream not available");
    	}
    	
    	return false;
    }

    /* Call this from the main Activity to shutdown the connection */
    public void cancel() 
    {
        try 
        {
        	if (socket != null)
        		socket.close();
        } 
        catch (IOException e) { }
    }
}
