package com.locationtracking.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Created by Nizam on 5/24/2016.
 */
public class GPSService {

    Context context;

    public GPSService(Context context) {
        this.context = context;
    }

    public void enableLocation() {
        if (!areNetworkSettingSatisfactory(context)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
                builder.setMessage("Please switch on the location permission.");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } else {
                try {
                    LocationRequest mLocationRequestHighAccuracy = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationRequest mLocationRequestBalancedPowerAccuracy = LocationRequest.create().setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(mLocationRequestHighAccuracy);
//                            .addLocationRequest(mLocationRequestBalancedPowerAccuracy);

                    builder.setAlwaysShow(true);
                    builder.setNeedBle(true);
                    Task<LocationSettingsResponse> result =
                            LocationServices.getSettingsClient(context).checkLocationSettings(builder.build());
                    result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                            try {
                                LocationSettingsResponse response = task.getResult(ApiException.class);
                            } catch (ApiException exception) {
                                switch (exception.getStatusCode()) {
                                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                        try {
                                            ResolvableApiException resolvable = (ResolvableApiException) exception;
                                            resolvable.startResolutionForResult(
                                                    (Activity) context,
                                                    Config.REQUEST_LOCATION);
                                        } catch (IntentSender.SendIntentException e) {
                                            e.printStackTrace();
                                        } catch (ClassCastException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
                                        builder.setMessage("Please switch on the location permission.");
                                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                                context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                            }
                                        });
                                        builder.setCancelable(false);
                                        builder.show();
                                        break;
                                }
                            }
                        }
                    }).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            LocationSettingsStates status = locationSettingsResponse.getLocationSettingsStates();

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API)
                            .build();
                    mGoogleApiClient.connect();
                    LocationRequest mLocationRequest = new LocationRequest();
                    mLocationRequest.setInterval(1000);
                    mLocationRequest.setFastestInterval(500);
                    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    mLocationRequest.setSmallestDisplacement(10);

                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
                    builder.setAlwaysShow(true);


                    builder.setNeedBle(true);
                    PendingResult<LocationSettingsResult> result =
                            LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            final LocationSettingsStates state = result.getLocationSettingsStates();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied. The client can initialize location
                                    // requests here.
                                    //...

                                    //ENTER HERE THE SECOND TIME BUT WIFI IS FALSE BECAUSE WIFI STILL NO STARTS..

                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied. But could be fixed by showing the user

                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        status.startResolutionForResult(
                                                (Activity) context,
                                                Config.REQUEST_LOCATION);
                                    } catch (IntentSender.SendIntentException e) {
                                            // Ignore the error.
                                    }
                                    // }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                                    // Location settings are not satisfied. However, we have no way to fix the
                                    // settings so we won't show the dialog.
                                    //...
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isHighAccuracyEnabled(final Context context) {
        try {
            return (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE) == 3);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean areNetworkSettingSatisfactory(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return isGpsAvailable(context);
        } else {
            return isHighAccuracyEnabled(context);
        }
    }

    private static boolean isGpsAvailable(Context context) {
        return ((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    public static boolean isInternetAvailable(Context context) {
        NetworkInfo activeNetworkInfo =
                ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

}
