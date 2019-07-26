package com.example.trackme;

import android.os.AsyncTask;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class GetNearByPlaces extends AsyncTask<Object,String,String> {

    private String googlePlaceData,url;
    private GoogleMap mMap;
    @Override
    protected String doInBackground(Object... objects) {
        mMap =(GoogleMap) objects[0];
        url = (String) objects[1];
        DownloadUrl downloadUrl = new DownloadUrl();
        try {
            googlePlaceData = downloadUrl.ReadtheUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return googlePlaceData;
    }

    @Override
    protected void onPostExecute(String s) {
        List<HashMap<String,String>> nearByPlacesList = null;
        DataParser dataParser = new DataParser();
        nearByPlacesList = dataParser.parse(s);
        DisplayNearByPlaces(nearByPlacesList);
    }

    private void DisplayNearByPlaces(List<HashMap<String,String>> nearByPlacesList){

        for (int i=0;i<nearByPlacesList.size();i++){
            HashMap<String,String> googleNearByPlace = new HashMap<>();
            String nameOfPlace = googleNearByPlace.get("place_name");
            String vicinity = googleNearByPlace.get("vicinity");
            Double latitude = Double.parseDouble(googleNearByPlace.get("latitude"));
            Double longitude = Double.parseDouble(googleNearByPlace.get("longitude"));
           // String reference = googleNearByPlace.get("reference");

            LatLng latLng = new LatLng(latitude,longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(nameOfPlace + " : " + vicinity));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,6));
        }
    }
}
