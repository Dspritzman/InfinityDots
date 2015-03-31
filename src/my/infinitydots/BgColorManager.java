package my.infinitydots;

import java.util.Random;

import android.graphics.Color;

public class BgColorManager {

	private int color;
	
	private int blue = Color.argb(255, 119, 158, 203);
	private int green = Color.argb(255, 119, 190, 119);
	private int red = Color.argb(255, 255, 105, 97);
	private int pink = Color.argb(255, 203, 153, 201);
	private int orange = Color.argb(255, 249, 173, 129);
	
	private Random randomColor;
	private int colorNum;
	
	public BgColorManager()
	{
		color = 0;
		randomColor = new Random();
		colorNum = 0;
	}
	
	public int getColor()
	{
		int tempColor = color;
		while (tempColor == color)
		{
			colorNum = randomColor.nextInt(4);
			
			if (colorNum == 0)
			{
				color = blue;
			}
			else if (colorNum == 1)
			{
				color = green;
			}
			else if (colorNum == 2)
			{
				color = red;
			}
			else if (colorNum == 3)
			{
				color = pink;
			}
			else if (colorNum == 4)
			{
				color = orange;
			}
		}
		
		return color;
	}
	
}


