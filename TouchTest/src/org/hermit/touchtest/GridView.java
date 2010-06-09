
package org.hermit.touchtest;


import java.util.HashMap;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;


class GridView
    extends SurfaceView
{

    public GridView(Context context) {
        super(context);

        appResources = context.getResources();
        appConfig = appResources.getConfiguration();
    	windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    	appDisplay = windowManager.getDefaultDisplay();

        trackedPointers = new Pointer[MAX_POINTERS];
        for (int i = 0; i < MAX_POINTERS; ++i)
            trackedPointers[i] = new Pointer();

        paint = new Paint();
        paint.setTextSize(20f);
        paint.setTypeface(Typeface.MONOSPACE);

        setBackgroundColor(0xFF404040);
    }


    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        sw = w;
        sh = h;

        // Create the edge markers.
        edgeMarker = new Path();
        int x, y;
        
        // Top
        x = sw / 2;
        y = 0;
        edgeMarker.moveTo(x, y);
        edgeMarker.lineTo(x + ARROW_SIZE, y + ARROW_SIZE);
        edgeMarker.lineTo(x - ARROW_SIZE, y + ARROW_SIZE);
        edgeMarker.close();
        
        // Right
        x = sw;
        y = sh / 2;
        edgeMarker.moveTo(x, y);
        edgeMarker.lineTo(x - ARROW_SIZE, y - ARROW_SIZE);
        edgeMarker.lineTo(x - ARROW_SIZE, y + ARROW_SIZE);
        edgeMarker.close();
        
        // Bottom
        x = sw / 2;
        y = sh;
        edgeMarker.moveTo(x, y);
        edgeMarker.lineTo(x + ARROW_SIZE, y - ARROW_SIZE);
        edgeMarker.lineTo(x - ARROW_SIZE, y - ARROW_SIZE);
        edgeMarker.close();
        
        // Left
        x = 0;
        y = sh / 2;
        edgeMarker.moveTo(x, y);
        edgeMarker.lineTo(x + ARROW_SIZE, y - ARROW_SIZE);
        edgeMarker.lineTo(x + ARROW_SIZE, y + ARROW_SIZE);
        edgeMarker.close();
        
        edgeMarker.close();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        lastActions[lastActionsIndex++] = action;
        lastActionsIndex %= STORED_ACTIONS;

        int npointers = event.getPointerCount();

        // Get the action and pointer ID.
        int pact = action & ~MotionEvent.ACTION_POINTER_ID_MASK;
        int pid = (action & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;

        // Update the up/down state of this pointer.
        if (pact == MotionEvent.ACTION_DOWN || pact == MotionEvent.ACTION_POINTER_DOWN) {
            trackedPointers[pid].down = true;
        } else if (pact == MotionEvent.ACTION_UP || pact == MotionEvent.ACTION_POINTER_UP) {
            trackedPointers[pid].down = false;
        } else if (pact == MotionEvent.ACTION_MOVE) {
            trackedPointers[pid].down = true;
        }

        // Save the pointer states for all the pointers.
        for (int i = 0; i < npointers; ++i) {
            int p = event.getPointerId(i);
            Pointer rec = trackedPointers[p];
            
            rec.seen = true;
            rec.x = event.getX(i);
            rec.y = event.getY(i);
            rec.size = event.getSize(i);
        }

        invalidate();

        return true;
    }

    
    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(0xff000000);
        
        // Draw the touch grid.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(0xff0060b0);
        for (int x = 0; x < sw; x += GRID_SPACING)
            canvas.drawLine(x, 0, x, sh, paint);
        for (int y = 0; y < sh; y += GRID_SPACING)
            canvas.drawLine(0, y, sw, y, paint);
        
        // Draw the edge markers.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffffff00);
        canvas.drawPath(edgeMarker, paint);

        // Draw the user's fingers.
        for (int i = 0; i < MAX_POINTERS; ++i) {
            Pointer rec = trackedPointers[i];
            if (!rec.seen)
                continue;
        	int col = POINTER_COLOURS[i % POINTER_COLOURS.length];
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(rec.down ? col : 0x80ffffff);
            canvas.drawCircle(rec.x, rec.y, rec.size * 2 + 24, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1f);
            canvas.drawLine(rec.x, 0, rec.x, sh, paint);
            canvas.drawLine(0, rec.y, sw, rec.y, paint);
            paint.setStrokeWidth(0f);
            canvas.drawText("" + i, rec.x + 8, rec.y, paint);
        }
        
        // Draw the display configuration.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0f);
        paint.setColor(0xffffa0a0);
        int y = 60;
		String size = appDisplay.getWidth() + "x" + appDisplay.getHeight();
        canvas.drawText(size, 30, y, paint);
        y += 22;
		String rot = translateToken(appDisplay.getOrientation(), DISP_ORIENTS);
        canvas.drawText(rot, 30, y, paint);
        y += 22;
        
        // Draw the most recent action codes.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0f);
        paint.setColor(0xffffa0a0);
        y = 60;
        for (int i = 0; i < STORED_ACTIONS; ++i) {
            int code = lastActions[(lastActionsIndex + i) % STORED_ACTIONS];
            String action = String.format("%4d 0x%04x", code, code);
            canvas.drawText(action, sw - 140, y, paint);
            y += 22;
        }
    }

    
    private String translateToken(int val, HashMap<Integer, String> map) {
    	String name = map.get(val);
    	if (name != null)
    		return name;
    	return "?<" + val + ">?";
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	private static final String TAG = "SensorTest";

	private static final HashMap<Integer, String> CONFIG_ORIENTS =
										new HashMap<Integer, String>();
	static {
		CONFIG_ORIENTS.put(Configuration.ORIENTATION_LANDSCAPE, "LAND");
		CONFIG_ORIENTS.put(Configuration.ORIENTATION_PORTRAIT, "PORT");
		CONFIG_ORIENTS.put(Configuration.ORIENTATION_SQUARE, "SQUARE");
		CONFIG_ORIENTS.put(Configuration.ORIENTATION_UNDEFINED, "UNDEF");
	}

	private static final HashMap<Integer, String> DISP_ORIENTS =
										new HashMap<Integer, String>();
	static {
		DISP_ORIENTS.put(Surface.ROTATION_0, "0 - Upright");
		DISP_ORIENTS.put(Surface.ROTATION_90, "90 - Left");
		DISP_ORIENTS.put(Surface.ROTATION_180, "180 - Inverted");
		DISP_ORIENTS.put(Surface.ROTATION_270, "270 - Right");
	}

    private static final int GRID_SPACING = 50;
    
    private static final int ARROW_SIZE = 80;
    
    private static final int STORED_ACTIONS = 10;
   
    private static final int MAX_POINTERS = 6;
    private class Pointer {
        boolean seen = false;
        boolean down = false;
        float x = 0;
        float y = 0;
        float size = 0;
    }
    
    private static final int[] POINTER_COLOURS = {
    	0xffffa000, 0xff00ffff, 0xff80ff00, 0xff80ff80,
    };

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	private Resources appResources;
	private Configuration appConfig;
	
	private WindowManager windowManager;
	private Display appDisplay;

    // Screen width and height.
    private int sw = 0, sh = 0;

    // Path for drawing the edge markers.
	private Path edgeMarker;

    // Most recently reported touch event action.
    private int[] lastActions = new int[STORED_ACTIONS];
    private int lastActionsIndex = 0;
    
    // Tracked pointer states.
    private Pointer[] trackedPointers = null;
    
    // Paint used for drawing.
    private Paint paint;

}

