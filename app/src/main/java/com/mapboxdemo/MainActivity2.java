package com.mapboxdemo;


import android.Manifest;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.mapbox.androidsdk.plugins.building.BuildingPlugin;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.models.Position;
import com.mapboxdemo.helper.FunctionHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainActivity2 extends AppCompatActivity implements MilestoneEventListener, ProgressChangeListener, MapboxMap.OnMapClickListener {
    private MapboxNavigation navigation;
    private MapView mapView;
    private MapboxMap mMapBoxMap;
    private Button btnSetNavigation;
    private NavigationMapRoute navigationMapRoute;
    private BuildingPlugin buildingPlugin;
    private Context mContext;
    LatLng point1 = new LatLng(22.3099771, 73.1754511);
    LatLng point2 = new LatLng(22.3089299, 73.1753277);
    LatLng point3 = new LatLng(22.3092947, 73.1752446);
    LatLng home = new LatLng(22.259326, 73.197862);
    private LocationLayerPlugin locationLayerPlugin;

    //    private NavigationMapRoute navigationMapRoute;
    private Bundle sis;
    private DirectionsRoute directionRoute;
    private static final int BEGIN_ROUTE_MILESTONE = 1001;
    private Position destination, waypoint;
    private Button startRouteButton;
    private DirectionsRoute mockRoute;
    private MockLocationEngine MockLocationEngine;
    private Marker currentPositionMarker;
    private boolean isFirstTimeLocal = true;
    private Location prevLocation;
    private Marker bikeMarker;
    private Icon carIcon;
    private MarkerViewOptions carViewOption;
    private Location lastUserLocation;


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
                IconFactory iconFactory = IconFactory.getInstance(mContext);
                carIcon = iconFactory.fromResource(R.drawable.ic_car_nav);

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
                            if (!running) {
                                navigationMapRoute.removeRoute(); // it will remove route when navigation end intentionally or not
                            }
                        }
                    });
                }
            }
        });

        startRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                lastUserLocation=mMapBoxMap.getMyLocation();
                mMapBoxMap.setMyLocationEnabled(false);
                MockLocationEngine = new MockLocationEngine(1000, 50, false);
                mMapBoxMap.setLocationSource(MockLocationEngine);
                ((MockLocationEngine) MockLocationEngine).setRoute(mockRoute);
                navigation.setLocationEngine(MockLocationEngine);
                navigation.startNavigation(mockRoute);
                mMapBoxMap.setOnMapClickListener(null);
            }
        });
    }

    private void doNavigation() {

        // From Mapbox to The White House
        Position origin = Position.fromLngLat(point1.getLongitude(), point1.getLatitude());//set default to my office location, ideally we need to keep device location.
        Position destination = Position.fromLngLat(home.getLongitude(), home.getLatitude());//destination is my home for just instance
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() != null) {
                            if (response.body().getRoutes().size() > 0) {
                                if (navigationMapRoute == null) {
                                    navigationMapRoute = new NavigationMapRoute(null, mapView, mMapBoxMap);
                                } else {
                                    List<DirectionsRoute> routes = response.body().getRoutes();
                                    for (DirectionsRoute route : routes) {
                                        navigationMapRoute.addRoute(route);
                                        directionRoute = route;
                                    }

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
        this.startRouteButton = (Button) findViewById(R.id.startRouteButton);
        this.btnSetNavigation = (Button) findViewById(R.id.btnSetNavigation);
        this.mapView = (MapView) findViewById(R.id.mapView);
//        MapboxNavigation navigation = new MapboxNavigation(this, getString(R.string.com_mapbox_mapboxsdk_accessToken));
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                navigation = new MapboxNavigation(mContext, AppConstant.ApiKey);//initialise
                buildingPlugin = new BuildingPlugin(mapView, mapboxMap);
                buildingPlugin.setColor(getResources().getColor(R.color.colorPrimary));
                buildingPlugin.setVisibility(true);
//                adding listeners
                navigation.addMilestoneEventListener(MainActivity2.this);
                navigation.addProgressChangeListener(MainActivity2.this);
                mapboxMap.setOnMapClickListener(MainActivity2.this);


                locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null, R.style.CustomLocationLayer);
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);

                mMapBoxMap = mapboxMap;
                mMapBoxMap.setStyle("mapbox://styles/mapbox/navigation-preview-night-v2");
                mMapBoxMap.setMyLocationEnabled(true);


                if (mMapBoxMap.getMyLocation() != null) {
                    navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap); //initialise
                    getDeviceLocation();
                }
                navigation.addMilestone(new RouteMilestone.Builder()
                        .setIdentifier(BEGIN_ROUTE_MILESTONE)
                        .setInstruction(new BeginRouteInstruction())
                        .setTrigger(
                                Trigger.all(
                                        Trigger.lt(TriggerProperty.STEP_INDEX, 3),
                                        Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 200),
                                        Trigger.gte(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS, 75)
                                )
                        ).build());
                LocationEngine locationEngine = LostLocationEngine.getLocationEngine(mContext);
                navigation.setLocationEngine(locationEngine);


            }
        });

    }

    private void getDeviceLocation() {
        mMapBoxMap.setMyLocationEnabled(true);
        if (mMapBoxMap.getMyLocation() != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mMapBoxMap.getMyLocation().getLatitude()
                            , mMapBoxMap.getMyLocation().getLongitude()
                            , mMapBoxMap.getMyLocation().getAltitude()))      // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();
            mMapBoxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000);


            carViewOption = new MarkerViewOptions().icon(carIcon);


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

    @Override
    public void onMilestoneEvent(RouteProgress routeProgress, String instruction, int identifier) {
        Timber.d("Milestone Event Occurred with id: %d", identifier);
        System.out.println("Milestone Event Occurred with id: " + identifier);
        switch (identifier) {
            case NavigationConstants.URGENT_MILESTONE:
                Toast.makeText(this, "Urgent Milestone", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.IMMINENT_MILESTONE:
                Toast.makeText(this, "Imminent Milestone", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.NEW_STEP_MILESTONE:
                Toast.makeText(this, "New Step", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.DEPARTURE_MILESTONE:
                Toast.makeText(this, "Depart", Toast.LENGTH_LONG).show();
                break;
            case NavigationConstants.ARRIVAL_MILESTONE:
                Toast.makeText(this, "Arrival", Toast.LENGTH_LONG).show();
                break;
            case BEGIN_ROUTE_MILESTONE:
                Toast.makeText(this, "you should reach your destination by", Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(this, "Undefined milestone event occurred", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        locationLayerPlugin.forceLocationUpdate(location);

     /*   Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.fractionTraveled());
        System.out.println("onProgressChange: fraction of route traveled: " + routeProgress.fractionTraveled());

        if (isFirstTimeLocal) {
            prevLocation = location;
            isFirstTimeLocal = false;
        }
*//*

        if (currentPositionMarker != null) {
            currentPositionMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
        } else {
            MarkerViewOptions options = new MarkerViewOptions().position(new LatLng(location.getLatitude(), location.getLongitude()));
            currentPositionMarker = mMapBoxMap.addMarker(options);
        }
*//*

        if (bikeMarker != null) {
            bikeMarker.remove();
        }
        float bearing = prevLocation.bearingTo(mMapBoxMap.getMyLocation());

        carViewOption.position(new LatLng(location))
                .anchor(0.5f, 0.5f)
                .rotation(bearing)
                .flat(true);
        bikeMarker = mMapBoxMap.addMarker(carViewOption);
        ValueAnimator markerAnimator = ValueAnimator.ofObject(new LatLngEvaluator(), (Object[]) new LatLng[]{new LatLng(prevLocation), new LatLng(location)});
        markerAnimator.setDuration(900);
//        markerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        markerAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        markerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (bikeMarker != null) {
                    bikeMarker.setPosition((LatLng) animation.getAnimatedValue());
                }
            }
        });
        markerAnimator.start();
        prevLocation = location;*/
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (destination == null) {
            destination = Position.fromLngLat(point.getLongitude(), point.getLatitude());
        } else if (waypoint == null) {
            waypoint = Position.fromLngLat(point.getLongitude(), point.getLatitude());
        } else {
            Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
        }
        mMapBoxMap.addMarker(new MarkerOptions().position(point));

        startRouteButton.setVisibility(View.VISIBLE);
        calculateRoute();
    }

    private void calculateRoute() {
        Location userLocation = mMapBoxMap.getMyLocation();
        if (userLocation == null) {
            Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
            return;
        }

        Position origin = Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude());
        if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
            startRouteButton.setVisibility(View.GONE);
            return;
        }

        NavigationRoute.Builder navigationRouteBuilder = NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken());
        navigationRouteBuilder.origin(origin);
        navigationRouteBuilder.destination(destination);
        if (waypoint != null) {
            navigationRouteBuilder.addWaypoint(waypoint);
        }

        navigationRouteBuilder.build().getRoute(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Timber.d("Url: %s", call.request().url().toString());
                if (response.body() != null) {
                    if (response.body().getRoutes().size() > 0) {
                        DirectionsRoute route = response.body().getRoutes().get(0);
                        MainActivity2.this.mockRoute = route;
                        navigationMapRoute.addRoute(route);

            /*for (LegStep step: route.getLegs().get(0).getSteps()) {
              mapboxMap.addMarker(new MarkerOptions().position(new LatLng(
                  step.getManeuver().asPosition().getLatitude(), step.getManeuver().asPosition().getLongitude())));
             }*/
                    }
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Timber.e("onFailure: navigation.getRoute()", throwable);
            }
        });
    }


    private class BeginRouteInstruction extends Instruction {

        @Override
        public String buildInstruction(RouteProgress routeProgress) {
            return "Have a safe trip!";
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
//        mapView.onStart();
        /*if (locationLayerPlugin != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationLayerPlugin.onStart();
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        /*if (locationLayerPlugin != null) {
            locationLayerPlugin.onStop();
        }*/
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    private class LatLngEvaluator implements TypeEvaluator<LatLng> {

        private LatLng mLatLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            mLatLng.setLatitude(startValue.getLatitude() + (endValue.getLatitude() - startValue.getLatitude()) * fraction);
            mLatLng.setLongitude(startValue.getLongitude() + (endValue.getLongitude() - startValue.getLongitude()) * fraction);
            return mLatLng;
        }
    }
}

