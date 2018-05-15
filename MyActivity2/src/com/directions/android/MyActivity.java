package com.directions.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.directions.route.Maneuver;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@SuppressLint("NewApi") public class MyActivity extends Activity implements RoutingListener
{	
	//MAP VARIABLES+++++++++++++++++++++++++++++++++++++++++++
    protected GoogleMap map;
    protected static LatLng start;
    protected static LatLng end;
    protected static Location destinationLoc;
    protected static Location currentLocation;
    protected static Location lastLocation;
    protected static float lastDistance;
    protected static boolean travelling;
    double lng, lat;
    EditText destination;
    protected int alternate;
    
    //TURN-VARIABLES+++++++++++++++++++++++++++++++++++++++++++
    public static final int SIG_INIT = 0;
    public static final int SIG_LEFT_TURN = 3;
    public static final int SIG_RIGHT_TURN = 4;
    public static final int SIG_RAMP_LEFT = 5;
    public static final int SIG_RAMP_RIGHT = 6;
    public static final int SIG_MISSED = 7;
    public static final int SIG_ARRIVED = 8;
    
    //FILTER-INDEX+++++++++++++++++++++++++++++++++++++++++++
    private int i1 = 0;
    float avg[] = {0,0,0};
    
    //AUDIO+++++++++++++++++++++++++++++++++++++++++++
    private AudioManager audio;
    private static final String CMDTOGGLEPAUSE = "togglepause";
    private static final String CMDPAUSE = "pause";
    private static final String CMDPREVIOUS = "previous";
    private static final String CMDNEXT = "next";
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDSTOP = "stop";
    private static final String CMDPLAY = "play";

    //MANEUVERS++++++++++++
    protected static List<Maneuver> maneuvers;
    protected static int index;
    //MANEUVERS++++++++++++
    
    //BLUETOOTH+++++++++++++++++++++++++++++++++++++++++++
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // Layout Views
    //private TextView mTitle;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    //BLUETOOTH+++++++++++++++++++++++++++++++++++++++++++
    
    //String array for comparing to next maneuver
    public String [] googleMan = {
    		"turn-left",
    		"turn-right",
    		"ramp-left",
    		"ramp-right",
    		"fork-left",
    		"fork-right",
    		"turn-sharp-left",
    		"turn-sharp-right",
    		"uturn-left",
    		"uturn-right",
    		"turn-slight-left",
    		"turn-slight-right",
    		"merge",
    		"roundabout-left",
    		"roundabout-right",
    		"straight",
    		"ferry-train"
    };
    
    /**
     * This activity loads a map and then displays the route and pushpins on it.
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD) @SuppressLint("NewApi") @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);  
        
        //Setup Audio stream
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); //allows side buttons to change volume for music
        
        //Initialize starting location
        index = 0;
        lastDistance = 1000000000;
        maneuvers = new ArrayList<Maneuver>();
        travelling = false;
        alternate = 0;
        
        //IF start is null, set location to montreal or 0,0.
       
        if (start != null ) {
        	final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            final Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    		start =  new LatLng(location.getLatitude(), location.getLongitude());
        }
        //Else use starting location as last known location
        else {
        	start =  new LatLng(0, 0);
        }
        //-----------------------------------------------------------
	        
        end = start;
        
        MapFragment fm = (MapFragment)  getFragmentManager().findFragmentById(R.id.map);
        map = fm.getMap();
        map.setMyLocationEnabled(true);	
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        //map.getMyLocation();
        
        destination = (EditText) findViewById(R.id.textBox);
        
        //clear button
        Button button = (Button)findViewById(R.id.clearMapButton);
        button.setOnClickListener(new View.OnClickListener() {
        	@Override
			public void onClick(View v) {
            	clearApp();
            }
        });
        
        //BLUETOOTH STUFF++++++++++++++++++++++++
        //mTitle = (TextView) findViewById(R.id.title_right_text);
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        //BLUETOOTH STUFF++++++++++++++++++++++++
        
        //to permit something to work
        StrictMode.ThreadPolicy ourPolicy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(ourPolicy);
        
        //when the user clicks finished on the keyboard
        destination.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                	routeToMap();
                	return true;
                }
                return false;
            }
        });
        
        //LOCATION UPDATER +++++++++++++++++++++
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //LocationManager locationManager2 = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
        	public void onLocationChanged(Location location) {
        		// Called when a new location is found by the network location provider.
        		start = new LatLng(location.getLatitude(), location.getLongitude());
        		if(travelling){
        			locationUpdate(location);
        		}
        	}

        	public void onStatusChanged(String provider, int status, Bundle extras) {}
        	public void onProviderEnabled(String provider) {}
        	public void onProviderDisabled(String provider) {}
        };
         
        // Register the listener with the Location Manager to receive location updates
        if ( (alternate%2) != 0) {
        	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
        else {
        	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
        alternate++;
        //LOCATION UPDATER +++++++++++++++++++++
        
        
        CameraUpdate center=CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom=  CameraUpdateFactory.zoomTo(15);

        map.moveCamera(center);
        map.animateCamera(zoom);
    }
    
    public void routeToMap(){
    	
    	String ad = destination.getText().toString();
    	
    	clearApp();
    	
    	ad = ad.replaceAll(" ","%20");
    	
    	//make the keyboard go away.
    	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(destination.getWindowToken(), 0);
    	
    	destination.setText(getLatLongFromAddress(ad));
    	
    	if(start!=end){
    		displayRoute();
    	}
    }
    
    public void locationUpdate(Location location){
		currentLocation = location;
		
		// I think index <maneuvers.size() instead of index < maneuvers.size-1.
		if (index<maneuvers.size() ) {
			Maneuver man = maneuvers.get(index);
			LatLng loc = man.getLoc();
			Location nextMan = getLocation(loc);
			
			float distance = location.distanceTo(nextMan);
			distance = average(distance);
			String doostance = new String(""+ distance);
			Toast.makeText(this, doostance, Toast.LENGTH_SHORT).show();
			Log.d("distance", doostance);
			
			if(distance-25>lastDistance){
				//sendMessage(SIG_MISSED);
				//reroute
				//routeToMap();
			}
			else if (distance < 15) {
				String distString = man.getMove();
				boolean test;

				int i;
				for (i=0;i<googleMan.length;i++) {
					test = distString.equals(googleMan[i]);
					if (test) {
						switch (i) {
						case 0:
							sendMessage(SIG_LEFT_TURN);
							nextManeuver(location);
							break;
						case 2:
							sendMessage(SIG_LEFT_TURN);
							nextManeuver(location);
							break;
						case 4:
							sendMessage(SIG_LEFT_TURN);
							nextManeuver(location);
							break;
						case 1:
							sendMessage(SIG_RIGHT_TURN);
							nextManeuver(location);
							break;
						case 3:
							sendMessage(SIG_RIGHT_TURN);
							nextManeuver(location);
							break;
						case 5:
							sendMessage(SIG_RIGHT_TURN);
							nextManeuver(location);
							break;
						default:
							Toast.makeText(this, "Unrecognized Maneuver", Toast.LENGTH_SHORT).show();
							break;
						}
					}
				}
			}
			else if (distance < 50 ) {
	            String distString = man.getMove();
	            boolean test;
	            
	            int i;
	            for (i=0;i<googleMan.length;i++) {
	                test = distString.equals(googleMan[i]);
	                if (test) {
			            switch (i) {
			            case 0:
			            	sendMessage(SIG_RAMP_LEFT);
			                break;
			            case 2:
			            	sendMessage(SIG_RAMP_LEFT);
			                break;
			            case 4:
			                sendMessage(SIG_RAMP_LEFT);
			                break;
			            case 1:
			            	sendMessage(SIG_RAMP_RIGHT);
			                break;
			            case 3:
			            	sendMessage(SIG_RAMP_RIGHT);
			                break;
			            case 5:
			            	sendMessage(SIG_RAMP_RIGHT);
			                break;
			            default:
			            	Toast.makeText(this, "Unrecognized Maneuver", Toast.LENGTH_SHORT).show();
			            	break;
			            }
	                }
	            }
	            lastDistance = distance;
			}
			else{
				lastDistance = distance;
			}
			lastLocation = currentLocation;
		}
		//arrived signal
		if((location.distanceTo(destinationLoc)<10) && (index==maneuvers.size()-1)){
			sendMessage(SIG_ARRIVED);
			//clearApp();
			//travelling = false;
		}
    }
    
    public void nextManeuver(Location location){
    	if(index < (maneuvers.size()-1) ){
			LatLng next = maneuvers.get(index+1).getLoc();
			Location nextLoc = getLocation(next);
			
			lastDistance = nextLoc.distanceTo(location);
			
    		index++;
    	}
    	
    }
    
    public static Location getLocation(LatLng pos){
		Location loc = new Location("loc");
		loc.setLatitude(pos.latitude);
		loc.setLongitude(pos.longitude);
		return loc;
    }
    
    public void displayRoute(){
    	//Change to biking later
    	Routing routing = new Routing(Routing.TravelMode.BIKING);
        routing.registerListener(this);
        routing.execute(start, end);
    }
    
    public void clearApp(){
    	index=0;
    	maneuvers.clear();
    	map.clear();
    	destination.setText("");
    	travelling=false; 
        lastDistance = 1000000000;
    }

    @Override
    public void onRoutingFailure() {
      // The Routing request failed
    	Toast.makeText(this, R.string.route_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
      // The Routing Request starts
    	//Toast.makeText(this, R.string.route_init, Toast.LENGTH_SHORT).show();
    	sendMessage(SIG_INIT);
    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route) {
      PolylineOptions polyoptions = new PolylineOptions();
      polyoptions.color(Color.MAGENTA);
      polyoptions.width(10);
      polyoptions.addAll(mPolyOptions.getPoints());
      map.addPolyline(polyoptions);
      
      // Start marker
      MarkerOptions options = new MarkerOptions();
      options.position(start);
      options.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue));
      map.addMarker(options);
      
      // End marker
      options = new MarkerOptions();
      options.position(end);
      options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_blue));  
      map.addMarker(options);
      
      //OUR STUFF++++++++++++++++
      maneuvers = route.getManeuvers();
      options.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_orange));
      for(int i=0;i<maneuvers.size();i++){
    	  options.position(maneuvers.get(i).getLoc());
    	  options.title(maneuvers.get(i).getMove());
    	  map.addMarker(options);
      }
      travelling = true;
      //OUR STUFF++++++++++++++++
      
    }
    
    public static String getLatLongFromAddress(String youraddress) {
        String uri = "http://maps.google.com/maps/api/geocode/json?address=" +
                      youraddress + "&sensor=false";
        HttpGet httpGet = new HttpGet(uri);
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();
        
        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());

            
            double lng = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
                .getDouble("lng");

            double lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
                .getDouble("lat");
            
            end = new LatLng(lat, lng);
            destinationLoc = getLocation(end);
            
            return ((JSONArray)jsonObject.get("results")).getJSONObject(0).getString("formatted_address");
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        end = start;
        return "";

    }
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        /*mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });*/

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /*private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }*/

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(int message) {
    	String myMessage = "";
        // Check that we're actually connected before trying anything
        /*if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            //return;
        }*/

        // Check that there's actually something to send
        if (message >= 0) {

        	switch(message){
        		case SIG_INIT:
        			myMessage="0";
        			Toast.makeText(this, R.string.nav_init, Toast.LENGTH_SHORT).show();
        			break;
        		
        		case SIG_LEFT_TURN:
        			myMessage="3";
        			Toast.makeText(this, R.string.nav_left, Toast.LENGTH_SHORT).show();
        			break;

        		case SIG_RIGHT_TURN:
        			myMessage="4";
        			Toast.makeText(this, R.string.nav_right, Toast.LENGTH_SHORT).show();
        			break;
        	    
        		case SIG_RAMP_LEFT:
        			myMessage="5";
        			Toast.makeText(this, R.string.nav_ramp_left, Toast.LENGTH_SHORT).show();
        			break;
        	    
        		case SIG_RAMP_RIGHT:
        			myMessage="6";
        			Toast.makeText(this, R.string.nav_ramp_right, Toast.LENGTH_SHORT).show();
        			break;
        	    
        		case SIG_MISSED:
        			myMessage="7";
        			Toast.makeText(this, R.string.nav_missed, Toast.LENGTH_SHORT).show();
        			break;
        	   
        		case SIG_ARRIVED:
        			myMessage="8";
        			Toast.makeText(this, R.string.nav_arrived, Toast.LENGTH_SHORT).show();
        			break;
        	}
        	
        	
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = myMessage.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }
    
    //Average filter
    private float average (float dist) {
    	int i2 = i1%3;    	
    	avg[i2] = dist;
    	float avgDist = (avg[0]+avg[1]+avg[2])/3;
    	i1++;
    	return avgDist;
    }
    
    //AUDIO CONTROLS
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    public void audioCommand(String cmd) {
    	
    	Intent i = new Intent(SERVICECMD);
    	
    	if(audio.isMusicActive()) {
    	    //PLAY/PAUSE MUSIC. This is 2.
    	    if (cmd.equals("2")) {
    	    	i.putExtra(CMDNAME , CMDTOGGLEPAUSE );
    	    	MyActivity.this.sendBroadcast(i);
    	    }
    	    //NEXT SONG. This is 3.
    	    else if (cmd.equals("3")) {
    	    	i.putExtra(CMDNAME , CMDNEXT );
    	    	MyActivity.this.sendBroadcast(i);
    	    }
    	    //PREVIOUS SONG. This is 1.
    	    else if (cmd.equals("1")) {
    	    	i.putExtra(CMDNAME , CMDPREVIOUS );
    	    	MyActivity.this.sendBroadcast(i);
    	    }
    	    //DO NOTHING
    	    else {}
    	}
    	else if (cmd.equals("2")) {
    		//PLAY MUSIC
    	    	i.putExtra(CMDNAME , CMDPLAY );
    	    	MyActivity.this.sendBroadcast(i);
    	}
    }
    
    //5 is volume up
    public void volume_up() {
    	audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }
    
    //4 is volume down
    public void volume_down() {
    	audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }
  //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    /*mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);*/
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            //READ AUDIO SIGNALS FROM BLUETOOOTH
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                String savedMsg = new String(readMessage);
                Toast.makeText(getApplicationContext(), savedMsg, Toast.LENGTH_SHORT).show();
                
                if (savedMsg.equals("5")){
                	volume_up();
                }
                else if (savedMsg.equals("4")) {
                	volume_down();
                }
                else {
                	audioCommand(savedMsg);
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        //case R.id.discoverable:
            // Ensure this device is discoverable by others
        //    ensureDiscoverable();
        //    return true;
        }
        return false;
    }
}
//+++++++++++++++++++++++++++++++++++++++++++END+++++++++++++++++++++++++++++++++++++++++++END+++++++++++++++++++++++++++++++++++++++++++