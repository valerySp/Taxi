package com.example.taxi;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.taxi.databinding.ActivityDriverMapsBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private ActivityDriverMapsBinding binding;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    com.google.android.gms.location.LocationRequest locationRequest;
    private Button logOutDriverBtn,settDriverBtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean currentLogOutDriverStatus=false;
    private DatabaseReference assignedCustRef, positionCustRef;
    private String driverID,customerID;
    private ValueEventListener assignedCustomerPosListener;
    Marker pickUPMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver_maps);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        driverID=currentUser.getUid();

        logOutDriverBtn=findViewById(R.id.driver_logout_btn);
        settDriverBtn=findViewById(R.id.driver_settings_btn);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(DriverMapsActivity.this,SettingsActivity.class);
                intent.putExtra("type","Drivers");
                startActivity(intent);
                finish();
            }
        });

        logOutDriverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentLogOutDriverStatus=true;
                mAuth.signOut();

                logOutDriver();
                disconnectDriver();
            }
        });

        getAssignedCustomerReq();
    }



    private void getAssignedCustomerReq() {
        assignedCustRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                .child(driverID).child("CustomerRideID");

        assignedCustRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    customerID=snapshot.getValue().toString();
                    getAssignedCustomerPosition();
                }
                else {
                    customerID="";
                    if(pickUPMarker!=null){
                        pickUPMarker.remove();
                    }
                    if (assignedCustomerPosListener!=null){
                        positionCustRef.removeEventListener(assignedCustomerPosListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getAssignedCustomerPosition() {
        positionCustRef=FirebaseDatabase.getInstance().getReference().child("Customers Request")
                .child(customerID).child("l");

        assignedCustomerPosListener=positionCustRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    List<Object> customerPositionMap=(List<Object>) snapshot.getValue();
                    double locLat=Double.parseDouble(customerPositionMap.get(0).toString());;
                    double locLan=Double.parseDouble(customerPositionMap.get(1).toString());;

                    LatLng driverLatLng=new LatLng(locLat,locLan);
                    pickUPMarker=mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Забрать клиента тут..").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLatLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void logOutDriver() {
        Intent intent=new Intent(DriverMapsActivity.this,Welcome.class);
        startActivity(intent);
        finish();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(100000);
        locationRequest.setFastestInterval(100000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
       if (getApplicationContext()!=null){
           lastLocation =location;
           LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
           mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
           mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

           String userID= FirebaseAuth.getInstance().getCurrentUser().getUid();

           DatabaseReference driverAvalabityRef= FirebaseDatabase.getInstance().getReference().child("Driver ");
           GeoFire geoFireAvailability=new GeoFire(driverAvalabityRef);


           DatabaseReference driverWorkingRef= FirebaseDatabase.getInstance().getReference().child("Driver Working");
           GeoFire geoFireWorking=new GeoFire(driverWorkingRef);


           switch (customerID){
               case "":
                   geoFireWorking.removeLocation(userID);
                   geoFireAvailability.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
                   break;
               default:
                   geoFireAvailability.removeLocation(userID);
                   geoFireWorking.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
                   break;
           }
       }
    }

    protected synchronized void buildGoogleApiClient(){
        googleApiClient=new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!currentLogOutDriverStatus){
            disconnectDriver();
        }
    }

    private void disconnectDriver() {
        String userID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverAvalabityRef= FirebaseDatabase.getInstance().getReference().child("Driver ");

        GeoFire geoFire=new GeoFire(driverAvalabityRef);
        geoFire.removeLocation(userID);
    }
}