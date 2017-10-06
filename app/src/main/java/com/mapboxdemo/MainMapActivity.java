package com.mapboxdemo;


import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.mapbox.directions.DirectionsCriteria;
import com.mapbox.directions.MapboxDirections;
import com.mapbox.directions.service.models.DirectionsResponse;
import com.mapbox.directions.service.models.DirectionsRoute;
import com.mapbox.directions.service.models.Waypoint;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapboxdemo.helper.FunctionHelper;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;


public class MainMapActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap mMapBoxMap;
    private Button btnSetMarker;
    private Context context;
    MarkerOptions markerOptions;
    private Button btnSetPolyLine;
    private Button btnSetPolygon;

    LatLng point1 = new LatLng(22.3099771, 73.1754511);
    LatLng point2 = new LatLng(22.3089299, 73.1753277);
    LatLng point3 = new LatLng(22.3092947, 73.1752446);
    private Button btnSetRoute;
    private DirectionsRoute currentRoute;
    private Button btnGetMyLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this);
        context = this;

        FunctionHelper.askForPermission(context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                getDeviceLocation();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                return;
            }
        });
        Mapbox.getInstance(this, getString(R.string.com_mapbox_mapboxsdk_accessToken));
        init(savedInstanceState);
        initListeners();


    }

    private void initListeners() {
        btnSetMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingMarkerDemo();
            }
        });

        btnSetPolyLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                settingPolyLineDemo();
            }
        });

        btnSetPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                settingPolygonDemo();
            }
        });

        btnSetRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingRouteDemo();
            }
        });

        if (mMapBoxMap != null) {
            mMapBoxMap.setOnMapClickListener(new MapboxMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng point) {
                    Waypoint target = new Waypoint(point.getLongitude(), point.getLatitude());
                    checkOffRoute(target);
                }
            });
        }

        btnGetMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Setup UserLocation monitoring
                if (mMapBoxMap.getMyLocation() != null) {
                    getDeviceLocation();
                }
            }
        });
    }

    private void checkOffRoute(Waypoint target) {
        if (currentRoute.isOffRoute(target)) {
            Toast.makeText(context, "You are off route", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "You are not off route", Toast.LENGTH_SHORT).show();
        }
    }

    private void settingRouteDemo() {
        // Dupont Circle
        Waypoint origin = new Waypoint(point1.getLongitude(), point1.getLatitude());

        // The White House
        Waypoint destination = new Waypoint(point2.getLongitude(), point2.getLatitude());

        getRoute(origin, destination);
    }

    private void getRoute(Waypoint origin, Waypoint destination) {
        MapboxDirections md = new MapboxDirections.Builder()
                .setAccessToken(getString(R.string.com_mapbox_mapboxsdk_accessToken))
                .setOrigin(origin)
                .setDestination(destination)
                .setProfile(DirectionsCriteria.PROFILE_WALKING)
                .build();

        md.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Response<DirectionsResponse> response, Retrofit retrofit) {
                // You can get generic HTTP info about the response
                Log.d("TAG", "Response code: " + response.code());

                // Print some info about the route
                currentRoute = response.body().getRoutes().get(0);
                Log.d("TAG", "Distance: " + currentRoute.getDistance());
                Toast.makeText(context, "Route is %d meters long. : " + currentRoute.getDistance(), Toast.LENGTH_SHORT).show();

                Log.d("TAG", "Distance: " + currentRoute.getDuration());
                Toast.makeText(context, "Route is %d sec far away : " + currentRoute.getDuration(), Toast.LENGTH_SHORT).show();

                // Draw the route on the map
                drawRoute(currentRoute);
            }


            @Override
            public void onFailure(Throwable t) {
                Log.e("TAG", "Error: " + t.getMessage());
                Toast.makeText(context, "err: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(DirectionsRoute currentRoute) {
        // Convert List<Waypoint> into LatLng[]
        List<Waypoint> waypoints = currentRoute.getGeometry().getWaypoints();
        LatLng[] point = new LatLng[waypoints.size()];
        for (int i = 0; i < waypoints.size(); i++) {
            point[i] = new LatLng(
                    waypoints.get(i).getLatitude(),
                    waypoints.get(i).getLongitude());
        }

        // Draw Points on MapView
        mMapBoxMap.addPolyline(new PolylineOptions()
                .add(point)
                .color(Color.parseColor("#3887be"))
                .width(5));
    }


    private void settingPolygonDemo() {
        List<LatLng> points = new ArrayList<LatLng>();
        points.add(point1);
        points.add(point2);
        points.add(point3);
        // Draw a polygon on the map
        mMapBoxMap.addPolygon(new PolygonOptions()
                .addAll(points)
                .fillColor(Color.parseColor("#3bb2d0")));
    }

    private void settingPolyLineDemo() {
        List<LatLng> points = new ArrayList<LatLng>();
        points.add(point1);
        points.add(point2);
        points.add(point3);

        mMapBoxMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#000000"))
                .width(2));
    }

    private void settingMarkerDemo() {
        addMapMarker(new LatLng(22.3089299, 73.1753277)); //adding simple marker
        addMarkerWithIcon(context, new LatLng(22.3092947, 73.1752446), R.drawable.ic_airport_shuttle); //adding marker with custom icon
        // getWindowInfo(); see if its possible
    }


    private void addMarkerWithIcon(Context context, LatLng latLng, int drawableSrc) {
        IconFactory iconFactory = IconFactory.getInstance(context);
        Icon icon = iconFactory.fromResource(drawableSrc);

        // Add the marker to the map
        mMapBoxMap.addMarker(new MarkerViewOptions()
                .position(new LatLng(latLng))
                .title("hey yaa")
                .icon(icon));
    }

    private void init(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main_map);
        this.btnGetMyLocation = (Button) findViewById(R.id.btnGetMyLocation);
        this.btnSetRoute = (Button) findViewById(R.id.btnSetRoute);
        this.btnSetPolygon = (Button) findViewById(R.id.btnSetPolygon);
        this.btnSetPolyLine = (Button) findViewById(R.id.btnSetPolyLine);
        this.btnSetMarker = (Button) findViewById(R.id.btnSetMarker);
        this.mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                mapboxMap.setStyle(Style.SATELLITE_STREETS);
                // One way to add a marker view
                /*
                    if you want to init your marker at the starting itself you can call it from here,
                    that is after you get mapBoxMap object
                */
                mMapBoxMap = mapboxMap;
               /* mapboxMap.addMarker(new MarkerViewOptions()
                        .position(new LatLng(22.3099771, 73.1754511))
                        .title("Webmyne")
                        .snippet("This is where I am working!")
                );*/

                getDeviceLocation();

            }
        });

//        addMapMarker(new LatLng(22.3093816, 73.1755584));


    }

    private void getDeviceLocation() {
        mMapBoxMap.setMyLocationEnabled(true);
        if (mMapBoxMap.getMyLocation() != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(point1.getLatitude(), point1.getLongitude()))      // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();
            mMapBoxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);
            addMapMarker(new LatLng(point1.getLatitude(), point1.getLongitude()));
        }
    }

    private void addMapMarker(LatLng latLng) {

        mMapBoxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng))
                .title("Amantran")
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


}
