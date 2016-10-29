package captainhampton.com.spaceapps;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import java.util.concurrent.TimeUnit;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions public class UserLocationActivity extends AppCompatActivity
    implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

  private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  private static final int RESULT_LOAD_IMAGE = 1;
  private static final int REQUEST_IMAGE_CAPTURE = 2;
  //http://stackoverflow.com/questions/36921700/butterknife-first-use-error
  @BindView(R.id.tvUserLatitude) TextView tvUserLatitude;
  @BindView(R.id.tvUserLongitude) TextView tvUserLongitude;
  @BindView(R.id.ivImageToUpload) ImageView ivImageToUpload;
  //Define a request code to send to Google Play services
  private GoogleApiClient mGoogleApiClient;
  private LocationRequest mLocationRequest;
  private double currentLatitude;
  private double currentLongitude;
  private Uri selectedImage;
  private JobManager jobManager;

  @OnClick(R.id.ivCaptureImage) void captureImage() {
    UserLocationActivityPermissionsDispatcher.showCameraWithCheck(this);
  }

  @OnClick(R.id.ivImageToUpload) void pickImage() {
    UserLocationActivityPermissionsDispatcher.readStorageWithCheck(this);
  }

  @OnClick(R.id.bUploadImage) void uploadImage() {
    UploadImageJob job = UploadImageJob.newJob(selectedImage, currentLatitude, currentLongitude);
    jobManager.addJobInBackground(job);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    UserLocationActivityPermissionsDispatcher.getLocationWithCheck(this);
    jobManager = new JobManager(new Configuration.Builder(this).build());
  }

  @Override protected void onStop() {
    super.onStop(); // google api is auto managed on stop
    if (mGoogleApiClient.isConnected()) {
      LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
  }

  @Override public void onLocationChanged(Location location) {
    setLatLon(location);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
      selectedImage = data.getData();
      ivImageToUpload.setImageURI(selectedImage);
    } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
      // if there are no errors, then consolidate this else if into the first one
      selectedImage = data.getData();
      ivImageToUpload.setImageURI(selectedImage);
    }
  }

  @Override public void onConnected(Bundle bundle) {
    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

    if (location == null) {
      LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,
          this);
    } else {
      //If everything went fine lets get latitude and longitude
      setLatLon(location);
    }
  }

  private void setLatLon(Location location) {
    currentLatitude = location.getLatitude();
    currentLongitude = location.getLongitude();

    Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG)
        .show();
    tvUserLatitude.setText(Double.toString(currentLatitude));
    tvUserLongitude.setText(Double.toString(currentLongitude));
  }

  @Override public void onConnectionSuspended(int i) {
  }

  @Override public void onConnectionFailed(ConnectionResult connectionResult) {
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
    if (connectionResult.hasResolution()) {
      try {
        // Start an Activity that tries to resolve the error
        connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
      } catch (IntentSender.SendIntentException e) {
        // Log the error
        e.printStackTrace();
      }
    } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
      Log.e("Error",
          "Location services connection failed with code " + connectionResult.getErrorCode());
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    UserLocationActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode,
        grantResults);
  }

  @NeedsPermission(android.Manifest.permission.CAMERA) void showCamera() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  @OnShowRationale(android.Manifest.permission.CAMERA) void showRationaleForCamera(
      final PermissionRequest request) {
    new AlertDialog.Builder(this).setMessage(R.string.perm_take_pics)
        .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.cancel();
          }
        })
        .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.proceed();
          }
        })
        .show();
  }

  @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE) void readStorage() {
    Intent galleryIntent =
        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
  }

  @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE) void showRationaleReadStorage(
      final PermissionRequest request) {
    new AlertDialog.Builder(this).setMessage(R.string.perm_get_pics)
        .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.cancel();
          }
        })
        .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.proceed();
          }
        })
        .show();
  }

  @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION) void getLocation() {
    mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this)
        // The next two lines tell the new client that “this” current class will handle connection stuff
        .addConnectionCallbacks(this).addOnConnectionFailedListener(this)
        //fourth line adds the LocationServices API endpoint from GooglePlayServices
        .addApi(LocationServices.API).build();

    // Create the LocationRequest object
    mLocationRequest = LocationRequest.create()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(TimeUnit.SECONDS.toMillis(1))
        .setFastestInterval(TimeUnit.SECONDS.toMillis(1));
  }

  @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION) void showRationaleGetLocation(
      final PermissionRequest request) {
    new AlertDialog.Builder(this).setMessage(R.string.perm_get_location)
        .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.cancel();
          }
        })
        .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            request.proceed();
          }
        })
        .show();
  }
}
