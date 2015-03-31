package my.infinitydots;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {


	  private static final String DATABASE_NAME = "InfinityDots.db";
	  private static final int DATABASE_VERSION = 1;


/*
 * 
 * Create prepared sql statements for table creation
 * 
 */
	  
	  //creates courses table if it doesn't exist
      private static final String SCORES_CREATE = "CREATE TABLE IF NOT EXISTS SCORES " +
                   "(score   INTEGER NOT NULL);";

      //constructor, creates database in app
	  public Database(Context context) {
	    super(context, DATABASE_NAME, null, DATABASE_VERSION);
	  }

	  
	  //where tables actually get created, runs when database is first constructed (ideally every time the app starts up)
	  @Override
	  public void onCreate(SQLiteDatabase database) {
	    database.execSQL(SCORES_CREATE);
	  }

	  //automatically called when the database version is incremented, we don't use it...yet...
	  @Override
	  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		  
	  }
	  
	  

	  /*
	   * 
	   * Add Functions for Database
	   * 
	   */
	  
	  public void addScore(int score)
	  {
		  String stmt = "INSERT INTO SCORES (score) VALUES" +
	              " ('" + score + "');";
		  SQLiteDatabase db = this.getWritableDatabase();
		  db.execSQL(stmt);
	  }
	  
	  
	  
	  
	  /*
	   * 
	   * Get Functions for Database
	   * 
	   */
	  
	  //returns unique id of major passed in
	  public String[] getScores(){
	      SQLiteDatabase db = this.getReadableDatabase();
	      Cursor res =  db.rawQuery( "SELECT * FROM SCORES ORDER BY score DESC;", null);
	      
	      res.moveToFirst();
	      res.getCount();
	      String scores[] = new String[res.getCount()];
          if (scores.length != 0) {
              scores[0] = res.getString(res.getColumnIndex("score"));
          }
	      for (int i = 1; i < res.getCount(); ++i)
	      {
	    	  res.moveToNext();
	    	  scores[i] = res.getString(res.getColumnIndex("score"));
	      }
          res.close();
	      return scores;
	   }


	} 