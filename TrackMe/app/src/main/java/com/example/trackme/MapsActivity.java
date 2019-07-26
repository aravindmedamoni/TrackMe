package com.example.trackme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private Marker currentUserLastlocation;
    private static final int Request_User_location_Code = 99;
    private Double latitude,longitude;
    private int proximityRadius=10000;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkUserLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){

            buildGoogleApiClient();

            mMap.setMyLocationEnabled(true);
        }

    }

    public boolean checkUserLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},Request_User_location_Code);
            }
            else {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},Request_User_location_Code);
            }
            return false;
        }
        else {
            return true;
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode){
            case Request_User_location_Code:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
                        if (googleApiClient == null){
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }else {
                    Toast.makeText(this, "Permission Denied..", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    protected synchronized void buildGoogleApiClient(){

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        lastLocation = location;

        if (currentUserLastlocation!= null){
            currentUserLastlocation.remove();
        }

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            String str = addressList.get(0).getSubLocality()+" ";
            str +=addressList.get(0).getLocality()+" ";
            str +=addressList.get(0).getCountryName();
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title(str);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            currentUserLastlocation = mMap.addMarker(markerOptions);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (googleApiClient != null){

            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient,this);

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void onClick(View view) {

        String hospitals = "hospital", schools = "school", restaurants = "restaurant";
        Object transferData[] = new Object[2];
        GetNearByPlaces getNearByPlaces = new GetNearByPlaces();

        switch (view.getId()){
            case R.id.search_button:

                EditText addressField = findViewById(R.id.location_search);
                String address = addressField.getText().toString();

                List<Address> addressList=null;

                if (!TextUtils.isEmpty(address)){
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        addressList = geocoder.getFromLocationName(address,1);

                        if (addressList != null){

                            for (int i=0;i<addressList.size();i++){
                                Address userAddress = addressList.get(0);
                                LatLng latLng = new LatLng(userAddress.getLatitude(),userAddress.getLongitude());
                                mMap.clear();
                                mMap.addMarker(new MarkerOptions().position(latLng).title(address));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,6));
                            }
                        }else {
                            Toast.makeText(this, "Sorry,Location Not Found", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else {
                    Toast.makeText(this, "Sorry,Please Enter the place", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.hospitals_nearBy:
                mMap.clear();
                String url = getUrl(latitude,longitude,hospitals);
                transferData[0] = mMap;
                transferData[1] = url;
                getNearByPlaces.execute(transferData);
                Toast.makeText(this, "Searching for NearBy Hospitals", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "showing NearBy Hospitals", Toast.LENGTH_SHORT).show();
                break;

            case R.id.restaurants_nearBy:
                mMap.clear();
                url = getUrl(latitude,longitude,restaurants);
                transferData[0] = mMap;
                transferData[1] = url;
                getNearByPlaces.execute(transferData);
                Toast.makeText(this, "Searching for NearBy restaurants", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "showing NearBy restaurants", Toast.LENGTH_SHORT).show();
                break;

            case R.id.schools_nearBy:
                mMap.clear();
                url = getUrl(latitude,longitude,schools);
                transferData[0] = mMap;
                transferData[1] = url;
                getNearByPlaces.execute(transferData);
                Toast.makeText(this, "Searching for NearBy schools", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "showing NearBy schools", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private String getUrl(Double latitude,Double longitude,String nearByPlaces){
        StringBuilder googleURL = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googleURL.append("location=" + latitude + "," + longitude);
        googleURL.append("&radius=" + proximityRadius);
        googleURL.append("&type=" + nearByPlaces);
        googleURL.append("&sensor=true");
        googleURL.append("&key=" + "AIzaSyBwsFxY3fjhn0poL4Qni4TOHtRTEcoRx9o");

        Log.d("GoogleMapActivity","url =" +googleURL.toString());

        return googleURL.toString();
    }
}
