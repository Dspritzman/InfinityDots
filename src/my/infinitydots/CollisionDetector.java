package my.infinitydots;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Color;
import android.graphics.Paint;

public class CollisionDetector 
{
	public class Debris
	{
		private int xCoord;
		private int yCoord;
		private boolean hit;

		//0 = normal, 1 = hit, 2 = infinity
		private int status;
		
		private Debris prev;
		private Debris next;
		
		public Debris(int x, int y)
		{
			xCoord = x;
			yCoord = y;
			hit = false;
			status = 0;
			
			prev = null;
			next = null;
		}
	}

    //sentinel nodes for main queue
	private Debris head;
	private Debris tail;
	private Debris collisionHeight;

    //sentinel nodes for pool stack
	private Debris poolHead;
	private Debris poolTail;
	private int poolSize;

	private int debrisSize;
	private int debrisSpeed;

    //size of queue
	private int listSize;

	
	private boolean canHit;
	
	
	public CollisionDetector(int size, int speed)
	{
		head = new Debris(0, 0);
		tail = new Debris(0, 0);
		head.next = tail;
		tail.prev = head;
		
		collisionHeight = tail;
		
		
		poolHead = new Debris(0, 0);
		poolTail = new Debris(0, 0);
		poolHead.next = poolTail;
		poolTail.prev = poolHead;
		poolSize = 0;

        //start with 50 debris objects; unlikely to need more
		addToPool(50);
		
		debrisSize = size;
		debrisSpeed = speed;
		listSize = 0;
		
		canHit = true;
	}
	

	
	//initialize
	public void changePaint()
	{
		Debris current = head.next;
		while (current != tail)
		{
			current.status = 0;
			current = current.next;
		}
		
	}

	//debris can't be hit while phase drive is engaged
	public void activatePhaseDrive()
	{
		canHit = false;
        //change status of debris to infinity mode activated
		colorPhase();
	}
	
	public void deactivatePhaseDrive()
	{
		canHit = true;
        //change status of debris to normal
		changePaint();
	}
	
	//infinity mode
	private void colorPhase()
	{
		Debris current = head.next;
        //change status for every object not already hit
		while (current != tail)
		{
            //only change to infinity mode if it hasn't already been hit
			if (current.status != 1)
			{
				current.status = 2;
			}
			current = current.next;
		}
	}
	
	//hit
	public void debrisHit(Debris d)
	{
		if (canHit == true)
		{
            //change object's status to 1 (hit)
			d.status = 1;
			d.hit = true;
		}
	}
	 
	
	public void addDebris(int x, int y)
	{
		//pool reserve low, add more
		if (poolSize < 5)
		{
			addToPool(25);
		}
		
		//remove from pool
		Debris debris = poolHead.next;
		poolHead.next = debris.next;
		debris.next.prev = poolHead;
		
		//add to list
		head.next.prev = debris;
		debris.next = head.next;
		debris.prev = head;
		head.next = debris;
		
		debris.xCoord = x;
		debris.yCoord = y;
		
		if (canHit)
		{
            //normal
			debris.status = 0;
		}
		else
		{
            //infinity mode
			debris.status = 2;
		}

		debris.hit = false;
		
		++listSize;
		--poolSize;
	}
	
	public void removeDebris(Debris d)
	{
		//remove from list
		d.prev.next = d.next;
		d.next.prev = d.prev;

		//add to pool
		poolHead.next.prev = d;
		d.next = poolHead.next;
		d.prev = poolHead;
		poolHead.next = d;
		
		--listSize;
		++poolSize;
	}

	//add a new, blank debris object to pool for later use by main list/queue
	public void addToPool(int amt)
	{
		for (int i = 0; i < amt; ++i)
		{
			Debris debris = new Debris(0, 0);
			Debris temp = poolHead.next;
			poolHead.next = debris;
			debris.prev = poolHead;
			temp.prev = debris;
			debris.next = temp;
			++poolSize;
		}
	}
	
	
	//check if debris objects are lower than the screen/out of vision;
    //if they are they need to be counted toward the player's score
	public int checkToRemove(int height)
	{
		int addScoreAmt = 0;
		while (tail.prev.yCoord > (height + debrisSize))
		{
			removeDebris(tail.prev);
			++addScoreAmt;
		}
		return addScoreAmt;
	}

    //list/queue iterators getNext and getPrev
	public Debris getNext(Debris d)
	{
		if (d.next != tail)
		{
			return d.next;
		}
		else
		{
			return tail;
		}
	}
	
	public Debris getPrev(Debris d)
	{
		if (d.next != head)
		{
			return d.prev;
		}
		else
		{
			return head;
		}
	}

	//move debris down the screen based on their speed
	public void updateYCoord()
	{
		Debris current = head;
		while (current != tail)
		{
			current = current.next;
			current.yCoord += debrisSpeed;
		}
	}

    //set collisionHeight sentinel equal to the first debris object that is too high on the screen to be hit by the player;
    //anything after it can potentially be hit by the player; we will only check these objects to save time
	public void updateCollisionHeight(int vehicleHeightMod)
	{
		if (collisionHeight != tail)
		{
			while (collisionHeight != head)
			{
				if (collisionHeight.yCoord < vehicleHeightMod)
				{
					collisionHeight = collisionHeight.prev;
				}
				else
				{
					break;
				}
			}
		}
		else
		{
			if (tail.prev != head)
			{
				collisionHeight = tail.prev;
			}
		}
	}


    public Debris getHead()
    {
        return head;
    }

    public Debris getTail() {
        return tail;
    }

    public int getDebrisSize()
    {
        return debrisSize;
    }

    public boolean getHit(Debris d)
    {
        return d.hit;
    }

    public boolean getCanHit()
    {
        return canHit;
    }

    public int getYCoord(Debris d)
    {
        return d.yCoord;
    }

    public int getXCoord(Debris d)
    {
        return d.xCoord;
    }

    public int getListSize()
    {
        return listSize;
    }

    public int getDebrisSpeed()
    {
        return debrisSpeed;
    }

    public void setDebrisSpeed(int s)
    {
        debrisSpeed = s;
    }


    public void setCanHit(boolean hittable)
    {
        canHit = hittable;
    }

    public int getStatus(Debris d)
    {
        return d.status;
    }

	public Debris getCHeight()
	{
		return collisionHeight;
	}
}
