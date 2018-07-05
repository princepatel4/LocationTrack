package com.locationtracking.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;


import java.util.List;
import java.util.Locale;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class FetchLocation {
    private static final float ACCURACY = 400;
    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long TIME_OUT = 2 * MINUTE;
    FusedLocationProviderClient fusedLocationProviderClient;
    Runnable fusedRunnable;
    Runnable apiRunnable;
    private Context mContext;
    private LocationListener mLocationListener;
    private boolean gotlocation = false, isLogin = false;
    private Location nonAccurateLocation = null;
    LocationCallback locationCallback;
    int checkingCount = 0;


    public FetchLocation(Context mContext) {
        this.mContext = mContext;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext);
        isLogin = false;
        checkingCount = 0;
    }

    public static String getLocationName(Context mContext, Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String latitude, longitude, location_name = "";
        if (lat != 0 && lon != 0) {

            List<Address> mAddresses;
            Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
            try {

                mAddresses = gcd.getFromLocation(lat, lon, 1);
                Address address = mAddresses.get(0);
                String country = address.getCountryName();
                String street = address.getSubAdminArea();
                String locationName = address.getAdminArea();
                String locality = address.getLocality();
                String subLocality = address.getSubLocality();
                String throughFare = address.getThoroughfare();
                String subThroughFare = address.getSubThoroughfare();

                location_name = (subThroughFare != null ? (subThroughFare.isEmpty() ? "" : subThroughFare + ", ") : "")
                        + (throughFare != null ? (throughFare.isEmpty() ? "" : throughFare + ", ") : "") +
                        (subLocality != null ? (subLocality.isEmpty() ? "" : subLocality + ", ") : "") +
                        (locality != null ? (locality.isEmpty() ? "" : locality + ", ") : "") +
                        (locationName != null ? (locationName.isEmpty() ? "" : locationName + ", ") : "") +
                        (country != null ? (country.isEmpty() ? "" : country) : "");

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return location_name;
    }

    public void getLocation() {
        if (GPSService.areNetworkSettingSatisfactory(mContext)) {
            gotlocation = false;
            nonAccurateLocation = null;
            if (mContext instanceof Activity)
                getLocationFromFusedLocation();
            else
                new CustomLocationListener().requestUpdates();
//            getFusedLocation();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!gotlocation) {
                        if (mLocationListener != null) {
                            RuntimeException exception = new RuntimeException() {
                                @Override
                                public String getMessage() {
                                    return "Location not available. Please try again later";
                                }

                                @Override
                                public String getLocalizedMessage() {
                                    return "Location Fetching Timed out";
                                }
                            };
                            StackTraceElement[] element = new StackTraceElement[]{new StackTraceElement(FetchLocation.class.getSimpleName(), "getLocation", "FetchLocation.java", 95)};
                            exception.setStackTrace(element);
                            mLocationListener.onError(exception);
                            mLocationListener = null;
                            if (fusedLocationProviderClient != null && locationCallback != null)
                                fusedLocationProviderClient.removeLocationUpdates(locationCallback);

                        }
                    }
                }
            }, isLogin ? 30 * SECOND : TIME_OUT);
        } else {
            if (mLocationListener != null)
                mLocationListener.onGpsNotEnabled();
            new GPSService(mContext).enableLocation();
        }
    }

    private void getLocationFromFusedLocation() {

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                updateNonAccurateLocation(location);
                if (isValidLocation(location)) {
                    gotlocation = true;
                    if (mLocationListener != null) {
                        mLocationListener.onGetLocation(location);
                        mLocationListener = null;
                    }
                    if (fusedLocationProviderClient != null)
                        fusedLocationProviderClient.removeLocationUpdates(this);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // This should never happen
            return;
        }
        if (mContext instanceof Activity && fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, null);

        final Handler handler = new Handler();
        fusedRunnable = new Runnable() {
            @Override
            public void run() {
                if (!gotlocation) {
                    new CustomLocationListener().requestUpdates();
                }
            }
        };

        handler.postDelayed(fusedRunnable, isLogin ? 10 * SECOND : 20 * SECOND);

    }

    private void updateNonAccurateLocation(Location location) {

        if (location == null) {
            return;
        }

        if (nonAccurateLocation != null) {
            if (nonAccurateLocation.getAccuracy() > location.getAccuracy()) {
                nonAccurateLocation = location;
            }
        } else {
            nonAccurateLocation = location;
        }

    }

    private synchronized void lastResort() {

        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should never reach here
            return;
        }
        if (fusedLocationProviderClient != null) {
            Task<Location> fusedLocationTask = fusedLocationProviderClient.getLastLocation();
            fusedLocationTask.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location fusedLocation = task.getResult();
                    if (isValidLocation(fusedLocation)) {
                        if (mLocationListener != null) {
                            Log.e("location fused", " 1: " + fusedLocation.toString());
                            mLocationListener.onGetLocation(fusedLocation);
                            mLocationListener = null;
                        }
                    } else if (checkingCount <= 3) {
                        checkingCount++;
                        lastResort();
                    } else {
                        fusedLocationProviderClient = null;
                        lastResort();
                    }
                }
            }).addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Location fusedLocation = location;
                    if (isValidLocation(fusedLocation)) {
                        if (mLocationListener != null) {
                            Log.e("location fused", " 2: " + fusedLocation.toString());
                            mLocationListener.onGetLocation(fusedLocation);
                            mLocationListener = null;
                        }
                    } else if (checkingCount <= 3) {
                        checkingCount++;
                        lastResort();
                    } else {
                        fusedLocationProviderClient = null;
                        lastResort();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    fusedLocationProviderClient = null;
                    lastResort();
                }
            });

        } else if (locationManager != null) {
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (mLocationListener != null) {
                if (gpsLocation != null) {
                    mLocationListener.onGetLocation(gpsLocation);
                } else if (networkLocation != null) {
                    mLocationListener.onGetLocation(networkLocation);
                } else if (nonAccurateLocation != null) {
                    mLocationListener.onGetLocation(nonAccurateLocation);
                } else {
                    RuntimeException ex = new RuntimeException() {
                        @Override
                        public String getMessage() {
                            return "NO_PROVIDER";
                        }
                    };
                    mLocationListener.onError(ex);
                }
                mLocationListener = null;
            }
        } else {
            RuntimeException ex = new RuntimeException() {
                @Override
                public String getMessage() {
                    return "NO_PROVIDER";
                }
            };
            mLocationListener.onError(ex);
            mLocationListener = null;
        }

    }

    private boolean isValidLocation(Location mLocation) {
        if ((mLocation != null
                && !(mLocation.getLongitude() == 0
                && mLocation.getLatitude() == 0))) {
            if (mLocation.getAccuracy() <= ACCURACY) {
                return true;
            } else {
                nonAccurateLocation = mLocation;
                return false;
            }
        }
        return false;
    }

    public FetchLocation onLocation(LocationListener listener) {
        mLocationListener = listener;
        return this;
    }

    public FetchLocation isLogin(boolean isLogin) {
        this.isLogin = isLogin;
        return this;
    }

    public interface LocationListener {
        void onGetLocation(Location location);

        void onGpsNotEnabled();

        void onError(Exception e);
    }

    private class CustomLocationListener implements android.location.LocationListener {

        final LocationManager locationManager;
        boolean isGps = true;

        CustomLocationListener() {
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }

        private boolean isGPSEnabled() {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        private boolean isNetworkEnabled() {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        public void requestUpdates() {
            requestUpdates(false);
        }

        private void requestUpdates(final boolean gpsDone) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    ActivityCompat.checkSelfPermission(mContext,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // This should never happen as we are checking permission time to time.
                return;
            }

            if (isNetworkEnabled()) {
                isGps = false;
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            } else if (isGPSEnabled() && !gpsDone) {
                isGps = true;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } else {
                lastResort();
            }

            final Handler handler = new Handler();
            apiRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!gotlocation) {
                        locationManager.removeUpdates(CustomLocationListener.this);
                        if (isGps) {
                            requestUpdates(true);
                        } else {
                            lastResort();
                        }
                    }
                }
            };
            handler.postDelayed(apiRunnable, isLogin ? 10 * SECOND : 20 * SECOND);
        }

        @Override
        public void onLocationChanged(Location location) {
            updateNonAccurateLocation(location);

            if (isValidLocation(location)) {
                gotlocation = true;
                if (mLocationListener != null) {
                    mLocationListener.onGetLocation(location);
                    mLocationListener = null;
                }
                locationManager.removeUpdates(this);
            } else {
                locationManager.removeUpdates(CustomLocationListener.this);
                if (isGps) {
                    requestUpdates(true);
                } else {
                    lastResort();
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    }

    int locationReadCount = 0;

    public void getFusedLocation() {
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(mContext);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(mContext, "Permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        locationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.e("location", " : " + location.toString());
                    if (isValidLocation(location)) {
                        if (mLocationListener != null) {
                            mLocationListener.onGetLocation(location);
                            mLocationListener = null;
                        }
                    } else
                        new CustomLocationListener().requestUpdates();
                } else {
                    locationReadCount = locationReadCount + 1;
                    //Log.e("count", " : " + locationReadCount);
                    //getLocation();
                    new CustomLocationListener().requestUpdates();
                }
            }
        });
    }
}