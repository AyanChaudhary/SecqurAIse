package com.example.secqraise.Others

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtil(val application: Application, val context: Context) {

    fun checkPermission():Boolean{
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(application,
            Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && ContextCompat.checkSelfPermission(context,
            Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    }

    fun checkGPS():Boolean{
        val locationManager=application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun requstPermission() {
        ActivityCompat.requestPermissions(
            context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.CAMERA),
            Constants.PERMISSION_REQUEST_CODE_FOR_LOCATION)
    }

    fun checkCameraPermission():Boolean{
        return ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(){
        ActivityCompat.requestPermissions(context as Activity,
            arrayOf(Manifest.permission.CAMERA),Constants.PERMISSION_REQUEST_CODE_FOR_CAMERA)
    }
}