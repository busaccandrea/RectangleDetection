package pavel11.rectangledetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class SeeRectActivity extends AppCompatActivity {
    TextView fnome, fdata, fluogo, nomefoto, data, luogo;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_rect);
        String myfoto=getIntent().getStringExtra("luogo");
        inizializzaWidget();

//        Log.e("nome foto", getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath());
        setImageView(myfoto);

    }

    private void inizializzaWidget(){
        fnome= (TextView) findViewById(R.id.fissoNome);
        fdata= (TextView) findViewById(R.id.fissoData);
        fluogo= (TextView) findViewById(R.id.fissoLuogo);
        nomefoto= (TextView) findViewById(R.id.nomefoto);
        data= (TextView) findViewById(R.id.datafoto);
        luogo= (TextView) findViewById(R.id.luogofoto);
        imageView=(ImageView)findViewById(R.id.mostraFoto);
    }

    private void setImageView(String picture){
        File imgFile = new  File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath()+ "/"+picture);
        ExifInterface exif=null;
        try {
            exif=new ExifInterface(imgFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (exif!=null) {
            if(imgFile.exists()){

                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                imageView.setImageBitmap(myBitmap);
                nomefoto.setText(picture);
                String coordinate=String.valueOf(HomeActivity.DMStoDecimal(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)))+",\n"+
                        String.valueOf(HomeActivity.DMStoDecimal(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)));
                luogo.setText(coordinate);

                data.setText(exif.getAttribute(ExifInterface.TAG_DATETIME));
            }
        }
    }
}
