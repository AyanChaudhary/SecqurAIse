package com.example.secqraise

import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.secqraise.Others.BatteryStatus
import com.example.secqraise.Others.ConnectionLiveData
import com.example.secqraise.Others.Constants
import com.example.secqraise.Others.PermissionUtil
import com.example.secqraise.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var perms : PermissionUtil
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private lateinit var cld : ConnectionLiveData
    private lateinit var imageUri: Uri
    private var capCount=0
    val storageRef= Firebase.storage.reference

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            val time=binding.tvValFrequency.text.toString().toLong()
            performAllFunctions()
            Log.d("MainActivity", "Task executed")

            handler.postDelayed(this, time* 60 * 1000)
        }
    }


    private val contract= registerForActivityResult(ActivityResultContracts.TakePicture()){
        binding.ivPhoto.setImageURI(null)
        binding.ivPhoto.setImageURI(imageUri)
        capCount+=1
        uploadToFirebaseStorage()
        Toast.makeText(this@MainActivity,
            "next upload will be done after ${binding.tvValFrequency.text.toString()} mins",
            Toast.LENGTH_SHORT).show()
    }
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        perms= PermissionUtil(application,this)
        imageUri=createImageUri()!!
        handler.post(runnable)
        binding.btnManualDataRefresh.setOnClickListener {
            performAllFunctions()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }



    private  fun performAllFunctions() {
        binding.ivPhoto.setImageURI(null)
        val batteryStatus=BatteryStatus(applicationContext)
        binding.tvValCapcount.text=(capCount+1).toString()
        binding.tvValBatteryCharge.text=batteryStatus.getBatteryPercentage().toString()
        binding.tvValBatteryCharging.text=if(batteryStatus.isCharging())"ON" else "OFF"
        imageUri=createImageUri()!!
        checkNetworkConnection()
        getCurrentLocation()
        binding.tvDataTime.text=currentFormattedTime()
        startCamera()
    }

    private fun startCamera() {
        if(perms.checkCameraPermission()){
            contract.launch(imageUri)
        }
        else{
            perms.requestCameraPermission()
        }
    }

    private fun uploadToFirebaseStorage()= CoroutineScope(Dispatchers.IO).launch {
        try {
            imageUri.let {
                val uid=UUID.randomUUID().toString()
                val imageRef=storageRef.child("images/$uid")
                imageRef.putFile(it).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener {
                        addDataTodataBase(it.toString())
                    }
                }
            }
        }
        catch (e:Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity,"${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addDataTodataBase(image: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            val uid=System.currentTimeMillis().toString()
            val ref=FirebaseDatabase.getInstance().getReference("/data/$uid")
            val data=Data(binding.tvDataTime.text.toString(),image,capCount.toString(),binding.tvValFrequency.text.toString(),
            binding.tvValConnectivity.text.toString(),binding.tvValBatteryCharging.text.toString(),binding.tvValBatteryCharge.text.toString(),
            binding.tvValLocation.text.toString())
            ref.setValue(data).addOnSuccessListener {
                Toast.makeText(this@MainActivity,"upload successful", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createImageUri() : Uri?{
        val image=File(applicationContext.filesDir,"camera_photo.png")
        return FileProvider.getUriForFile(applicationContext,"com.example.secqraise.fileProvider",
        image)
    }
    private fun checkNetworkConnection() {
        cld=ConnectionLiveData(application)
        cld.observe(this){ isConnected->
            if(isConnected){
                binding.tvValConnectivity.text="ON"
            }
            else {
                binding.tvValConnectivity.text = "OFF"
            }

        }
    }
    private fun getCurrentLocation() {
        if(perms.checkPermission()){
            if(perms.checkGPS()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){task->
                    val location: Location?=task.result
                    location?.let {
                        Log.d("TAG", "getCurrentLocation: ${it.latitude}, ${it.longitude}")
                        binding.tvValLocation.text="${it.latitude}, ${it.longitude}"
                    }?:Toast.makeText(this,"location NULL",Toast.LENGTH_SHORT).show()
                }
            }else{
                Snackbar.make(binding.root,"Please enable the location",Snackbar.LENGTH_SHORT).show()
            }
        }else{
            perms.requstPermission()
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode==Constants.PERMISSION_REQUEST_CODE_FOR_CAMERA){
            if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Camera permission granted", Toast.LENGTH_SHORT).show()
                performAllFunctions()
            }else{
                perms.requestCameraPermission()
                Toast.makeText(this,"Camera permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
        if(requestCode== Constants.PERMISSION_REQUEST_CODE_FOR_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"granted", Toast.LENGTH_SHORT).show()
                getCurrentLocation()
            }else{
                Toast.makeText(this,"permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private fun currentFormattedTime() : String{
        val time=System.currentTimeMillis()
        val fullDateAndTime=Date(time)
        val sdf=SimpleDateFormat("dd/MM/yyyy HH:mm a", Locale.ENGLISH)
        return sdf.format(fullDateAndTime)
    }
}