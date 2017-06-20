package pavel11.rectangledetection;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Rettangolo {
    private String nomeFoto;
    private String lat;
    private String lng;
    private String date;


    public Rettangolo(String nomeFoto, String date, String lat, String lng ) {
        this.nomeFoto=nomeFoto;
        this.date=date;
        this.lat=lat;
        this.lng=lng;
    }
    public String getNomeFoto(){
        return nomeFoto;
    }
    public String getLat(){
        return lat ;
    }
    public String getLng(){
        return lng;
    }
    public String getDate() {
        return date;
    }


}