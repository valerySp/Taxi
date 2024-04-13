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

import com.example.taxi.databinding.ActivityCustmersMapBinding;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    private ActivityCustmersMapBinding binding;

    private int radius=1;
    private boolean driverFound=false,requestType;
    private String driverFoundId;

    GoogleApiClient googleApiClient;
    Location lastLocation;
    com.google.android.gms.location.LocationRequest locationRequest;
    private Button logOutCustomerBtn,callTaxiBtn;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String customerID;
    private LatLng customerPosition;
    private DatabaseReference driversAvailableRef;
    private DatabaseReference driverThisRef;
    private DatabaseReference driverLocRef;
    private ValueEventListener driverLocRefListener;
    Marker driverMarker, pickUPMarker;
    GeoQuery geoQuery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCustmersMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logOutCustomerBtn=findViewById(R.id.customer_logout_btn);
        callTaxiBtn=findViewById(R.id.customer_order_btn);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        customerID=currentUser.getUid();

        driversAvailableRef=FirebaseDatabase.getInstance().getReference().child("Driver Available");
        driverLocRef=FirebaseDatabase.getInstance().getReference().child("Driver Working");

        logOutCustomerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //currentLogOutDriverStatus=true;
                mAuth.signOut();

                logOutDriver();
                disconnectDriver();
            }
        });

        callTaxiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (requestType){
                    requestType=false;
                    geoQuery.removeAllListeners();
                    driverLocRef.removeEventListener(driverLocRefListener);

                    if (driverFoundId!=null){
                        driverThisRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                                .child(driverFoundId).child("CustomerRideID");
                        driverThisRef.removeValue();
                        driverFoundId=null;
                    }
                    driverFound=false;
                    radius=1;
                    GeoFire geoFire=new GeoFire(FirebaseDatabase.getInstance().getReference().child("Customer Request "));
                    geoFire.removeLocation(customerID);

                    if(pickUPMarker !=null){
                        pickUPMarker.remove();
                    }
                    if(driverMarker !=null){
                        driverMarker.remove();
                    }

                    callTaxiBtn.setText("Вызвать такси");
                }
                else{
                    requestType=true;
                    GeoFire geoFire=new GeoFire(FirebaseDatabase.getInstance().getReference().child("Customer Request "));
                    geoFire.setLocation(customerID,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    customerPosition=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(customerPosition).title("Im here!").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiBtn.setText("Поиск такси...");

                    getNearbyDrivers();
                }
            }
        });
    }

    private void getNearbyDrivers() {
        GeoFire geoFire=new GeoFire(driversAvailableRef);
        GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(customerPosition.latitude,customerPosition.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound&&requestType){
                    driverFound=true;
                    driverFoundId=key;

                    driverThisRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                            .child(driverFoundId);
                    HashMap driverMap=new HashMap();
                    driverMap.put("CustomerRideID",customerID);
                    driverThisRef.updateChildren(driverMap);
                    
                    getDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound){
                    radius++;
                    getNearbyDrivers();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverLocation() {
        driverLocRefListener=driverLocRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()&&requestType){
                            List<Object>driverLocMap=(List<Object>) snapshot.getValue();
                            double locLat=0;
                            double locLan=0;
                            callTaxiBtn.setText("Водитель найден.");

                            if (driverLocMap.get(0)!=null){
                                locLat=Double.parseDouble(driverLocMap.get(0).toString());
                            }
                            if (driverLocMap.get(1)!=null){
                                locLan=Double.parseDouble(driverLocMap.get(1).toString());
                            }

                            LatLng driverLatLng=new LatLng(locLat,locLan);
                            if (driverMarker!=null){
                                driverMarker.remove();
                            }

                            Location location=new Location("");
                            location.setLatitude(customerPosition.latitude);
                            location.setLongitude(customerPosition.longitude);

                            Location location2=new Location("");
                            location2.setLatitude(driverLatLng.latitude);
                            location2.setLongitude(driverLatLng.longitude);

                            float distance=location.distanceTo(location2);
                            if (distance>100){
                                callTaxiBtn.setText("Ваше такси подъезжает" );
                            }
                            else {
                                callTaxiBtn.setText("Расстояние до такси "+String.valueOf(distance));
                            }


                            driverMarker=mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Ваше такси тут..")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void logOutDriver() {
        Intent intent=new Intent(CustomersMapActivity.this,Welcome.class);
        startActivity(intent);
        finish();
    }

    private void disconnectDriver() {
        String userID= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference customerAvalabityRef= FirebaseDatabase.getInstance().getReference().child("Driver ");

        GeoFire geoFire=new GeoFire(customerAvalabityRef);
        geoFire.removeLocation(userID);
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

    protected synchronized void buildGoogleApiClient(){
        googleApiClient=new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
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
        lastLocation =location;
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}