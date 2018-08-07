package com.example.maple.locationupdatefrequent.Activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.maple.locationupdatefrequent.GPSTracker;
import com.example.maple.locationupdatefrequent.GeoFencingDemo;
import com.example.maple.locationupdatefrequent.Helper.DBHelper;
import com.example.maple.locationupdatefrequent.Models.CenterDetails;
import com.example.maple.locationupdatefrequent.Models.Checkins;
import com.example.maple.locationupdatefrequent.Models.DailyReportState;
import com.example.maple.locationupdatefrequent.Models.GetCat;
import com.example.maple.locationupdatefrequent.Models.GetParams;
import com.example.maple.locationupdatefrequent.Models.QuestionsParams;
import com.example.maple.locationupdatefrequent.Models.UploadInstall;
import com.example.maple.locationupdatefrequent.R;
import com.example.maple.locationupdatefrequent.Validations;
import com.example.maple.locationupdatefrequent.rest.ApiClient;
import com.example.maple.locationupdatefrequent.rest.ApiInterface;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MultipartBody;

public class UploadQuestion extends Activity implements View.OnClickListener {
    ProgressDialog progress;
    File otherImagefile2_offline;
    String offlineimgpath2 = "";
    File otherImagefile2 = null;
    MultipartBody.Part imageFilePart2;
    Uri iv_url2;
    TextView clickimage;
    ImageView ivOtherImage2;
    int O_IMAGE2 = 2;
    GPSTracker gps;
    String latitude, longitude;
    ArrayList<CenterDetails> centerDetails;
    ArrayList<String> center;
    AppCompatSpinner category_spinner;
    ImageView back_image, done_img;
    TextView[] tvArray;
    EditText[] etArray;
    int len;
    String formattedMessage="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_question);
        center = new ArrayList<>();
        centerDetails = new ArrayList<CenterDetails>();
        category_spinner = findViewById(R.id.category_spinner);
        done_img = findViewById(R.id.done_img);
        back_image = findViewById(R.id.back_image);
        back_image.setOnClickListener(this);
        done_img.setOnClickListener(this);
        ivOtherImage2 = findViewById(R.id.ivOtherImage2);
        clickimage = findViewById(R.id.clickimage);
        // clickimage.setOnClickListener(this);
        ivOtherImage2.setOnClickListener(this);
        gps = new GPSTracker(this);
        if (!gps.isGPSEnabled && !gps.isNetworkEnabled) {
            Log.d("networkd", "false");
            gps.showSettingsAlert(UploadQuestion.this);
        } else {
            latitude = String.valueOf(gps.getLatitude());
            longitude = String.valueOf(gps.getLongitude());
            // Toast.makeText(getBaseContext(),latitude+" "+longitude  ,Toast.LENGTH_SHORT).show();
        }

        SharedPreferences s = getSharedPreferences("Userdetails", MODE_PRIVATE);
        if (Validations.hasActiveInternetConnection(UploadQuestion.this)) {
            GetCenterDetails("VVD@14", s.getString("DeviceId", ""));
        }else {

        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_image:
                finish();
                break;
            case R.id.done_img:

                for (int i = 0; i < len; i++) {
                    formattedMessage = formattedMessage + tvArray[i].getText().toString() + "<br/>Obs. " + etArray[i].getText().toString();
                    if (i != len - 1)
                        formattedMessage = formattedMessage + "<br/><br/>";
                }

                SharedPreferences s = getSharedPreferences("Userdetails", MODE_PRIVATE);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String millisInString = dateFormat.format(new Date());
                if (Validations.hasActiveInternetConnection(UploadQuestion.this)) {
                    updateInstall("VVD@14", s.getString("DeviceId", ""),
                            formattedMessage, latitude,longitude, s.getString("PersonName", ""),
                            millisInString, "1", s.getString("MobileDeviceID", ""), otherImagefile2.getAbsolutePath());

                }else {
                    DBHelper dbHelper = new DBHelper(UploadQuestion.this);
                    dbHelper.insertReport(latitude,longitude,formattedMessage.replace("'",""),millisInString,"offline",otherImagefile2.getAbsolutePath(),"local",
                            UploadQuestion.this);
                }

                break;
            case R.id.ivOtherImage2:
                String root = Environment.getExternalStorageDirectory().toString();
                File myDir = new File(root + "/RecceImages/");
                myDir.mkdirs();
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                otherImagefile2 = new File(myDir,
                        String.valueOf(System.currentTimeMillis()) + ".jpg");
                //  iv_url2 = Uri.fromFile(otherImagefile2);

                iv_url2 = FileProvider.getUriForFile(getApplicationContext(),
                        getApplication().getPackageName() + ".provider", otherImagefile2);


                intent.putExtra(MediaStore.EXTRA_OUTPUT, iv_url2);
                startActivityForResult(intent, O_IMAGE2);
                break;
        }
    }

    public void GetReportParams() {
        progress = new ProgressDialog(UploadQuestion.this);
        progress.setMessage("Getting Report Questions data from server..");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
        ApiInterface apiService = ApiClient.getSams().create(ApiInterface.class);
        SharedPreferences s = getSharedPreferences("Userdetails", MODE_PRIVATE);
        retrofit2.Call<GetParams> call = apiService.GetParamDetails("VVD@14", s.getString("CategoryID", ""), s.getString("DeviceId", ""),
                s.getString("MobileDeviceID", ""));
        call.enqueue(new retrofit2.Callback<GetParams>() {
            @Override
            public void onResponse(retrofit2.Call<GetParams> call, retrofit2.Response<GetParams> response) {
                Log.d("response fromserver" + response.isSuccessful(), String.valueOf(response.body().toString()));
                progress.dismiss();
                if (response.isSuccessful()) {
                    List<QuestionsParams> cd = response.body().getQuestionsParams();

                    tvArray = new TextView[cd.size()];
                    etArray = new EditText[cd.size()];
                    LinearLayout llMain = findViewById(R.id.my_ll);
                    llMain.setPadding(10, 10, 10, 10);
                    len = cd.size();
                    for (int i = 0; i < cd.size(); i++) {
                        //TextView tv = new TextView(this);
                        tvArray[i] = new TextView(UploadQuestion.this);
                        tvArray[i].setText(cd.get(i).getParameterDescription().toString());
                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        tvArray[i].setLayoutParams(p);
                        tvArray[i].setTypeface(tvArray[i].getTypeface(), Typeface.BOLD);
                        llMain.addView(tvArray[i]);
                        //EditText et = new EditText(this);
                        etArray[i] = new EditText(UploadQuestion.this);
                        etArray[i].setLayoutParams(p);
                        llMain.addView(etArray[i]);
                    }

                } else {

                }
            }

            @Override
            public void onFailure(retrofit2.Call<GetParams> call, Throwable t) {
                progress.dismiss();
                Log.d("response error", t.toString());
            }
        });


    }

    public void GetCenterDetails(String Token, String DeviceID) {
        center.add("-Select Category-");
        progress = new ProgressDialog(UploadQuestion.this);
        progress.setMessage("Fetching Category From Server..");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
        System.out.println("Token = " + Token + "DeviceId  = " + DeviceID);
        ApiInterface apiService = ApiClient.getSams().create(ApiInterface.class);

        retrofit2.Call<GetCat> call = apiService.GetCenterDetails(Token, DeviceID);
        call.enqueue(new retrofit2.Callback<GetCat>() {
            @Override
            public void onResponse(retrofit2.Call<GetCat> call, retrofit2.Response<GetCat> response) {
                progress.dismiss();
                Log.d("response fromserver" + response.isSuccessful(), String.valueOf(response.body().toString()));
                if (response.isSuccessful()) {

                    List<CenterDetails> cd = response.body().getCenters();
                    System.out.println("sizeof cener" + cd.size());
                    for (int i = 0; i < cd.size(); i++) {
                        System.out.println("centerno " + cd.get(i).getCenterNumber());
                        // centerDetails.add(new CenterDetails(  cd.get(i).getCenterNumber(),  cd.get(i).getCenterid()));
                        center.add(cd.get(i).getCenterNumber().toString());
                    }

                } else {

                    //  finish();
                }
                setadptertolist();
            }

            @Override
            public void onFailure(retrofit2.Call<GetCat> call, Throwable t) {
                Log.d("response error", t.toString());
                progress.dismiss();
                setadptertolist();
            }
        });


    }

    public void setadptertolist() {

        ArrayAdapter<String> plot_number = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, center);
        category_spinner.setAdapter(plot_number);
        GetReportParams();
    }

    public void updateInstall(String Token,
                              String DeviceID, final String MessageDescription,
                              final String Long, final String Lat,
                              String ReportedFrom, final String ReportedDateTime,
                              String DR, String MobileDeviceID,
                              final String imagepath) {
        progress = new ProgressDialog(UploadQuestion.this);
        progress.setMessage("Uploading Q & A To Server..");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
        System.out.println("Token = " + Token + "DeviceId  = " + DeviceID + " Message Des = " + MessageDescription + " Lat = " + Lat + " Long = " + Long + " Reported Time = "
                + ReportedDateTime + " ReportFrom = " + ReportedFrom + " DR = " + DR + " MobileDeviceId = " + MobileDeviceID + " IMageBitmap " + getStringImage(imagepath));
        ApiInterface apiService = ApiClient.getSams().create(ApiInterface.class);

        retrofit2.Call<UploadInstall> call = apiService.sendMessage(Token, DeviceID, MessageDescription, Long, Lat, ReportedFrom, ReportedDateTime, DR, MobileDeviceID,
                getStringImage(imagepath));
        call.enqueue(new retrofit2.Callback<UploadInstall>() {
            @Override
            public void onResponse(retrofit2.Call<UploadInstall> call, retrofit2.Response<UploadInstall> response) {
                progress.dismiss();
                Log.d("response fromserver" + response.isSuccessful(), String.valueOf(response.body().getMessage_data()));
                if (response.isSuccessful()) {

                    List<DailyReportState> cd = response.body().getMessage_data();
                    if (cd.get(0).getResponse().equals("Fail")) {
                        System.out.println("Failureeeeeeeee");
                        showDialog(UploadQuestion.this,"Unable sent daily report ,internal error occured please contact admin (or ) please try again","no");
                    } else {
                        showDialog(UploadQuestion.this,"Successfully sent daily report thankyou","yes");
                        System.out.println("sucessssssssssssss");
                    }
                 } else {
                    DBHelper dbHelper = new DBHelper(UploadQuestion.this);
                    dbHelper.insertReport(Lat,Long,MessageDescription.replace("'",""),ReportedDateTime,"offline",imagepath,"local",
                            UploadQuestion.this);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<UploadInstall> call, Throwable t) {
                progress.dismiss();
                Log.d("response error", t.toString());
                DBHelper dbHelper = new DBHelper(UploadQuestion.this);
                dbHelper.insertReport(Lat,Long,MessageDescription.replace("'",""),ReportedDateTime,"offline",imagepath,"local",
                        UploadQuestion.this);
            }
        });
    }

    public void showDialog(Activity activity, String msg, final String status) {
        final Dialog dialog = new Dialog(activity, R.style.PauseDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.custom_dialog);

        TextView text = dialog.findViewById(R.id.text_dialog);
        text.setText(msg);

        ImageView b = dialog.findViewById(R.id.b);
        if (status.equals("yes")) {
            b.setVisibility(View.VISIBLE);
        } else {
            b.setVisibility(View.GONE);
        }
        Button dialogButton = dialog.findViewById(R.id.btn_dialog);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                Intent upload= new Intent(UploadQuestion.this,UploadQuestion.class);
                startActivity(upload);
                finish();
            }
        });
        dialog.show();

    }

    private String getStringImage(String path) {
        String encodedImage = null;
        try {

            if (path != null) {
                Bitmap mBitmap = BitmapFactory.decodeFile(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos); //bm is the bitmap object
                byte[] byteArrayImage = baos.toByteArray();
                encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
            }
        } catch (Exception e) {
            Log.d("unable to read image", e.toString());
//            Toast.makeText(MainActivity1.this,"Retake picture",Toast.LENGTH_SHORT).show();
        }
        return encodedImage;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 8;
            opt.inMutable = true;
            Bitmap bmImage = BitmapFactory.decodeFile(otherImagefile2.getPath().toString(), opt);
            ivOtherImage2.setScaleType(ImageView.ScaleType.FIT_XY);
            ivOtherImage2.setImageBitmap(bmImage);
            compressImage(otherImagefile2.getAbsolutePath().toString());
        } catch (Exception e) {
            Log.e("msg", e.getMessage());
        }

    }

    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public String compressImage(String imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        // String filename = getFilename();
        try {
            out = new FileOutputStream(imageUri);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 25, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return imageUri;

    }

}
