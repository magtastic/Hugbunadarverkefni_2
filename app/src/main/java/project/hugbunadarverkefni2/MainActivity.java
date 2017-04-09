package project.hugbunadarverkefni2;

import android.*;
import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;


public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, OnMapReadyCallback {
    private static final int PLACE_PICKER_REQUEST = 1;
    private GoogleApiClient mGoogleApiClient;
    private ListView listOfEventsView;
    private List<Event> events;
    private EventManager eventManager = new EventManager();
    private Location mLastLocation;
    private EditText minDayFilterInput;
    private EditText maxDayFilterInput;
    private EditText minAttFilterInput;
    private EditText maxAttFilterInput;
    private Filter activeFilter = new Filter(0,Integer.MAX_VALUE,0,Integer.MAX_VALUE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermissionToReadUserContacts();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .build();
        }


        // Find the toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Sets the Toolbar to act as the ActionBar for this Activity window.
        // Make sure the toolbar exists in the activity and is not null
        setSupportActionBar(toolbar);
        listOfEventsView = (ListView) findViewById(R.id.list_of_events);
    }



    private static final int ACCESS_LOCATION_PERMISSIONS_REQUEST = 1;

    // Called when the user is performing an action which requires the app to read the
    // user's contacts
    public void getPermissionToReadUserContacts() {
        // 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid
        // checking the build version since Context.checkSelfPermission(...) is only available
        // in Marshmallow
        // 2) Always check for permission (even if permission has already been granted)
        // since the user can revoke permissions at any time through Settings
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // The permission is NOT already granted.
            // Check if the user has been asked about this permission already and denied
            // it. If so, we want to give more explanation about why the permission is needed.
            if ((ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION))) {
                // Show our own UI to explain to the user why we need to read the contacts
                // before actually requesting the permission and showing the default UI
            }

            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_LOCATION_PERMISSIONS_REQUEST);
        }
    }

    // Callback with the request from calling requestPermissions(...)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // Make sure it's our original READ_CONTACTS request
        if (requestCode == ACCESS_LOCATION_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read Contacts permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // showRationale = false if user clicks Never Ask Again, otherwise true
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale( this, android.Manifest.permission.ACCESS_FINE_LOCATION);

                if (showRationale) {
                    // do something here to handle degraded mode
                } else {
                    Toast.makeText(this, "Read Contacts permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            eventManager.fetchFBEvents(String.valueOf(mLastLocation.getLatitude()), String.valueOf(mLastLocation.getLongitude()), this, displayEvents);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connection:", "Suspended");
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem attendeesMenuItem = menu.findItem(R.id.action_filter_attendees);
        final MenuItem daysUntilMenuItem = menu.findItem(R.id.action_filter_daysUntil);

        MenuItemCompat.OnActionExpandListener attendeesExpandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {

                if(daysUntilMenuItem.isActionViewExpanded()){
                    daysUntilMenuItem.collapseActionView();
                }

                minAttFilterInput = (EditText) menuItem.getActionView().findViewById(R.id.attendees_min);
                maxAttFilterInput = (EditText) menuItem.getActionView().findViewById(R.id.attendees_max);

                minAttFilterInput.setOnEditorActionListener(new TextView.OnEditorActionListener(){

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                        if(!minAttFilterInput.getText().toString().equals("")) {
                            activeFilter.setMinAttendees(Integer.parseInt(minAttFilterInput.getText().toString()));
                        }

                        if(!maxAttFilterInput.getText().toString().equals("")) {
                            activeFilter.setMaxAttendees(Integer.parseInt(maxAttFilterInput.getText().toString()));
                        }
                        displayEvents.run();
                        return false;
                    }
                });
                maxAttFilterInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if(!minAttFilterInput.getText().toString().equals("")) {
                            activeFilter.setMinAttendees(Integer.parseInt(minAttFilterInput.getText().toString()));
                        }
                        if(!maxAttFilterInput.getText().toString().equals("")) {
                            activeFilter.setMaxAttendees(Integer.parseInt(maxAttFilterInput.getText().toString()));
                        }

                        displayEvents.run();
                        return false;
                    }
                });

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                return true;
            }
        };

        MenuItemCompat.setOnActionExpandListener(attendeesMenuItem, attendeesExpandListener);

        /////////////////////////////////////////////
        // Days Until
        /////////////////////////////////////////////
        MenuItemCompat.OnActionExpandListener daysUntilExpandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {

                minDayFilterInput = (EditText) menuItem.getActionView().findViewById(R.id.days_until_min);
                maxDayFilterInput = (EditText) menuItem.getActionView().findViewById(R.id.days_until_max);

                if(attendeesMenuItem.isActionViewExpanded()){
                    attendeesMenuItem.collapseActionView();
                }

                minDayFilterInput.setOnEditorActionListener(new TextView.OnEditorActionListener(){

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if(!maxDayFilterInput.getText().toString().equals("")) {
                            activeFilter.setMaxDaysUntil(Integer.parseInt(maxDayFilterInput.getText().toString()));
                        }
                        if(!minDayFilterInput.getText().toString().equals("")) {
                            activeFilter.setMinDaysUntil(Integer.parseInt(minDayFilterInput.getText().toString()));
                        }


                        displayEvents.run();
                        return false;
                    }
                });
                maxDayFilterInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if(!minDayFilterInput.getText().toString().equals("")) {
                            activeFilter.setMinDaysUntil(Integer.parseInt(minDayFilterInput.getText().toString()));
                        }
                        if(!maxDayFilterInput.getText().toString().equals("")) {
                            activeFilter.setMaxDaysUntil(Integer.parseInt(maxDayFilterInput.getText().toString()));
                        }

                        displayEvents.run();
                        return false;
                    }
                });

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                return true;
            }
        };

        MenuItemCompat.setOnActionExpandListener(daysUntilMenuItem, daysUntilExpandListener);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                // Click the search icon
                try {
                    PlacePicker.IntentBuilder intentBuilder =
                            new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build(MainActivity.this);
                    startActivityForResult(intent, PLACE_PICKER_REQUEST);

                } catch (GooglePlayServicesRepairableException
                        | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                return true;

            default:

                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onMapReady(GoogleMap map) {

        List<Event> eve = eventManager.getShownEvents();

            map.addMarker(new MarkerOptions().anchor(0.35f, 0.87f).position(new LatLng(64.1265,-21.8174)).title("Titill"));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(64.1265,-21.8174),14.0f));


    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();

                // search string
                LatLng latLng = place.getLatLng();

                //fetch Events
                eventManager.fetchFBEvents(String.valueOf(latLng.latitude), String.valueOf(latLng.longitude), this, displayEvents);

            }
        }
    }

    // callback function
    public Runnable displayEvents = new Runnable() {
        @Override
        public void run() {
            eventManager.applyActiveFilter(activeFilter);

            final List<Event> allEvents = eventManager.getShownEvents();

            String[] eventsTitles = new String[allEvents.size()];
            String[] profilePictures = new String[allEvents.size()];
            for(int i = 0; i<eventsTitles.length; i++) {
                eventsTitles[i] = allEvents.get(i).getTitle();
                profilePictures[i] = allEvents.get(i).getProfilePhotoSrc();
            }

            CustomListAdapter adapter = new CustomListAdapter(MainActivity.this, eventsTitles, profilePictures );

            listOfEventsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    // Fragment
                    FragmentManager manager = getFragmentManager();
                    Fragment frag = manager.findFragmentByTag("single_event_fragment");

                    if(frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }

                    EventFragment eventFragment = EventFragment.newInstance(allEvents.get(position));

                    eventFragment.show(manager, "single_event_fragment");
                }
            });
            listOfEventsView.setAdapter(adapter);
        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("Connection:", connectionResult.toString());
    }

}
