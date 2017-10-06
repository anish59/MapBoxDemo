package com.mapboxdemo;


import android.Manifest;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;
import com.mapboxdemo.helper.FunctionHelper;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private MapboxNavigation navigation;
    private com.mapbox.mapboxsdk.maps.MapView mapView;
    private MapboxMap mMapBoxMap;
    private android.widget.Button btnSetNavigation;
    private NavigationMapRoute navigationMapRoute;

    private Context mContext;
    LatLng point1 = new LatLng(22.3099771, 73.1754511);
    LatLng point2 = new LatLng(22.3089299, 73.1753277);
    LatLng point3 = new LatLng(22.3092947, 73.1752446);
    //    private NavigationMapRoute navigationMapRoute;
    private Bundle sis;
    private DirectionsRoute directionRoute;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        sis = savedInstanceState;
        Mapbox.getInstance(this, getString(R.string.com_mapbox_mapboxsdk_accessToken));
        FunctionHelper.askForPermission(mContext, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                init(sis);
                initListeners();
                if (mMapBoxMap != null) {
                    getDeviceLocation();
                }
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                return;
            }
        });

    }

    private void initListeners() {

        btnSetNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doNavigation();

                if (navigation != null) {
                    navigation.addNavigationEventListener(new NavigationEventListener() {
                        @Override
                        public void onRunning(boolean running) {
                            System.out.println("isRunning : " + running);
                            Toast.makeText(mContext, "isRunning : " + running, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });


    }

    private void doNavigation() {
        // From Mapbox to The White House
        Position origin = Position.fromLngLat(point1.getLongitude(), point1.getLatitude());
        Position destination = Position.fromLngLat(point2.getLongitude(), point2.getLatitude());
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() != null) {
                            if (response.body().getRoutes().size() > 0) {
                                if (navigationMapRoute == null) {
                                    navigationMapRoute = new NavigationMapRoute(null, mapView, mMapBoxMap);
                                } else {
                                    navigationMapRoute.addRoute(response.body().getRoutes().get(0));
                                    directionRoute = response.body().getRoutes().get(0);
                                    LocationEngine locationEngine;
                                    locationEngine = LostLocationEngine.getLocationEngine(mContext);
                                    navigation.setLocationEngine(locationEngine);
//                                    ((MockLocationEngine) locationEngine).setRoute(directionRoute);
//                                    navigation.setLocationEngine(locationEngine);
                                    navigation.startNavigation(directionRoute);
                                }
                            }

                        }


                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

                    }
                });

    }

    private void init(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        this.btnSetNavigation = (Button) findViewById(R.id.btnSetNavigation);
        this.mapView = (MapView) findViewById(R.id.mapView);
//        MapboxNavigation navigation = new MapboxNavigation(this, getString(R.string.com_mapbox_mapboxsdk_accessToken));
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mMapBoxMap = mapboxMap;
                mMapBoxMap.setStyle(Style.SATELLITE_STREETS);
                mapboxMap.setMyLocationEnabled(true);
                if (mMapBoxMap.getMyLocation() != null) {
                    navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap);
                    getDeviceLocation();
                }
                navigation = new MapboxNavigation(mContext, AppConstant.ApiKey);
              /*  LocationEngine locationEngine = LostLocationEngine.getLocationEngine(mContext);
                navigation.setLocationEngine(locationEngine);*/
            }
        });

    }

    private void getDeviceLocation() {
        mMapBoxMap.setMyLocationEnabled(true);
        if (mMapBoxMap.getMyLocation() != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(point1))      // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();
            mMapBoxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navigation != null) {
            navigation.endNavigation();
            navigation.onDestroy();
        }
    }

}

