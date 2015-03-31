package my.infinitydots;


import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class MainActivity extends Activity {

	boolean paused = false;
	InfinityView v;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		v = new InfinityView(this);
		setContentView(v);
	}
	
	
	@Override
    public void onPause() {
        super.onPause();
        v.paused();
        v.stopThread();
    }
	
	@Override
    public void onResume() {
        super.onResume();
        	v.reMakeThread();
    }
	
}
