package my.infinitydots;

import android.graphics.Color;
import android.graphics.Paint;

public class Vehicle {


	private int xCoord;
	private int yCoord;
	private int size;
	private double rawSpeed;
	private double speedHolder;
	private double speedMod;
	private double speedModHolder;
	private Paint paint = new Paint();
	
	public Vehicle(int speed, int x, int y, int s)
	{
		speedMod = .34;
		speedModHolder = speedMod;
		
		
		rawSpeed = speed;
		speedHolder = rawSpeed;
		
		
		xCoord = x;
		yCoord = y;
		
		size = s;
		
		paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
	}
	
	public void activatePhaseDrive()
	{
		paint.setColor(Color.LTGRAY);
	}
	
	public void deactivatePhaseDrive()
	{
		paint.setColor(Color.BLACK);
	}
	
	public Paint getPaint()
	{
		return paint;
	}
	
	public void setRawSpeed(int s)
	{
		rawSpeed = s;
	}
	
	public void setSpeedMod(double s)
	{
		speedMod = s;
	}
	
	public double getRawSpeed()
	{
		return rawSpeed;
	}
	
	public double getSpeedMod()
	{
		return speedMod;
	}
	
	public double getSpeedHolder()
	{
		return speedHolder;
	}
	
	public void reset()
	{
		rawSpeed = speedHolder;
		speedMod = speedModHolder;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public void setXCoord(int x)
	{
		xCoord = x;
	}
	
	public int getXCoord()
	{
		return xCoord;
	}
	
	public void setYCoord(int y)
	{
		yCoord = y;
	}
	
	public int getYCoord()
	{
		return yCoord;
	}
}
