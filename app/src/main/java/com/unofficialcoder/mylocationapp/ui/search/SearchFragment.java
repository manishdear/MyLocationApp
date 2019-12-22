package com.unofficialcoder.mylocationapp.ui.search;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.unofficialcoder.mylocationapp.R;

import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SearchFragment extends Fragment implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    //Constants
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATIION_PERMISSION_REQUEST_CODE = 1234;
    public static final double DEFAULT_LAT = 23.3679703;
    public static final double DEFAULT_LNG = 79.0682004;
    private static final float DEFAULT_ZOOM =4.98f;
    private static final int M_MAX_ENTRIES = 5;

    //Widgets
    private ImageView info, gps;
    //Vars
    private static boolean mLocationPermissionGranted = false;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Address searchedAddress;
    private PlacesClient placesClient;
    private SupportMapFragment mapFragment;
    View root;

    private SearchViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(SearchViewModel.class);
        root = inflater.inflate(R.layout.fragment_search, container, false);

        homeViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {

            }
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        info = view.findViewById(R.id.place_info);
        gps = view.findViewById(R.id.ic_gps);

        getLocationPermission();
        setSugession();
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting Location Permission");
        String[] permission = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(getContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(getContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;

                //Initialize our Map
                initMap();

            }else{
                ActivityCompat.requestPermissions(getActivity(), permission, LOCATIION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(getActivity(), permission, LOCATIION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        mLocationPermissionGranted = false;

        switch (requestCode){
            case LOCATIION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: Permission failed");
                            return;
                        }
                    }

                    mLocationPermissionGranted = true;
                    Log.d(TAG, "onRequestPermissionsResult: Permission Granted");

                    //Initialize our Map
                    initMap();
                }
            }
        }
    }

    private void initMap(){
        Log.d(TAG, "initMap: Initilizing Map");
        SupportMapFragment supportMapFragment = (SupportMapFragment)getChildFragmentManager().findFragmentById(R.id.map_container);
        supportMapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: Map is Ready");
        this.googleMap = googleMap;

        if(mLocationPermissionGranted){
            getDeviceLocation();
        }else{
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(DEFAULT_LAT, DEFAULT_LNG), DEFAULT_ZOOM));
        }
        this.googleMap.setMyLocationEnabled(true);

        init();
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: Getting the device current Location");
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        try{
            if(mLocationPermissionGranted){
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: Found Location");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM,
                                    "Your Location");
                        }
                        else{
                            Log.d(TAG, "onComplete: Location Not Found");
                            Toast.makeText(getContext(), "Unable to find current location", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                });
            }
        }catch (Exception e){
            Log.d(TAG, "getDeviceLocation: Exception: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float defaultZoom, String your_location) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, defaultZoom));

        if(!your_location.equals("Your Location")){
            MarkerOptions options = new MarkerOptions().position(latLng)
                    .title(your_location);
            marker = googleMap.addMarker(options);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: Connection Failed: " + connectionResult.getErrorMessage());
    }

    private void init() {
        Log.d(TAG, "init: Initiliazing");
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Clicked place info");
                try {
                    if(marker.isInfoWindowShown()){
                        marker.hideInfoWindow();
                    }else{
                        marker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.d(TAG, "onClick: NullPointerException: "+ e.getMessage());
                }
            }
        });

        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Clicked gps icon");
                getDeviceLocation();
            }
        });

    }

    private void setSugession(){
        String apikey2 = "AIzaSyDIoOsso3TznDa8czihX7OeZq65NXOjVW4";
        if(!Places.isInitialized()){
            Places.initialize(getContext(), apikey2);

        }
        placesClient = Places.createClient(getContext());

        AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment)  getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if(autocompleteSupportFragment != null) {

            autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

            autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    final LatLng latLng = place.getLatLng();

                    Toast.makeText(getContext(), "latlan: " + latLng, Toast.LENGTH_SHORT).show();
                    moveCamera(new LatLng(latLng.latitude, latLng.longitude), DEFAULT_ZOOM, place.getName());

                    Log.i(TAG, "onPlaceSelected: " + latLng.latitude + "\n" + latLng.longitude);
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.d(TAG, "onError: " + status);
                }
            });
        }
    }

}