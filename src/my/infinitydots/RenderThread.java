package my.infinitydots;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.util.Log;

@SuppressLint("WrongCall")
public class RenderThread extends Thread {
    static final long FPS = 60;

    private InfinityView view;

    private boolean running = false;

    public boolean pause = false;
    private boolean firstTime = false;
   

    public RenderThread(InfinityView view, boolean first) {
          this.view = view;
          firstTime = first;
    }

    public void setRunning(boolean run) {
          running = run;
    }


    @Override
    public void run() {
          long ticksPS = 1000 / FPS;
          long startTime;
          long sleepTime;

          Log.d("run", String.valueOf(running));
          
          if (firstTime)
          {
        	  try {
                  //make sure the thread doesn't start running before the view's constructor has finished
                  //otherwise the thread will attempt to call function's that aren't yet ready and will crash
        		  sleep(400);
        	  } catch (InterruptedException e) {
        		  // TODO Auto-generated catch block
        		  e.printStackTrace();
        	  }
          }
          
          
          while (running) {
                 Canvas c = null;

                 //start timing frame
                 startTime = System.currentTimeMillis();

                 try {
                        c = view.getHolder().lockCanvas();

                        synchronized (view.getHolder()) {
                        	if (running == true)
                        	{
                                //draw to appropriate draw state
                        		if (!pause)
                        		{
                                    //active game
                        			view.onDraw(c);
                        		}
                        		else if (pause)
                        		{
                                    //paused game
                        			view.onDraw2(c);
                        		}
                        	}
                        }
                 } finally {
                        if (c != null) {
                            //release canvas to screen
                               view.getHolder().unlockCanvasAndPost(c);
                        }
                 }

                 //stop timing frame
                 sleepTime = (ticksPS-(System.currentTimeMillis() - startTime));

                 try {
                        if (sleepTime > 0)
                        {
                            //synchronize time to 60fps by sleeping when frames load faster than needed
                               sleep(sleepTime);
                        }
                        else
                        {
                               sleep(0);
                        }
                 } catch (Exception e) {}
          }
    }
    
    public void togglePause(boolean toggle)
    {
    		pause = toggle;
    }
    
    public void resetPause()
    {
    	pause = true;
    }



    public void onPause() throws InterruptedException {
        synchronized (view.getHolder()) {
            running = false;
        }
    }


    protected void onResume() {
        synchronized (view.getHolder()) {
            running = true;
        }
    }

}