package com.omeryagmur.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.omeryagmur.artbookjava.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {
    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database=openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent=getIntent();
        String info= intent.getStringExtra("info");

        if (info.equals("new")){
            binding.nametext.setText("");
            binding.artisttext.setText("");
            binding.yeartext.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.selectimage);

        }else {
            int artId=intent.getIntExtra("artId",0);
            binding.button.setVisibility(View.INVISIBLE);
            binding.nametext.setEnabled(false);
            binding.artisttext.setEnabled(false);
            binding.yeartext.setEnabled(false);

            try {

                Cursor cursor=database.rawQuery("SELECT * FROM arts WHERE id=?",new String[]{String.valueOf(artId) });

                int artNameIX=cursor.getColumnIndex("artname");
                int painterNameIX=cursor.getColumnIndex("paintername");
                int yearNameIX=cursor.getColumnIndex("year");
                int imageIX=cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    binding.nametext.setText(cursor.getString(artNameIX));
                    binding.artisttext.setText(cursor.getString(painterNameIX));
                    binding.yeartext.setText(cursor.getString(yearNameIX));

                    byte[] bytes= cursor.getBlob(imageIX);

                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void save(View view) {
        String name=binding.nametext.getText().toString();
        String artist=binding.artisttext.getText().toString();
        String year=binding.yeartext.getText().toString();

        Bitmap smallimage=makesmallerimage(selectedImage,300);

        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallimage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id Integer PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");

            String sqlstring="INSERT INTO arts(artname,paintername,year,image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlstring);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artist);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent=new Intent(this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makesmallerimage(Bitmap image,int maxsize){
        int width=image.getWidth();
        int height= image.getHeight();
        float BitmapRatio=(float) width/ (float) height;
        if(BitmapRatio>1){
            width=maxsize;
            height=(int) (width/BitmapRatio);
        }
        else {
            height=maxsize;
            width=(int) (height*BitmapRatio);
        }
        return image.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view) {

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                }else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }


            } else {
//gallery
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }else{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                }else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }


            } else {
//gallery
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }


    }
    private void registerLauncher(){
        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                  Intent IntentFromResult=  result.getData();
                  if(IntentFromResult.getData()!=null){
                    Uri Imagedata=IntentFromResult.getData();
                   // binding.imageView.setImageURI(Imagedata);
                      try {
                          if (Build.VERSION.SDK_INT >= 28) {
                              ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), Imagedata);
                              selectedImage = ImageDecoder.decodeBitmap(source);
                              binding.imageView.setImageBitmap(selectedImage);
                          }
                          else
                          {
                              selectedImage=MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),Imagedata);
                              binding.imageView.setImageBitmap(selectedImage);
                          }
                      }
                      catch(Exception e){
                          e.printStackTrace();
                      }



                      }
                }
            }
        });

        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result == true) {
                    Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                    //permission granted
                }
                else
                {
                    Toast.makeText(ArtActivity.this,"Permission denied",Toast.LENGTH_LONG).show();
                    //permission denied
                }
            }
        });
    }
}