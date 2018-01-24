package com.example.rlowe.ramblintreks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.*;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    RequestQueue queue;
    private GoogleMap map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    @Override
    public void onMapReady(GoogleMap googleMap){
        map = googleMap;
        setUpMap();

    }

    public void setUpMap() {
        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(33.7756, -84.3963)));
        map.moveCamera(CameraUpdateFactory.zoomTo(15));
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
        //TODO

        EditText startLatText = (EditText)findViewById(R.id.startLat);
        final double startLatitude = Double.parseDouble(startLatText.getText().toString());

        EditText startLongText = (EditText)findViewById(R.id.startLong);
        final double startLongitude = Double.parseDouble(startLongText.getText().toString());

        EditText endLatText = (EditText)findViewById(R.id.endLat);
        double endLatitude = Double.parseDouble(endLatText.getText().toString());

        EditText endLongText = (EditText)findViewById(R.id.endLong);
        double endLongitude = Double.parseDouble(endLongText.getText().toString());

        JSONObject coords = new JSONObject();
        try {
            coords.put("startLatitude", startLatitude);
            coords.put("startLongitude", startLongitude);
            coords.put("endLatitude", endLatitude);
            coords.put("endLongitude", endLongitude);
        } catch (JSONException e) {
            System.out.println(e.getMessage());
        }

        String url = "http://jasongibson274.hopto.org:9003/pathing";
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, coords, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getApplicationContext(),"Received!", (short)20).show();
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

    public void drawHandler(JSONObject route){
        List<LatLng> coordinates = new ArrayList<>();

        try {
            JSONArray out = route.toJSONArray(route.names());

            Integer i;
            for (i = 0; i < out.length(); i ++) {
                //DEBUG
                Object q = out.get(i);
                String s = out.toString();

            }
        } catch (JSONException e) {
            e.getMessage();
        }
    }

}
