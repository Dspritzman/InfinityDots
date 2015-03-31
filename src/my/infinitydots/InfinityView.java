package my.infinitydots;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class InfinityView extends SurfaceView {
    //b bitmap is output for blurred screen when we pause
	 private Bitmap b;
    //bm is bitmap for input to be blurred; a capture of entire screen
	 private Bitmap bm;


     //gameLoopThread is thread used for rendering of the game at 60fps
     //holder is used for callback and interoperation with render thread
     private SurfaceHolder holder;
     private RenderThread gameLoopThread;

     //used for switching draw states when pausing/unpausing; keeps track of first and last frames to be drawn
     private boolean firstTime = true;
     private boolean lastTime = false;

     //used for scaling to different sized screens
     private static int controlScreenHeight = 1280;
     
     //main game object
     MissionControl Mission = null;
     
     private int buttonsHeight;
     private int buttonsWidth;
     private int midButtonWidth;

     private int scoreMenuXleft;
     private int scoreMenuXright;
     private int scoreMenuYtop;
     private int scoreMenuYbottom;

     //touch support variables
     private float tx, ty;
     private boolean touched = false;

    //used for logic for draw state switching
     private boolean pausefirst = true;
     

    //paint for left and right move buttons
     private Paint buttonsPaint;
    //paint for current score text while playing
     private Paint scorePaint;
    //paint for player/vehicle that dodges dots
     private Paint vehiclePaint;
    //paint for paused/high score menu rectangle
     private Paint menuPaint;

    //pNormal is default state of circles/unhit
     private Paint pNormal;
    //pMode is when infinity mode is activated and screen turns black
     private Paint pMode;
    //pHit is when dots are hit; sets them to invisible by making alpha 0
     private Paint pHit;

    //keeps track of frame count used to determine when to evaluate phase drive/infinity mode
     private int frameCountPhase;
    //keeps track of frame count used to determine how fast to make the vehicle/player
    //this is because there is an acceleration factor that needs to know how long/how many frames the player has been moving
     private int frameCountVehicle;


    //all dots are drawn to canvas and saved as bitmaps to be drawn much more efficiently than drawCircle(),
    //however we must store the image for dots in every possible state

    //used to store the bitmap of a normal, white dot
     private Bitmap bmNormal;

    //used to store the bitmap of a darkish dot for infinity mode
     private Bitmap bmMode;

    //used to store the bitmap of a invisible, hit dot
     private Bitmap bmHit;

    //used to swap in and out the previous three bitmaps depending on the state of every particular dot on screen
     private Bitmap bmTemp;

    //used to store the bitmap of the left and right buttons which also improves performance, but not as much as not draawing circles
     private Bitmap bmRect;

    //stores the size of the radius of the dots
     private int bmSize;

    //keeps track of whether or not we are drawing to the first frame
    //we do special things in the first frame for both draw states
     private boolean firstDraw = true;



     public InfinityView(Context context)
     {
        super(context);

         //paint for left and right move buttons
        buttonsPaint = new Paint();

        //paint for current score while playing
         scorePaint = new Paint();
         scorePaint.setColor(Color.WHITE);
         scorePaint.setAntiAlias(true);
         scorePaint.setTextSize(60);


         //paint for vehicle/player
         vehiclePaint = new Paint();
         //stroke makes it empty on the inside with just the white ring on the edge of the circle
         vehiclePaint.setStyle(Style.STROKE);
         vehiclePaint.setStrokeWidth(3);
         vehiclePaint.setColor(Color.argb(255, 235, 235, 235));
         vehiclePaint.setAntiAlias(true);


         //paint for pause menu
         menuPaint = new Paint();
         menuPaint.setColor(Color.argb(255, 235, 235, 235));
         menuPaint.setAntiAlias(true);
         //slight shadow
         menuPaint.setShadowLayer(3, 0, 0, Color.BLACK);

        
        pNormal = new Paint();
        pMode = new Paint();
        pHit = new Paint();

         //make dots whiteish
        pNormal.setColor(Color.argb(255, 235, 235, 235));
	    pNormal.setFlags(Paint.ANTI_ALIAS_FLAG);

         //make dots darker
	    pMode.setColor(Color.argb(255, 45, 45, 45));
	    pMode.setFlags(Paint.ANTI_ALIAS_FLAG);

         //make dots invisible by making alpha 0
	    pHit.setColor(Color.argb(0, 235, 235, 235));
	    pHit.setFlags(Paint.ANTI_ALIAS_FLAG);


	    createThread(true);
        holder = getHolder();
        
        holder.addCallback(new SurfaceHolder.Callback() {
				  @Override
               public void surfaceDestroyed(SurfaceHolder holder) 
               {
               }

               @Override
               public void surfaceCreated(SurfaceHolder holder) 
               {
               }

               @Override
               public void surfaceChanged(SurfaceHolder holder, int format,int width, int height) 
               {
             	  gameLoopThread.setRunning(true);
               }
        });
        
     }
     
     public void createThread(boolean first)
     {
    	 gameLoopThread = new RenderThread(this, first);

    	 Log.d("create", String.valueOf(gameLoopThread.isAlive()));
     }
     
     public void stopThread()
     {
    	 gameLoopThread.setRunning(false);

    	 try {
    		 gameLoopThread.join();
    	 } catch (InterruptedException e) {

    	 }

    	 try {
    		 gameLoopThread.onPause();
    	 } catch (InterruptedException e) {
    		 // TODO Auto-generated catch block
    		 e.printStackTrace();
    	 }
     }
     
     public void reMakeThread()
     {
    	 gameLoopThread.setRunning(true);

         if (!gameLoopThread.isAlive())
         {
        	 if (Mission == null)
        	 {
            	 gameLoopThread.start();
        	 }
        	 else if (Mission != null)
        	 {
        		 createThread(true);
            	 gameLoopThread.setRunning(true);
        		 gameLoopThread.start();
        	 }
         }
         else
         {
        	 if (Mission != null)
        	 {
        		 createThread(true);
        	 }
         }
     }

     public boolean checkFirstTime()
     {
    	 return firstTime;
     }



    @SuppressLint("DrawAllocation")
	@Override
    //first draw state; used to render active game
	protected void onDraw(Canvas canvas)
     {
    	 boolean firstTouch = true;

    		 if (firstTime == true)
    		 {
                 //Mission is main object for game; keeps track of everything that doesn't directly involve drawing to the screen
                 //every dot's position and state, vehicle position and speed, score database, etc.
    			 Mission = new MissionControl(getWidth(), getHeight(), getContext());

                 //makes button height  about 20% of screen's height
    			 buttonsHeight = (int) (getHeight() * getHeight() / controlScreenHeight * .1953);
                 //makes each button 1/3 of the screen's width; makes room for left/right buttons and infinity mode button
    			 buttonsWidth = (getWidth() / 3);
                 //infinity mode button/phase drive
    			 midButtonWidth = (getWidth() / 3);

                 //initializes frame counters
                 frameCountPhase = 0;
    			 frameCountVehicle = 0;

                 //bm and b are bitmaps used to capture entire screen to be blurred in second draw state when you pause
    			 bm = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
    			 b = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
    			  
    			 //return radius of circles; determined by size of screen in terms of pixel count
    			 bmSize = Mission.getDebrisSize();

                 //multiply debrisSize by 2 because it only return radius and is intended for drawing circles.
                 //we are determining the size of the circle as a rectangle, though, and must get the full width which is radius * 2
    			 bmNormal = Bitmap.createBitmap(bmSize * 2, bmSize * 2, Config.ARGB_8888);
    			 bmMode = Bitmap.createBitmap(bmSize * 2, bmSize * 2, Config.ARGB_8888);
    			 bmHit = Bitmap.createBitmap(bmSize * 2, bmSize * 2, Config.ARGB_8888);
    			 bmRect = Bitmap.createBitmap(buttonsWidth, buttonsHeight, Config.ARGB_8888);

                 //temporary canvas we can use to create bitmaps
    			 Canvas cTemp = new Canvas();

                 //draw the dots to the temporary canvas, but swap the intended bitmaps we want to draw to in and out first
    		     cTemp.setBitmap(bmNormal);
    		     cTemp.drawCircle(bmSize, bmSize, bmSize, pNormal);

                 //replace bitmap with infinity mode bitmap, then draw with infinity mode paint
    		     cTemp.setBitmap(bmMode);
    		     cTemp.drawCircle(bmSize, bmSize, bmSize, pMode);

                 ///replace bitmap with hit mode bitmap, then draw with hit paint
    		     cTemp.setBitmap(bmHit);
    		     cTemp.drawCircle(bmSize, bmSize, bmSize, pHit);


                 //make left and right buttons half transparent, then draws them to bmRect bitmap to be drawn more efficiently
                 buttonsPaint.setColor(Color.argb(125, 225, 225, 225));
    		     cTemp.setBitmap(bmRect);
    		     cTemp.drawRect(0, 0, buttonsWidth, buttonsHeight, buttonsPaint);

                 //done with special first frame operations
    			 firstTime = false;
    		 }


             //pause has been initiated, draw entire screen to bitmap bm on last frame which we can then
             //blur and put in the background of the pause screen
    		 if (lastTime)
    		 {
                 //new canvas used to draw the screen to our bitmap
    			 canvas = new Canvas(bm);
    		 }

            //finite amount of values (255) we can set the shade of the infinity mode button to
            //so we only change it every 4 frames to make it take 400% longer to deplete/regenerate health
    		 ++frameCountPhase;
    		 if (frameCountPhase % 4 == 1)
    		 {
    			 Mission.evaluatePhaseDrive();
    		 }
    		 if (frameCountPhase > 5)
    		 {
    			 frameCountPhase = 0;
    		 }


             //first thing actually drawn to screen every frame; clears screen as well
             canvas.drawColor(Mission.getBGColor());

             //add new dots to fall
    		 Mission.debrisAdder();

             //update y position of all dots to move them down
    		 Mission.updateDebrisYCoord();


    		 //draw all dots/debris to screen
    		 for (CollisionDetector.Debris current = Mission.getNext(Mission.getHead()); current != Mission.getTail(); current = Mission.getNext(current))
    		 {
                 //check status of every dot/debris and draw it with the paint that matches
    			 if (Mission.getDebrisStatus(current) == 0)
    			 {
    				 bmTemp = bmNormal;
    			 }
    			 else if (Mission.getDebrisStatus(current) == 1)
    			 {
    				 bmTemp = bmHit;
    			 }
    			 else if (Mission.getDebrisStatus(current) == 2)
    			 {
    				 bmTemp = bmMode;
    			 }
    			 canvas.drawBitmap(bmTemp, (float) Mission.getDebrisXCoord(current) - bmSize, (float) Mission.getDebrisYCoord(current) - bmSize, null);
    		 }

             //manages score and modifies dot/debris speed based on it
    		 Mission.checkList();

    		 
    		 //draw left move button
    		 canvas.drawBitmap(bmRect, 0, (getHeight() - buttonsHeight), null);

             //draw infinity mode button
    		 canvas.drawRect(buttonsWidth, (getHeight() - buttonsHeight), (getWidth() - buttonsWidth), getHeight(), Mission.getPhaseDrivePaint());

    		 //draw right move button
    		 canvas.drawBitmap(bmRect, (getWidth() - buttonsWidth), (getHeight() - buttonsHeight), null);


    		 ++frameCountVehicle;

             //detect if the vehicle has collided with any debris
    		 Mission.CollisionDetect();

             //draw vehicle/player
    		 canvas.drawCircle(Mission.getVehicleXCoord() , Mission.getVehicleYCoord(), Mission.getVehicleSize(), vehiclePaint);


             //get score and draw it as text
    		 String score = Mission.getScore();
    		 int scoreWidth = (int) scorePaint.measureText(score, 0, score.length());
    		 int scoreX = (getWidth() - scoreWidth) / 2;
    		 int scoreY = getHeight() - buttonsHeight - 20;
    		 canvas.drawText(Mission.getScore(), scoreX, scoreY, scorePaint);


    	 
    	 
    	 //detect what parts of the screen are being touched; doesn't support multi touch yet
    	 if(touched){
             //increase speed of vehicle every five frames
			 if (frameCountVehicle % 5 == 0)
			 {
				 Mission.vehicleSpeedAccelerate(); 
			 }

			 //right button
			 if ( (tx > getWidth() - buttonsWidth) && (tx < getWidth())  &&  (ty > getHeight() - buttonsHeight) && (ty < getHeight()))
			 {
				 Mission.rightButtonClicked();
			 }
			 //middle button / infinity mode
			 if((tx > buttonsWidth) && (tx < (getWidth() - buttonsWidth)) && ((ty > getHeight() - buttonsHeight) && (ty < getHeight())))
			 {
				 if (firstTouch == true)
				 {
					 Mission.queryPhaseDrive();
					 firstTouch = false;
					 
				 }
			 }

			 //left button
			 if ( (tx > 0) && (tx < buttonsWidth)  &&  (ty > getHeight() - buttonsHeight) && (ty < getHeight()))
			 {
				 Mission.leftButtonClicked();
			 }


             //any other part of the screen touched; start pause process (save screen to bitmap for second draw state)
			 if ((ty < getHeight() - buttonsHeight))
			 {
				 if (lastTime)
				 {
					 lastTime = false;
                     //start rendering second draw state in render thread
					 gameLoopThread.togglePause(true);
				 }
				 
				 if (pausefirst)
				 {
					 lastTime = true;
				 }
				 pausefirst = false;
			 }

		 }
		 else
		 {
			 lastTime = false;
			 frameCountVehicle = 0;
			 Mission.resetFirstTouch();
             //resets speed of vehicle/player to default
			 Mission.vehicleReset();
			 pausefirst = true;
		 }
    	 
    	 firstDraw = true;
     }
     

    //second draw state; used to render pause menu/high scores; freezes game
     protected void onDraw2(Canvas canvas)
      {
         //if you anywhere while paused then start unpausing
    	 if(touched)
    	 {
    		 lastTime = false;
    		 if (pausefirst)
    		 {
    			 gameLoopThread.togglePause(false);
    			 pausefirst = false;
    		 }
    	 }
    	 else
		 {
    		 pausefirst = true;
		 }
    	 
    	 //take bitmap from last frame of active game, apply Gaussian blur to it, and save it to bitmap b for efficient drawing every frame
    	 if (firstDraw)
    	 {
    		 RenderScript rs = RenderScript.create(getContext());
    		 ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
             //bm is input bitmap
    		 Allocation tmpIn = Allocation.createFromBitmap(rs, bm);
             //allocate bitmap for size we want output to be by passing in output bitmap
    		 Allocation tmpOut = Allocation.createFromBitmap(rs, b);
             //set radius of pixels to blur
    		 theIntrinsic.setRadius(9.f);
    		 theIntrinsic.setInput(tmpIn);
    		 theIntrinsic.forEach(tmpOut);
             //b is output bitmap
    		 tmpOut.copyTo(b);

    		 rs.destroy();

    		 firstDraw = false;
    	 }

          //draw blurred background bitmap
    	 canvas.drawBitmap(b, 0, 0, null);

          //determine dimensions of pause menu based on size of screen
          scoreMenuXleft = getWidth() / 6;
          scoreMenuXright = getWidth() - getWidth() / 6;
          scoreMenuYtop = getWidth() / 6;
          scoreMenuYbottom = 460;

          //draw pause menu
          RectF rect1 = new RectF(scoreMenuXleft, scoreMenuYtop, scoreMenuXright, scoreMenuYbottom);
          canvas.drawRoundRect(rect1, 5, 5, menuPaint);

          String score1 = "1. ";
          String score2 = "2. ";
          String score3 = "3. ";

          //get string array containing all scores in descending order; only take top/first three
          String[] Scores = Mission.getScores();
          for (int i = 0; i < Scores.length && i < 3; ++i)
          {
              if (i == 0)
              {
                  score1 += Scores[i];
              }
              else if (i == 1)
              {
                  score2 += Scores[i];
              }
              else if (i == 2)
              {
                  score3 += Scores[i];
              }
          }

          String gamePauseText = "Paused";

          Paint pausePaint = new Paint();
          pausePaint = new Paint();
          pausePaint.setColor(Color.BLACK);
          pausePaint.setAntiAlias(true);
          pausePaint.setTextSize(100);

          Paint pauseScorePaint = new Paint();
          pauseScorePaint = new Paint();
          pauseScorePaint.setColor(Color.BLACK);
          pauseScorePaint.setAntiAlias(true);
          pauseScorePaint.setTextSize(60);

          int pauseWidth = (int) pausePaint.measureText(gamePauseText, 0, gamePauseText.length());
          int scoreX = (getWidth() - pauseWidth) / 2;
          int scoreY = scoreMenuYtop + 90;
          canvas.drawText(gamePauseText, scoreX, scoreY, pausePaint);

          int score1Width = (int) pauseScorePaint.measureText(score1, 0, score1.length());
          int score1X = (getWidth() - pauseWidth) / 2;
          int score1Y = scoreMenuYtop + 90 + 70;
          canvas.drawText(score1, score1X, score1Y, pauseScorePaint);

          int score2Width = (int) pauseScorePaint.measureText(score2, 0, score2.length());
          int score2X = (getWidth() - pauseWidth) / 2;
          int score2Y = scoreMenuYtop + 90 + 140;
          canvas.drawText(score2, score2X, score2Y, pauseScorePaint);

          int score3Width = (int) pauseScorePaint.measureText(score3, 0, score3.length());
          int score3X = (getWidth() - pauseWidth) / 2;
          int score3Y = scoreMenuYtop + 90 + 210;
          canvas.drawText(score3, score3X, score3Y, pauseScorePaint);
      }


    //touch screen logic; doesn't yet support multi touch
     @Override
     public boolean onTouchEvent(MotionEvent event) {
      // TODO Auto-generated method stub
       
      tx = event.getX();
      ty = event.getY();
       
      int action = event.getAction();
      switch(action){
      case MotionEvent.ACTION_DOWN:
       touched = true;
       break;
      case MotionEvent.ACTION_MOVE:
       touched = true;
       break;
      case MotionEvent.ACTION_UP:
       touched = false;
       break;
      case MotionEvent.ACTION_CANCEL:
       touched = false;
       break;
      case MotionEvent.ACTION_OUTSIDE:
       touched = false;
       break;
      default:
      }
      return true;
     }
     
     public void paused(){
         gameLoopThread.togglePause(true);
    	 gameLoopThread.setRunning(false);
     }
     
     public void resumed(){
    	 gameLoopThread.setRunning(true);
     }
}

