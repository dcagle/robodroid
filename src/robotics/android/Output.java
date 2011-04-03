package robotics.android;

import android.app.Activity;
import android.widget.EditText;

public class Output
{
	public static final String id = "Output";
	
	private static EditText output;
	
	public Output(Activity parent)
	{
		output = (EditText)parent.findViewById(R.id.txtOutput);
	}
	
	public static void writeMessage(String message)
	{
		output.append(message + System.getProperty("line.separator"));
	}
}
