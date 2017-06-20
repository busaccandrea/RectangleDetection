package pavel11.rectangledetection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import pavel11.rectangledetection.models.CameraData;
import pavel11.rectangledetection.models.MatData;
import pavel11.rectangledetection.utils.OpenCVHelper;
import pavel11.rectangledetection.views.CameraPreview;
import pavel11.rectangledetection.views.DrawView;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import static android.os.Build.TAGS;

public class RectDetectActivity extends AppCompatActivity {
    private static final String TAG = RectDetectActivity.class.getSimpleName();

    public static String current_date;
    String nomefoto;

    public static boolean scatta;



    static {
        if (!OpenCVLoader.initDebug()) {
            Log.v(TAG, "init OpenCV");
        }
    }

    private PublishSubject<CameraData> subject = PublishSubject.create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Se non voglio immagini multiple
        scatta = false;
        setContentView(R.layout.rect_detect_main);
        CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.camera_preview);

        cameraPreview.setCallback((data, camera) -> {
            CameraData cameraData = new CameraData();
            cameraData.data = data;
            cameraData.camera = camera;
            subject.onNext(cameraData);
        });
        cameraPreview.setOnClickListener(v -> cameraPreview.focus());
        DrawView drawView = (DrawView) findViewById(R.id.draw_layout);
        subject.concatMap(cameraData ->
                OpenCVHelper.getRgbMat(new MatData(), cameraData.data, cameraData.camera))
                .concatMap(matData -> OpenCVHelper.resize(matData, 400, 400))
                .map(matData -> {
                    matData.resizeRatio = (float) matData.oriMat.height() / matData.resizeMat.height();
                    matData.cameraRatio = (float) cameraPreview.getHeight() / matData.oriMat.height();
                    return matData;
                })
                .concatMap(this::detectRect)
                .compose(mainAsync())
                .subscribe(matData -> {
                    if (drawView != null ) {
                        if (matData.cameraPath != null && !scatta) {
                            drawView.setPath(matData.cameraPath);
//                          Qui dobbiamo scattare la foto!

//                            if (HomeActivity.location!=null) {
//
//
                                new TaskCercaRettangolo().execute(matData.oriMat);
                                Toast.makeText(getApplicationContext(), "Hotrovato un rettangolo." +
                                        "\nTorno alla Home", Toast.LENGTH_SHORT).show();
                                killActivity();
//                            }else if (HomeActivity.location==null){
//                                Log.e("sono nell'if", "location null");
//                                Toast.makeText(getApplicationContext(),
//                                        "Non sono ancora riuscito a trovare la posizione,\nnon sarò in grado di salvare le foto",
//                                        Toast.LENGTH_SHORT).show();
//                            }

                        } else {
                            drawView.setPath(null);
                        }
                        drawView.invalidate();

                    }
                });
    }

    private void killActivity(){
        finish();
    }
    private class TaskCercaRettangolo extends AsyncTask<Mat, Integer, Long> {
        protected Long doInBackground(Mat... urls) {

            Mat matrice = urls[0];
            salvaFile(matrice);


            return (long) 1;
        }



        protected void salvaFile(Mat mat) {
            try {
                scatta=true;
                current_date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(new Date());
                nomefoto = "RECT_" + current_date + ".jpg";
                Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mat, bitmap);
                File f = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), nomefoto);
                FileOutputStream stream = new FileOutputStream(f);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);

                writeExif(nomefoto);


                //  aggiungo un elemento alla lista di rettangoli
                HomeActivity.listaRettangoli.add(
                        new Rettangolo(nomefoto, current_date,
                                decimalLatToDegreesMinutesSeconds(HomeActivity.location.getLatitude())+
                                        latNSWE(HomeActivity.location.getLatitude()),
                                decimalLngToDegreesMinutesSeconds(HomeActivity.location.getLongitude())+
                                        latNSWE(HomeActivity.location.getLatitude())));

                try {
                    stream.flush();
                } finally {
                    stream.close();
                }
//                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            location=new Location("GPS");
//            listener.onLocationChanged(location);

//            Se non voglio immagini multiple

        }

        protected void writeExif(String nomeFoto){
            try {
                File dir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                HomeActivity.filelist = dir.listFiles();
                ExifInterface exif=null;
                for (int i=0; i<HomeActivity.filelist.length; i++){
                    if (HomeActivity.filelist[i].getPath().endsWith(nomeFoto)){
                        exif= new ExifInterface(HomeActivity.filelist[i].getPath());

                    }
                }

                if (exif!=null) {

//                    scrivo la data nell'exif

                    writeDate(exif);
//                    scrivo le coordinate nell'exif
                    writePosition(exif);

                    exif.saveAttributes();
//                    Log.e("EXIF",exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
                }else{
                    Log.e("exif è null",  "!");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void writeDate(ExifInterface exif){
            current_date= new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(new Date());
            exif.setAttribute(ExifInterface.TAG_DATETIME, current_date);
        }

        public String latNSWE(double lat){
            String retLat="";
            if (lat>0) return retLat+="E";
            else return retLat+="W";
        }
//        metodo per convertire nel formato deg/1,minuti'/1,secondi''/1 N, deg°/1,minuti'/1,secondi''/1 E
//        ATTENZIONE: PER UTILIZZARE LA NOTAZIONE DEGREE MINUTES SECONDS VA SPECIFICATA PRIMA LA LONGITUDINE E POI LA LATITUDINE!!
        public String decimalLatToDegreesMinutesSeconds(double lat){
            double degrees=Math.floor( lat);
            double minutes=Math.floor((60*(lat-degrees)));
            double seconds=Math.floor((3600*(lat-degrees)-60*minutes));
            return String.valueOf(degrees)+"/1,"+String.valueOf(minutes)+"/1,"+ String.valueOf(seconds)+"/1";
        }

        public String lngNSWE(double lng){
            if (lng>0)return "N";
            else return "S";
        }
        public String decimalLngToDegreesMinutesSeconds(double lng){
            double degrees= Math.floor(lng);
            double minutes=Math.floor(60*(lng-degrees));
            double seconds=(3600*(lng-degrees)-60*minutes);
            return String.valueOf(degrees)+"/1,"+String.valueOf(minutes)+"/1,"+ String.valueOf(seconds)+"/1,";
        }
        public void writePosition(ExifInterface exif){
            String lat=decimalLatToDegreesMinutesSeconds(HomeActivity.location.getLatitude());
            String lng=decimalLngToDegreesMinutesSeconds(HomeActivity.location.getLongitude());
//            Log.e("tag Location", lng+lngNSWE(HomeActivity.location.getLongitude()) +
//                            "\n"+lat+ latNSWE(HomeActivity.location.getLatitude()));
            try {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, lat);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latNSWE(HomeActivity.location.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lng);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lngNSWE(HomeActivity.location.getLongitude()));
            }catch (Exception e){
                Log.e("eccezione", "eccezione!");
            }
        }

        protected void onProgressUpdate(Integer... progress) {

        }

        protected void onPostExecute(Long result) {
            Log.i("task ","finish");
        }
    }
    private Observable<MatData> detectRect(MatData mataData) {
        return Observable.just(mataData)
                .concatMap(OpenCVHelper::getMonochromeMat)
                .concatMap(OpenCVHelper::getContoursMat)
                .concatMap(OpenCVHelper::getPath);
    }

    private static <T> Observable.Transformer<T, T> mainAsync() {
        return obs -> obs.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

}
