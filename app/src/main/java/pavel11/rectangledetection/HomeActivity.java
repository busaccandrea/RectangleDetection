package pavel11.rectangledetection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class HomeActivity extends AppCompatActivity {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;

//    Lista usata per salvare le foto e le sue varie info
    protected static ArrayList<Rettangolo> listaRettangoli;

    protected static File[] filelist;
//    Variabili per la geolocalizzazione
    private LocationManager locationManager;
    private LocationListener listener;
    protected static Location location=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

//      collego tutte le variabili al file xml dell'activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        //        inizializzo locationmanager e locationlistener
        inizializzaLocalizzazione();

//        richiedo la posizione gps
        location=richiediPosizione();
//        leggo i file dela cartella
        listaRettangoli = new ArrayList<Rettangolo>();
        leggiCartella();
        stampaListaRettangoli();
        FloatingActionButton new_photo = (FloatingActionButton) findViewById(R.id.new_photo);
//      chiamo l'activity per rilevare un nuovo rettangolo
        new_photo.setOnClickListener(view -> {
            Intent openRectDetect = new Intent(HomeActivity.this,RectDetectActivity.class);
            if (location!=null){
                Log.e("posizione", String.valueOf(location.getLatitude())+String.valueOf( location.getLongitude()));
                startActivity(openRectDetect);
            }
            else
                Toast.makeText(this, "non sono ancora riuscito a trovare la posizione.\n", Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        location=null;
    }

    private void inizializzaLocalizzazione() {
        locationManager= (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);


        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                Log.e("tagtagtag",
//                        "long:"+ String.valueOf(location.getLongitude()) + "lat: " +String.valueOf( location.getLatitude()));

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
//
//                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                startActivity(i);
            }
        };



    }

    protected Location richiediPosizione() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.e("permessi", "non ho i permessi");
            return null;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
        Location mLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        return mLocation;
    }
    protected String dateFromExif(String nomeFoto){
        try {
            for (int i=0; i<filelist.length; i++){
                if (filelist[i].getName().equals(nomeFoto)){
                    ExifInterface exif = new ExifInterface(filelist[i].getPath());
                    Log.e("file ", filelist[i].getPath());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        return exif.getAttribute(ExifInterface.TAG_DATETIME);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected String latFromExif(String nomeFoto){
        try {
            for (int i=0; i<filelist.length; i++){
                if (filelist[i].getPath().equals(nomeFoto)){
                    ExifInterface exif = new ExifInterface(filelist[i].getPath());
                    return exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)+
                            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    protected String lngFromExif(String nomeFoto){
        try {
            for (int i=0; i<filelist.length; i++){
                if (filelist[i].getPath().equals(nomeFoto)){
                    ExifInterface exif = new ExifInterface(filelist[i].getPath());
                    return exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) +
                            exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

//    protected Rettangolo rectFromList(String nomeFoto){
//        Rettangolo rect=null;
//        for (int i=0; i< HomeActivity.listaRettangoli.size(); i++){
//            if (HomeActivity.listaRettangoli.get(i).getNomeFoto().equals(nomeFoto)){
//                rect=HomeActivity.listaRettangoli.get(i);
//            }
//
//        }
//        return rect;
//    }

    public  void leggiCartella(){
        File dir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        filelist = dir.listFiles();
        if (filelist!=null) {

            for (int i = 0; i < filelist.length; i++) {
                if (filelist[i].getName().startsWith("RECT")){
                    HomeActivity.listaRettangoli.add(
                            new Rettangolo(
                                    filelist[i].getName(),
                                    dateFromExif(filelist[i].getPath()),
                                    latFromExif(filelist[i].getPath()),
                                    lngFromExif(filelist[i].getPath())));
                }
            }
        }else Toast.makeText(getApplicationContext(), "Cartella Vuota!", Toast.LENGTH_SHORT).show();
    }

    public void stampaListaRettangoli(){
        for (int i=0; i<listaRettangoli.size(); i++){
            Rettangolo tmp=listaRettangoli.get(i);
            Log.e("Stamp","nome foto "+tmp.getNomeFoto()+
            "\n data "+ tmp.getDate()+
            "\n coordinate "+ String.valueOf(DMStoDecimal(tmp.getLat())
                    +"," +DMStoDecimal(tmp.getLng())));
        }
    }
    //        15/1,4/1,23/1N37/1,31/1,32/1E to decimal
//    ATTENZIONE: PER USARE LA NOTAZIONE DECIMALE VA SPECIFICATA PRIMA LA LATITUDINE E POI LA LONGITUDINE
    public static double DMStoDecimal(String coordinate){
        String sdeg="";
        String smin="";
        String ssec="";
        int c=0;
        for (int i=0; i<coordinate.length();i++){
            if (coordinate.charAt(i)=='/'){
                i+=2;
                c++;
            }else{
                switch (c){
                    case 0: sdeg+=coordinate.charAt(i);
                        break;
                    case 1: smin+=coordinate.charAt(i);
                        break;
                    case 2: ssec+=coordinate.charAt(i);
                        break;
                    default: break;
                }
            }
        }
        Log.e("decimal func", "input "+coordinate+
                "\ndeg "+sdeg+ "\tlmin "+smin+"\tsec "+ssec);
//        Decimal deg=deg+minutes/60+seconds/3600
        double decDeg=Double.parseDouble(sdeg)+
                (Double.parseDouble(smin))/60+
                (Double.parseDouble(ssec)/3600);
        return decDeg;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0:
                    tab1Class tab1=new tab1Class();
                    return tab1;
                case 1:
                    tab2Class tab2 = new tab2Class();
                    return tab2;
                default:
                    return null;
            }

        }

        @Override
        public int getCount() {
            // Mostra x pagine
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "List";
                case 1:
                    return "Map";
            }
            return null;
        }
    }



}
