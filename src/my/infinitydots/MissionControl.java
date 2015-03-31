package my.infinitydots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.lang.Math;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;

public class MissionControl {

	private Vehicle Player;

	private CollisionDetector DebrisField;
	
	private ScoreKeeper ScoreK;
	
	//Color.WHITE returns integer
	private int backgroundColor;
    //holder holds the original value
	private int backgroundColorHolder;

    //screen height of current device
	private int screenHeight;
    //screen height of my phone as a control
	private int controlScreenHeight;
    //screenHeight/controlScreenHeight
	private double effectiveHeightPixel;
    // 1/screenHeight
	private double percentHeightPixel;
	 
	public int screenWidth;
	private int controlScreenWidth;
	private float effectiveWidthPixel;
	private float percentWidthPixel;

    //space in frames between "waves" or lines of dots/debris; originally intended to count pixels between waves;
    //needs to be changed due to erratic behavior
	private int debrisWaveSpace;
    //used with wavespace to create a semirandom spacing between each wave
	private Random randomHeight;

    //phase drive is infinity mode
	private boolean phaseDriveEngaged;
    //phase drive paint returns color/shade of infinity mode button
	private Paint phaseDrivePaint = new Paint();
    //count is number from 0-255 which represents health and color of infinity button
	public int phaseDriveCount;
    //temp number for health
	private double phaseCountTemp;
    //required so that touching infinity mode button toggles only on the first frame, not every frame
    //that the finger is touching it
	private  boolean firstTouch;
	

	//what percent of the screen's height to fix the vehicle; fixed at 65% down of 1280 and moves left or right
	static double controlVehiclePos = .65;

    //speed at which debris moves down the screen in pixels
	private int baseDebrisSpeed;

    //keeps track of if the game is in progress or if the player has lost and is waiting for all dots to be removed to restart
	private boolean gameActive;

    //minimum damage the debris does to the player
	private int debrisBaseDamage;
    //actual damage the debris does to the player; includes base damage plus more aas the game goes on
	private int debrisHitDamage;

    //minimum space between debris
	private int baseDebrisSpace;
    //actual space between debris with base space plus random additional space
	private int debrisSpace;


    //keeps track of background color of game; switches randomly when game ends
	private BgColorManager colorManager;

    //amount of points to add to score every frame the phase drive is engaged; adds up very fast (60fps)
	private static int modeScore = 10;

    //database to store scores in
    private Database infinityDatabase;
	
	public MissionControl(int width, int height, Context context)
	{
        infinityDatabase = new Database(context);

		gameActive = true;

        //my devices screen dimensions
		controlScreenHeight = 1280;
		controlScreenWidth = 720;

        //current devices screen dimensions
		screenHeight = height;
		screenWidth = width;
		
		
		effectiveHeightPixel = (float)screenHeight / controlScreenHeight;
		effectiveWidthPixel = (float) screenWidth / controlScreenWidth;
		
		percentHeightPixel = (float) 1 / screenHeight;
		percentWidthPixel = (float) 1 / screenWidth;

        //convert height by adjusting for different screen sizes
		baseDebrisSpeed = convertHeight(7);
		debrisBaseDamage = 30;
		debrisHitDamage = debrisBaseDamage;
		baseDebrisSpace = convertHeight(18);
		debrisSpace = baseDebrisSpace;

        //initialize player and debris container objects
		Player = new Vehicle(convertWidth(4), (screenWidth / 2), (int) (screenHeight * controlVehiclePos), convertWidth(14)) ;
		DebrisField = new CollisionDetector(convertWidth(40), convertWidth(baseDebrisSpeed));
		
		randomHeight = new Random();
		debrisWaveSpace = 0;


		colorManager = new BgColorManager();
		backgroundColorHolder = colorManager.getColor();
		backgroundColor = backgroundColorHolder;
		
		phaseDriveEngaged = false;
		phaseDriveCount = 0;
		phaseCountTemp = 0;
		firstTouch = true;
		phaseDrivePaint.setColor(Color.argb(255 - phaseDriveCount, phaseDriveCount, phaseDriveCount, phaseDriveCount));
		
		ScoreK = new ScoreKeeper();
	}

    //returns all scores
    public String[] getScores()
    {
        return infinityDatabase.getScores();
    }

    //rand out of health; stop spawning debris and wait to reset
	public void gameOver()
	{
		gameActive = false;
		DebrisField.setCanHit(false);
	}
	
	//determine if health needs to be added or taken away based on if the phase drive/infinity mode is activated
	public void evaluatePhaseDrive()
	{
		if (gameActive)
		{
            //0 is full  health, 255 is no health
			if (!phaseDriveEngaged && (phaseDriveCount != 0))
			{
				if (phaseDriveCount > 2)
				{
                    //regenerate health
					phaseCountTemp -= .85;
					phaseDriveCount = (int) phaseCountTemp;
				}
				else
				{
					phaseCountTemp = 0;
					phaseDriveCount = 0;
				}
				if (phaseDriveCount < 0)
				{
					phaseCountTemp = 0;
					phaseDriveCount = 0;
				}
                //adjust color
				phaseDrivePaint.setColor(Color.argb(255 - phaseDriveCount, phaseDriveCount, phaseDriveCount, phaseDriveCount));
			}
			else if (phaseDriveCount != 255)
			{
				if (phaseDriveCount < 253)
				{
                    //subtract from health while phase drive is engaged
					phaseCountTemp += 3;
					phaseDriveCount = (int) phaseCountTemp;
				}
				else
				{
					phaseCountTemp = 255;
					phaseDriveCount = 255;
				}
				if (phaseDriveCount >= 255)
				{
					phaseCountTemp = 255;
					phaseDriveCount = 255;
					gameOver();
				}

				if (phaseDriveCount >= 215)
				{
					disengagePhaseDrive();
				}
                //adjust color
				phaseDrivePaint.setColor(Color.argb(255 - phaseDriveCount, phaseDriveCount, phaseDriveCount, phaseDriveCount));
			}
		}
		
		
	}
	
	public void resetFirstTouch()
	{
		firstTouch = true;
	}
	
	//toggles phase drive/infinity mode
	public void queryPhaseDrive()
	{
		if (firstTouch)
		{
			if (phaseDriveEngaged)
			{
				disengagePhaseDrive();
			}
            //cuts off phase drive before health reaches 255 (no health)
			else if (phaseDriveCount < 215)
			{
				engagePhaseDrive();
			}
		}
		firstTouch = false;
	}
	

	
	public void engagePhaseDrive()
	{
		phaseDriveEngaged = true;
		DebrisField.activatePhaseDrive();
		Player.activatePhaseDrive();
		backgroundColor = Color.BLACK;
	}
	
	public void disengagePhaseDrive()
	{
		phaseDriveEngaged = false;
		DebrisField.deactivatePhaseDrive();
		Player.deactivatePhaseDrive();
		backgroundColor = backgroundColorHolder;
	}
	
	
	public void CollisionDetect()
	{
        //only checks debris with height low enough on the screen to potentially hit the player
		int checkHeight = Player.getYCoord() - Player.getSize();
		DebrisField.updateCollisionHeight(checkHeight);

        //returns sentinel node; all nodes following until tail node are low enough on screen to potentially be hit by player
		CollisionDetector.Debris CHeight = DebrisField.getCHeight();


		while (CHeight != DebrisField.getTail())
		{
            //uses distance formula with circle radii to check if they overlap/debris has been hit
			double distance = calculateDistance(DebrisField.getXCoord(CHeight), DebrisField.getYCoord(CHeight));

            //only checks debris that hasn't already been hit
			if (!DebrisField.getHit(CHeight))
			{
                //only proceeds if player and debris overlap / they hit
				if (distance <= (Player.getSize() + DebrisField.getDebrisSize()))
				{
                    //set that debris object's status to hit
					DebrisField.debrisHit(CHeight);

                    //only proceeds if debris can be hit
					if (DebrisField.getCanHit())
					{
                        //reduce health by damage
						phaseCountTemp += debrisHitDamage;
						phaseDriveCount += debrisHitDamage;
						if (phaseDriveCount > 255)
						{
                            //if health is empty then end game
							phaseCountTemp = 255;
							phaseDriveCount = 255;
							gameOver();
						}
                        //readjust infinity mode button's color
						phaseDrivePaint.setColor(Color.argb(255 - phaseDriveCount + 10, phaseDriveCount, phaseDriveCount, phaseDriveCount));
					}
				}
			}
            //iterate to next node
			CHeight = DebrisField.getNext(CHeight);
		}
	}

    //returns result of distance formula
	private double calculateDistance(int x, int y)
	{
		int x2 = Player.getXCoord() - x;
		int xSqr = (x2) * (x2);
		int y2 = Player.getYCoord() - y;
		int ySqr = (y2) * (y2);
		
		return Math.sqrt(xSqr + ySqr);
	}
	
	
	
	public void leftButtonClicked()
	{
        //checks to make sure player is within bounds of screen
		if (Player.getXCoord() > Player.getSize())
		{
			moveVehicleLeft();
			
			if (Player.getXCoord() < Player.getSize())
			{
				Player.setXCoord(Player.getSize());
			}
		}
	}
	
	public void rightButtonClicked()
	{
        //checks to make sure player is within bounds of screen
		if (Player.getXCoord() < screenWidth - Player.getSize())
		{
			moveVehicleRight();
			
			if (Player.getXCoord() > screenWidth - Player.getSize())
			{
				Player.setXCoord(screenWidth - Player.getSize());
			}
		}
	}
	
	public void moveVehicleRight()
	{
		Player.setXCoord((int) (Player.getXCoord() + Player.getRawSpeed()));
	}
	
	public void moveVehicleLeft()
	{
		Player.setXCoord((int) (Player.getXCoord() - Player.getRawSpeed()));
	}

	
	public void vehicleSpeedAccelerate()
	{
		//multiplying already converted initial raw speed, so we don't need to factor in screen dimensions again
		if ((Player.getRawSpeed() <= Player.getSpeedHolder() * 3))
		{
			if ((Player.getRawSpeed() * (1 + Player.getSpeedMod()) ) < 9)
			{
				Player.setRawSpeed((int) (Player.getRawSpeed() * (1 + Player.getSpeedMod()) ));
				Player.setSpeedMod(Player.getSpeedMod() * 1);
			}
			else
			{
				Player.setRawSpeed((int) (Player.getSpeedHolder() * 2));
			}
		}
	}
	

	
	public void checkList()
	{
        //returns number of debris objects being removed because they have fallen below the screen
		int addScoreAmt = DebrisField.checkToRemove(screenHeight);
		if (!phaseDriveEngaged && addScoreAmt != 0)
		{
            //add to score for each object being removed
			ScoreK.addScore(addScoreAmt);
		}
		else if (phaseDriveEngaged)
		{
			ScoreK.addBonusScore(modeScore);
		}


        //logic for modifying debris speed and spacing based on score to make it harder as the player progresses
		debrisHitDamage = debrisBaseDamage + ((ScoreK.getScore() / 2000) * 5);
		if (ScoreK.getScore() < 800)
		{
			DebrisField.setDebrisSpeed(baseDebrisSpeed + convertHeight((1 )));
			debrisSpace = baseDebrisSpace - convertHeight((1 ));
		}
		else if (ScoreK.getScore() < 2000)
		{
			DebrisField.setDebrisSpeed(baseDebrisSpeed + convertHeight((2)));
			debrisSpace = baseDebrisSpace - convertHeight((2));
		}
		else if (ScoreK.getScore() < 3500)
		{
			DebrisField.setDebrisSpeed(baseDebrisSpeed + convertHeight((3)));
			debrisSpace = baseDebrisSpace - convertHeight((3));
		}
		else if (ScoreK.getScore() < 6000)
		{
			DebrisField.setDebrisSpeed(baseDebrisSpeed + convertHeight((4)));
			debrisSpace = baseDebrisSpace - convertHeight((4));
		}
		else
		{
			DebrisField.setDebrisSpeed(baseDebrisSpeed + convertHeight((4)) + (ScoreK.getScore() / 4000));
			debrisSpace = baseDebrisSpace - convertHeight((4)) - convertHeight(((ScoreK.getScore() / 4000) ));
		}
		
		
	}

	

	public void debrisAdder()
	{
		if (gameActive)
		{
            //determine how many frames to wait until next wave/line of debris; explains erratic
            //behavior when speed changes; was intended to keep track of pixels not frames and needs to be changed
			if (debrisWaveSpace == 0)
			{
				addDebrisWave();
				debrisWaveSpace = convertHeight(debrisSpace) + randomHeight.nextInt(convertHeight(10));
			}
			else
			{
				--debrisWaveSpace;
			}
		}
		else	//reset game
		{
            //wait until all debris objects are removed before resetting the game
			if (DebrisField.getListSize() == 0)
			{
				gameActive = true;
				debrisWaveSpace = 0;
				DebrisField.setCanHit(true);
				phaseCountTemp = 0;
				phaseDriveCount = 0;
				debrisHitDamage = debrisBaseDamage;
				debrisSpace = baseDebrisSpace;
				DebrisField.setDebrisSpeed(baseDebrisSpeed);
                infinityDatabase.addScore(ScoreK.getScore());
				ScoreK.resetScore();
				backgroundColorHolder = colorManager.getColor();
				backgroundColor = backgroundColorHolder;
			}
		}
	}
	
	
	private void addDebrisWave()
	{
		Random randQuantity = new Random();
		
		int minDebris;
		int bonusDebris;
		float debrisPercent = 0;

        //determines the minimum number of debris objects are needed to fill the screen
		for (minDebris = 0; debrisPercent < 20; ++minDebris)
		{
			debrisPercent = (float) ((float) getDebrisSize() * 2 / screenWidth * 100) * minDebris;
		}
		
		--minDebris;
		debrisPercent = 0;

        //determines the maximum number of bonus debris objects that can be added to the minDebris
		for (bonusDebris = 0; debrisPercent <= 30; ++bonusDebris)
		{
			debrisPercent = (float) ((float) getDebrisSize() * 2 / screenWidth * 100) * bonusDebris;
		}
		
		--bonusDebris;
		
		ArrayList<Integer> dList = new ArrayList<Integer>();
		for (int i = 0; i < randQuantity.nextInt(bonusDebris) + minDebris; ++i)
		 {
			 int tempRand = 0;
			 boolean doesFit = false;
			 int loopCounter = 0;

             //doesFit gets set to false if tempRand would overlap an already existing debris object; if it doesn't get set to false, then
             //tempRand will be added as a new debris object
             //also, we only attempt to place it 20 times; if it isn't placed after 20 times we move on the the next debris object we are trying to add
			 while (doesFit == false && loopCounter < 20)
			 {
				 Random  randWidth = new Random();
				 //temp rand = number at least debrisSize greater than 0, but no more than debrisSize less than getWidth()
				 //multiply by 2 to account for added debrisSize outside of rand function
				 tempRand = (int) (randWidth.nextInt((int) (screenWidth - DebrisField.getDebrisSize() * 2)) + DebrisField.getDebrisSize());
				 
				 doesFit = true;
				 ++loopCounter;

				 for (int j = 0; j < dList.size(); ++j )
				 {
                     //tempRand is the position we want to put a debris object; x represents the positions of
                     //existing debris objects as we iterate through them all
					int x = dList.get(j);
                     //make sure tempRand and x do not overlap, if they do then the object can't fit there
					 if (x == tempRand)
					 {
						 doesFit = false;
					 }
					 else if ((x > tempRand)&&(x < (tempRand + DebrisField.getDebrisSize() * 2)))
					 {
						 doesFit = false;
					 }
					 else if ((x < tempRand)&&(x > (tempRand - DebrisField.getDebrisSize() * 2)))
					 {
						 doesFit = false;
					 }
				 }
			 }

             //debris object will fit, add it to list of objects
			 if (doesFit == true)
			 {
				 dList.add(tempRand);
			 }
		 }

        //sort and add debris objects to debris container by using the integer values as X coordinates
		Collections.sort(dList);
		for (int k = dList.size(); k > 0; --k)
		{
            //place new debris objects at Y cooridnate -150 (offscreen) so that they can fall down onto screen
			DebrisField.addDebris(dList.get(k - 1), -150);
		}
	}


    public Paint getPhaseDrivePaint()
    {
        return phaseDrivePaint;
    }

    //converts amt to desired equivalent on current device by pixel percent
    private int convertWidth(int amt)
    {
        return ((int) ((screenWidth * effectiveWidthPixel * percentWidthPixel) * amt));
    }

    //converts amt to desired equivalent on current device by pixel percent
    private int convertHeight(int amt)
    {
        return ((int) ((screenHeight * effectiveHeightPixel * percentHeightPixel) * amt));
    }

    //returns: normal, infinity mode, or hit
    public int getDebrisStatus(CollisionDetector.Debris d)
    {
        return DebrisField.getStatus(d);
    }

    public int getBGColor()
    {
        //returns random, new color from available colors
        return backgroundColor;
    }


    public void vehicleReset()
    {
        //resets speed of vehicle/player to default speed
        Player.reset();
    }

    public double getVehicleSpeed()
    {
        return Player.getRawSpeed();
    }

    public int getVehicleXCoord()
    {
        return Player.getXCoord();
    }

    public int getVehicleYCoord()
    {
        return Player.getYCoord();
    }

    public int getVehicleSize()
    {
        return Player.getSize();
    }


    public CollisionDetector.Debris getHead()
    {
        return DebrisField.getHead();
    }

    public CollisionDetector.Debris getTail()
    {
        return DebrisField.getTail();
    }

    public CollisionDetector.Debris getNext(my.infinitydots.CollisionDetector.Debris d)
    {
        return DebrisField.getNext(d);
    }

    public CollisionDetector.Debris getPrev(my.infinitydots.CollisionDetector.Debris d)
    {
        return DebrisField.getPrev(d);
    }

    public int getDebrisXCoord(CollisionDetector.Debris d)
    {
        return DebrisField.getXCoord(d);
    }

    public int getDebrisYCoord(CollisionDetector.Debris d)
    {
        return DebrisField.getYCoord(d);
    }

    public void updateDebrisYCoord()
    {
        DebrisField.updateYCoord();
    }

    public int getDebrisSize()
    {
        return DebrisField.getDebrisSize();
    }

    public int getCDSize()
    {
        return DebrisField.getListSize();
    }


    public String getScore()
    {
        return String.valueOf(ScoreK.getScore());
    }

    public int getDebrisSpace()
    {
        return debrisSpace;
    }

    public int getDebrisSpeed()
    {
        return DebrisField.getDebrisSpeed();
    }
	
}
