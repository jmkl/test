
package org.hermit.touchtest;


import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;


public class TouchTest extends Activity {	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                     WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.requestFeature(Window.FEATURE_NO_TITLE);
		
		gridView = new GridView(this);
		setContentView(gridView);
	}
	
    private GridView gridView;
    
}

