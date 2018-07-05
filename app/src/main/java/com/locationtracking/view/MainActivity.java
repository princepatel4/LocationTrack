package com.locationtracking.view;

import android.location.Location;
import android.location.LocationListener;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.gson.Gson;
import com.locationtracking.R;
import com.locationtracking.model.LocationModel;
import com.locationtracking.utils.APIHandler;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {


    GoogleMap mMap;
    LocationModel locationModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUI();
    }

    private void setUI(){
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        getLocation();
    }

    private void getLocation(){
        APIHandler.getsharedInstance(MainActivity.this).execute(Request.Method.GET, APIHandler.restAPI.FETCH_LOCATION, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {

                locationModel = new Gson().fromJson(response.toString(),LocationModel.class);



            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }, null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
    }
}
