package ca.uwaterloo.lab4_201_01;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import android.app.Activity;
import android.app.Fragment;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MainActivity extends Activity{
	LinearLayout layout;
	static MapView mv; 
	List<PointF> pointList = new ArrayList<PointF>();
	List<InterceptPoint> interceptPoint = new ArrayList<InterceptPoint>(); 
	static PointF startPoint;
	static PointF endPoint;
	static PointF userPoint;
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        
        layout = (LinearLayout) findViewById(R.id.container);
        
        //code for map
        mv = new MapView(getApplicationContext(),1200,700,30,30);
        registerForContextMenu(mv);
        final NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null),"E2-3344.svg");
        mv.setMap(map);      
                
        mv.addListener(new PositionListener(){

			@Override
			public void originChanged(MapView source, PointF loc) {
				userPoint = loc;
				startPoint = loc; //assign a start point to be referenced within our code
				pointList.add(loc);
			}

			@Override
			public void destinationChanged(MapView source, PointF dest) {
				endPoint = dest;
				interceptPoint = map.calculateIntersections(startPoint, endPoint);
				mv.setUserPoint(userPoint);
				for(int i = 1; i < interceptPoint.size(); i++){
					//if we're in the dead zone
					if(startPoint.x < 5 && startPoint.y > 20 
							&& endPoint.x > 19 && endPoint.y > 19){
						if(interceptPoint.get(i).getLine().end.x - interceptPoint.get(i).getLine().start.x < 1){
							//how to handle the line if it is vertical
							if(interceptPoint.get(i).getLine().end.y > interceptPoint.get(i).getLine().start.y){
								pointList.add(interceptPoint.get(i).getLine().start);
							}else{
								pointList.add(interceptPoint.get(i).getLine().end);
							}
						}else{
							//how to handle the line if it is horizontal
							if(interceptPoint.get(i).getLine().end.x > interceptPoint.get(i).getLine().start.x){
								pointList.add(interceptPoint.get(i).getLine().start);
							}else{
								pointList.add(interceptPoint.get(i).getLine().end);
							}
						}
					}else{//if we're anywhere else on the map
						if(interceptPoint.get(i).getLine().end.x - interceptPoint.get(i).getLine().start.x < 1){
							//how to handle the line if it is vertical
							if(interceptPoint.get(i).getLine().end.y > interceptPoint.get(i).getLine().start.y){
								pointList.add(interceptPoint.get(i).getLine().end);
							}else{
								pointList.add(interceptPoint.get(i).getLine().start);
							}
						}else{
							//how to handle the line if it is horizontal
							if(interceptPoint.get(i).getLine().end.x > interceptPoint.get(i).getLine().start.x){
								pointList.add(interceptPoint.get(i).getLine().start);
							}else{
								pointList.add(interceptPoint.get(i).getLine().end);
							}
						}
					}
				}
				pointList.add(endPoint);				
		        mv.setUserPath(pointList); //add destination to the list later on
			}
        }); 
        layout.addView(mv);
        
    }
	//the method updateUserPoint updates the user's current position
	public static void updateUserPoint(double addedX, double addedY){
		userPoint.x = (float) addedX + userPoint.x;
		userPoint.y = (float) addedY + userPoint.y;
    }
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo){
		super.onCreateContextMenu(menu, v, menuInfo);
		mv.onCreateContextMenu(menu,v,menuInfo);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item){
		return super.onContextItemSelected(item)|| mv.onContextItemSelected(item);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class PlaceholderFragment extends Fragment {
    	
    	float[] mGravity;
    	Button resetButton;
    	Button startButton;
    	int stepsCount = 0;
    	double stepsCountNorth = 0;
    	double stepsCountEast = 0;
    	float angleReading = 0;
    	double addedXdirection;
    	double addedYdirection;
    	
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            
			LinearLayout layout = (LinearLayout) rootView
					.findViewById(R.id.linear);
			
            
            /*final TextView outputX = (TextView)rootView.findViewById(R.id.outputX);
        	final TextView outputY = (TextView)rootView.findViewById(R.id.outputY);
        	final TextView outputZ = (TextView)rootView.findViewById(R.id.outputZ);*/
            
        	final TextView orientationReading  = new TextView(rootView.getContext());
        	final TextView angle  = new TextView(rootView.getContext());
        	final TextView steps  = new TextView(rootView.getContext());
        	final TextView stepsNorth = new TextView(rootView.getContext());
        	final TextView stepsEast = new TextView(rootView.getContext());

        	
        	steps.setText("Steps: ");
        	stepsNorth.setText("Steps North: ");
        	stepsEast.setText("Steps East: ");
        	
        	//button is declared above, linked to our View here
        	resetButton = (Button) rootView.findViewById(R.id.reset);
  	      	
        	//<ACCELEROMETER
            final SensorManager sensorManager = (SensorManager)rootView.getContext().getSystemService(SENSOR_SERVICE); 
            final Sensor acceleroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            final SensorEventListener acceleroListener = new SensorEventListener(){
            	float accelerationReadingX;
            	float accelerationReadingY;
            	float accelerationReadingZ;
            	
            	
            	
            	boolean greaterThan = false;
            	boolean time = false;
            	
            	long timeStart = 0;
            	long timeEnd = 0;
            	double timeElapsed = 0;

				@Override
				public void onSensorChanged(SensorEvent event) {
					//graph.addPoint(event.values);
					/* outputX.setText("X-direction acceleration: " + String.valueOf(new DecimalFormat("##.##").format((event.values[0]))) + " m/s squared");
					outputY.setText("Y-direction acceleration: " + String.valueOf(new DecimalFormat("##.##").format((event.values[1]))) + " m/s squared");
					outputZ.setText("Z-direction acceleration: " + String.valueOf(new DecimalFormat("##.##").format((event.values[2]))) + " m/s squared");	*/
					mGravity = event.values;
					accelerationReadingX = event.values[0];
					accelerationReadingY = event.values[1];
					accelerationReadingZ = event.values[2];
					
					if(accelerationReadingY>1.5){
						greaterThan = true;
						timeStart = System.nanoTime();
					}
					
					if(accelerationReadingY<0 && greaterThan){
						timeEnd = System.nanoTime();
						timeElapsed = (timeEnd - timeStart)/Math.pow(10, 9);
						
						if(timeElapsed<1.5 && timeElapsed>0){
							time = true;
							timeElapsed = 0;
						}
					}
					
					if(time && greaterThan){
						stepsCount = stepsCount + 1;
						steps.setText(String.valueOf("Steps: " + stepsCount));
						stepsCountNorth = stepsCountNorth + Math.cos(angleReading);			
						stepsNorth.setText(String.valueOf("Steps North: " + String.valueOf(stepsCountNorth)));
						stepsCountEast = stepsCountEast + Math.sin(angleReading);
						stepsEast.setText(String.valueOf("Steps East: " + String.valueOf(stepsCountEast)));
						
						if(startPoint != null){
							addedXdirection = (double) 0.8*Math.sin(angleReading);
							addedYdirection = (double) 0.8*Math.cos(angleReading);
							updateUserPoint(addedXdirection, addedYdirection);
							mv.setUserPoint(userPoint);
						}
						greaterThan = false;
						time = false;
					}
					
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					
					
				}
            };
            sensorManager.registerListener(acceleroListener,acceleroSensor,SensorManager.SENSOR_DELAY_FASTEST);
            
            //ACCELEROMETER/>
            
            //MAGNETIC
           
            final Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            final SensorEventListener magneticListener = new SensorEventListener(){
            	double magReadingX = 0;
            	double magReadingY = 0;
            	double magReadingZ = 0;
            	float[] mGeomagnetic;

				@Override
				public void onSensorChanged(SensorEvent event) {
					magReadingX = event.values[0];
					magReadingY = event.values[1];
					magReadingZ = event.values[2];
					mGeomagnetic = event.values;
					
					if ( mGravity!= null && mGeomagnetic != null) {
						float R[] = new float[9];
						float I[] = new float[9];
						if (SensorManager.getRotationMatrix(R, I,mGravity, mGeomagnetic)) {
							
							// orientation contains azimut, pitch and roll
							float orientation[] = new float[3];	
							SensorManager.getOrientation(R, orientation);
							angleReading = (float) (orientation[0]/Math.PI)*180;
							angle.setText("Orientation: " + String.valueOf(angleReading));
							
							

						}
					}
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {
					// TODO Auto-generated method stub
					
				}
            	
            };
            sensorManager.registerListener(magneticListener,magneticSensor,SensorManager.SENSOR_DELAY_FASTEST);
            //MAGNETIC />
            
            
            resetButton.setOnClickListener(
        			new Button.OnClickListener(){
						public void onClick(View v) {
							stepsCount = 0;
							steps.setText("Steps: " + String.valueOf(stepsCount));
							
							stepsCountNorth = 0;
							stepsNorth.setText("Steps North: " + String.valueOf(stepsCountNorth));
							
							stepsCountEast = 0;
							stepsEast.setText("Steps East: " + String.valueOf(stepsCountEast));
						}
        			}
        	);
            
        	
        	final TextView title = new TextView(rootView.getContext());
        	title.setText("-----Steps Taken--------");
        	
        	final TextView title2 = new TextView(rootView.getContext());
        	title2.setText("-----Displacement(steps)--------");
        	
        	final TextView title3 = new TextView(rootView.getContext());
        	title3.setText("-----Extra--------");
        	
            //add textViews
            layout.addView(title);
            layout.addView(steps);
            
            layout.addView(title2);            	
            layout.addView(stepsNorth);
            layout.addView(stepsEast);
           
            layout.addView(title3);
            layout.addView(angle);
            return rootView;
        }
    }


	
}
