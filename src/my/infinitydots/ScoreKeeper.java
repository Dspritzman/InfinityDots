package my.infinitydots;

public class ScoreKeeper {
	
	private int score;
	
	private int scoreIncrement;
	
	public ScoreKeeper()
	{
		score = 0;
		scoreIncrement = 5;
	}
	
	public void addScore(int amt)
	{
		score += scoreIncrement * amt;
	}
	
	public void addBonusScore(int amt)
	{
		score += amt;
	}
	
	public int getScore()
	{
		return score;
	}
	
	public void resetScore()
	{
		score = 0;
	}

}
