package pavel11.rectangledetection;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class tab2Class extends Fragment implements OnMapReadyCallback{
    private GoogleMap map;
    private MapView mapView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab2, container, false);

        mapView=(MapView)rootView.findViewById(R.id.map);

        mapView.onCreate(savedInstanceState);
        mapView.onResume();




        try{
            MapsInitializer.initialize(getActivity().getApplicationContext());
        }catch (Exception e){
            e.printStackTrace();
        }


        mapView.getMapAsync(this);

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map= googleMap;

//        SETTIAMO LA VISTA INIZIALE E ZOOM DELLA MAPPA
        LatLng cittadella= new LatLng(37.525705, 15.073056);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cittadella,14.0f));
//        LEGGO LE FOTO CHE GIA' ABBIAMO ACQUISITO
        aggiornaMarkers();

//        APRO L'ACTIVITY PER VEDERE I DETTAGLI DELLE FOTO
        map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String nameMarker=marker.getTitle();
                Intent openSeeRectActivity = new Intent(getContext(),SeeRectActivity.class);
                startActivity(openSeeRectActivity.putExtra("luogo",nameMarker));
            }
        });
    }

    public void aggiornaMarkers(){
        //        LEGGO LE FOTO CHE GIA' ABBIAMO ACQUISITO
        LatLng punto;
        Rettangolo tmp;
        for (int i=0; i<HomeActivity.listaRettangoli.size();i++){
            tmp=HomeActivity.listaRettangoli.get(i);
            punto=new LatLng(HomeActivity.DMStoDecimal(tmp.getLat()), HomeActivity.DMStoDecimal(tmp.getLng()));
            map.addMarker(new MarkerOptions().position(punto).title(tmp.getNomeFoto()));

        }
    }
    @Override
    public void onResume() {
        super.onResume();

    }
}
