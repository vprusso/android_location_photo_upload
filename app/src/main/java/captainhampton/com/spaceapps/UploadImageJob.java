package captainhampton.com.spaceapps;

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;

/**
 * Created by mms on 10/28/16.
 */

public class UploadImageJob extends Job {

  private static final String SERVER_ADDRESS = "http://vprusso-spaceapps-beta.site88.net/";

  private final double lat, lon; // uploaded in String(lat)_String(lon) format
  private final String uri;

  private UploadImageJob(Params params, Uri uri, double lat, double lon) {
    super(params);
    this.lat = lat;
    this.lon = lon;
    this.uri = uri.toString();
  }

  public static UploadImageJob newJob(Uri uri, double lat, double lon) {
    return new UploadImageJob(new Params(1).requireNetwork().persist(), uri, lat, lon);
  }

  @Override public void onAdded() {
  }

  @Override public void onRun() throws Throwable {
    Bitmap image = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(),
        Uri.parse(uri));

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
    String encodedImage =
        Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

    HashMap<String, String> dataToSend2 = new HashMap<>();
    dataToSend2.put("image", encodedImage);
    dataToSend2.put("name", Double.toString(lat).concat("_" + Double.toString(lon)));

    performPostCall(SERVER_ADDRESS + "SavePicture.php", dataToSend2);
  }

  // http://stackoverflow.com/questions/29536233/deprecated-http-classes-android-lollipop-5-1
  private String performPostCall(String requestURL, HashMap<String, String> postDataParams) {

    URL url;
    String response = "";
    try {
      url = new URL(requestURL);

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(15000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("POST");
      conn.setDoInput(true);
      conn.setDoOutput(true);

      OutputStream os = conn.getOutputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
      writer.write(getPostDataString(postDataParams));

      writer.flush();
      writer.close();
      os.close();
      int responseCode = conn.getResponseCode();

      if (responseCode == HttpsURLConnection.HTTP_OK) {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = br.readLine()) != null) {
          response += line;
        }
      } else {
        response = "";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return response;
  }

  private String getPostDataString(HashMap<String, String> params)
      throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (first) {
        first = false;
      } else {
        result.append("&");
      }

      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
    }

    return result.toString();
  }

  @Override protected void onCancel(int cancelReason, @Nullable Throwable throwable) {

  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount,
      int maxRunCount) {
    return null;
  }
}
