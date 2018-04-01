package com.example.rlowe.ramblintreks;

import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import org.json.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    //Request Queue used for JSON Requests
    public RequestQueue queue;

    //Instance of GoogleMap object seen in app
    private GoogleMap map;

    private CameraPosition mCameraPosition;

    //the list of coordinates to be used to draw path on map
    public List<LatLng> coordinates;

    //local collection of GT buildings with coordinates
    public JSONArray gtBuildings;

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mLocationPermissionGranted;

    private static final int DEFAULT_ZOOM = 20;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    //Default Location for the map (Georgia Tech Campus)
    private final LatLng mDefaultLocation = new LatLng(33.776433, -84.4015629);

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    //Keys for storing activity state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    //Map of Building Names to Coordinates
    private static Map<String, LatLng> buildingMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Retrieve location and camera position from saved state
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(getApplicationContext());
        getGTBuildings();

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (map != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, map.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }

    }

    public void getGTBuildings() {
        String gtCall = "https://m.gatech.edu/api/gtplaces/buildings";
        JsonArrayRequest call = new JsonArrayRequest(gtCall, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                gtBuildings = response;
                for (int i = 0; i < gtBuildings.length(); i++) {
                    try {
                       JSONObject place = gtBuildings.getJSONObject(i);
                       String name = place.getString("name");
                       double lat = place.getDouble("latitude");
                       double lon = place.getDouble("longitude");
                       buildingMap.put(name, new LatLng(lat, lon));
                    } catch (JSONException e) {
                        System.out.println(e.getMessage());
                    }
                }

                //Spinner dd = findViewById(R.id.spinner);
                List<String> buildings = new ArrayList<>();
                Iterator itr = buildingMap.keySet().iterator();

                while (itr.hasNext()) {
                    String name = (String) itr.next();
                    buildings.add(name);
                }

                String[] b = new String[buildings.size()];
                b = buildings.toArray(b);

                Arrays.sort(b);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.simple_spinner_dropdown_item_custom, b);

                //dd.setAdapter(adapter);

                AutoCompleteTextView actv = findViewById(R.id.autoCompleteTextView);
                actv.setAdapter(adapter);
                actv.setThreshold(3);

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.getMessage());
            }
        });

        queue.add(call);
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap){
        map = googleMap;
        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        //sets misc fields for the map object
        setUpMap();
    }

    public void setUpMap() {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setBuildingsEnabled(true);
        coordinates = new ArrayList<>();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void pathHandler(View view) {
        double startLatitude = mLastKnownLocation.getLatitude();
        double startLongitude = mLastKnownLocation.getLongitude();
        //EditText endLatText = (EditText)findViewById(R.id.endLat);
        //EditText endLongText = (EditText)findViewById(R.id.endLong);
        //double endLatitude = Double.parseDouble(endLatText.getText().toString());
        //double endLongitude = Double.parseDouble(endLongText.getText().toString());
        //Spinner loc = findViewById(R.id.spinner);
        //String dest = loc.getSelectedItem().toString();
        AutoCompleteTextView a= findViewById(R.id.autoCompleteTextView);
        String dest = a.getEditableText().toString();
        LatLng endPoint = buildingMap.get(dest);

        JSONObject coords = new JSONObject();
        try {
            coords.put("startLatitude", startLatitude);
            coords.put("startLongitude", startLongitude);
            coords.put("endLatitude", endPoint.latitude);
            coords.put("endLongitude", endPoint.longitude);
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }

        String url = "http://jasongibson274.hopto.org:9003/pathing";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, coords, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getApplicationContext(),"Received!", (short)20).show();
                coordinates.clear();
                drawHandler(response);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),error.getMessage(), (short)20).show();


            }
        });

        queue.add(request);

    }

    /**
     * Method that returns a LatLng based on an index into a JSONObject
     * @param i index of point
     * @param r JSONObject to parse
     * @return LatLng object at index i of r
     */
    public LatLng getPoint(int i, JSONObject r) {
        try {
            String curr = r.getJSONObject(Integer.toString(i)).toString();
            String[] latLong = curr.split(",");
            String[] latS = latLong[1].split(":");
            String[] longS = latLong[2].split(":");
            longS[1] = longS[1].substring(0, longS[1].length()-1);

            double lat = Double.parseDouble(latS[1]);
            double lng = Double.parseDouble(longS[1]);

            LatLng co = new LatLng(lat, lng);
            return co;

        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    /**
     * Method that returns color of point i in jsonobject r
     * @param i index of point
     * @param r JSONObject to parse
     * @return Color of point at index i in r
     */
    public int getColor(int i, JSONObject r) {
        try {
            String curr_point = r.getJSONObject(Integer.toString(i)).toString();
            String[] latLong = curr_point.split(",");
            String[] color = latLong[0].split(":");
            int curr_col = Color.parseColor(color[1].substring(1, color[1].length()-1));
            return curr_col;

        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }

        return 0;
    }

    public void handleSnappedPath(JSONObject r, int c) {
        try {
            List<LatLng> p = new ArrayList<>();
            JSONArray points = r.getJSONArray("snappedPoints");
            for (int i = 0; i < points.length(); i++) {
                JSONObject point = points.getJSONObject(i);
                String loc = point.getString("location");
                String[] latLong = loc.split(",");
                String[] lat = latLong[0].split(":");
                String[] lon = latLong[1].split(":");
                double latP = Double.parseDouble(lat[1]);
                double longP = Double.parseDouble(lon[1].substring(0, lon[1].length()-1));
                LatLng newPoint = new LatLng(latP, longP);
                p.add(newPoint);
            }

            createLine(c, p);
            p.clear();

        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }
    }

    public void createLine(int c, List<LatLng> p) {
        Polyline walkLine = map.addPolyline(new PolylineOptions().clickable(true).addAll(p));
        walkLine.setEndCap(new RoundCap());
        walkLine.setStartCap(new RoundCap());
        walkLine.setColor(c);
        walkLine.setWidth(12);
        p.clear();
    }


    public void drawHandler(JSONObject route){
        map.clear();
        JSONArray out;
        List<LatLng> tempPoints = new ArrayList<>();

        try {
            out = route.toJSONArray(route.names());

            for (int i = 0; i < out.length() - 1; i ++) {
                final int curr_col = getColor(i, route);
                int next_col = getColor(i+1, route);

                LatLng curr_point = getPoint(i, route);

                if (curr_col == next_col) {
                    tempPoints.add(curr_point);
                } else if (next_col == 0) {
                    tempPoints.add(curr_point);

                    //Create Snap-To-Road Path
                    String request = "path=";
                    for (int j = 0; j < tempPoints.size(); j++) {
                        request = request.concat(Double.toString(tempPoints.get(j).latitude) + "," + Double.toString(tempPoints.get(j).longitude));
                        if (j != tempPoints.size()-1) {
                            request = request.concat("|");
                        }
                    }

                    String url = "https://roads.googleapis.com/v1/snapToRoads?" + request + "&interpolate=true" + "&key=AIzaSyCnvTOVY8FCw7S73Bv-gUOgG4b0Owe1iT0";
                    JsonObjectRequest snapPath = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            handleSnappedPath(response, curr_col);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });

                    tempPoints.clear();
                    queue.add(snapPath);

                } else {
                    tempPoints.add(curr_point);
                    createLine(curr_col, tempPoints);
                    tempPoints.clear();
                }
            coordinates.add(curr_point);
            }

            int l = out.length()-1;
            String bearing = Double.toString(route.getDouble("orientation"));
            float b = Float.valueOf(bearing);

            CameraPosition curr = map.getCameraPosition();

            CameraPosition cameraPosition = new CameraPosition.Builder(curr)
                .target(coordinates.get(0))
                .zoom(20)
                .bearing(360 - b)
                .tilt(45)
                .build();

            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        } catch (JSONException e) {
            e.getMessage();
        }
    }
    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                            //Fills Start Coordinates Text Boxes with Current Location
                            //EditText startLatText = (EditText)findViewById(R.id.startLat);
                            //startLatText.setText(Double.toString(mLastKnownLocation.getLatitude()));
                            //EditText startLongText = (EditText)findViewById(R.id.startLong);
                            //startLongText.setText(Double.toString(mLastKnownLocation.getLongitude()));
                            //EditText endLatText = (EditText)findViewById(R.id.endLat);
                            //endLatText.requestFocus();
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            map.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            map.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
