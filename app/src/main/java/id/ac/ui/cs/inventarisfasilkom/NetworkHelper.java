package id.ac.ui.cs.inventarisfasilkom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;


/**
 * Created by Faisal Agus Tri Putra on 2017-03-07.
 */
public class NetworkHelper{

    public boolean isConnected(Context context, int networkType) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(networkType);
        return networkInfo != null && networkInfo.isConnected();
    }

    public boolean isConnectedWifi(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    public boolean isConnectedGprs(Context context) {
        return isConnected(context, ConnectivityManager.TYPE_MOBILE);
    }
    /**
     * Method ini digunakan untuk koneksi API dengan metode GET
     * @param url Alamat API
     * @return JSONObject yang merupakan objek balasan dari API
     * @throws Exception jika terjadi kesalahan sambungan
     */
    public static JSONObject doAPIConnection(String url, String token) throws Exception {
        return createConnection("GET", url, token, null);
    }

    /**
     * Method ini digunakan untuk koneksi API dengan metode POST
     * @param url Alamat API
     * @param dataToSend Data yang akan dikirimkan
     * @param isUpdate Jika proses yang dilakukan adalah update, nilainya true. Jika upload biasa, nilainya false
     * @return JSONObject yang merupakan objek balasan dari API
     * @throws Exception jika terjadi kesalahan sambungan
     */
    public static JSONObject doAPIConnection(String url, String token, String dataToSend, boolean isUpdate) throws Exception {
        String method = "POST";
        if(isUpdate){
            method = "PUT";
        }
        return createConnection(method,url,token,dataToSend);
    }

    /**
     * Method internal untuk melakukan koneksi dengan API
     * @param method Metode koneksi (GET atau POST)
     * @param url Alamat API
     * @param dataToSend Data yang akan dikirimkan (set null jika metodenya GET)
     * @return JSONObject yang merupakan objek balasan dari API
     * @throws Exception jika terjadi kesalahan sambungan
     */
    private static JSONObject createConnection(String method, String url, String token, String dataToSend) throws Exception {
        URL urlObject;
        HttpsURLConnection connection;

        Log.i("CALL", method + " " + url);
        urlObject = new URL(url);
        connection = (HttpsURLConnection) urlObject.openConnection();

        if(method.equals("POST")) {
            connection.setDoOutput(true);
        } else if(method.equals("GET")){
            connection.setDoOutput(false);
        }
        connection.setDoInput(true);
        if(!token.equals("")) connection.setRequestProperty("Authorization","Bearer "+token); //TODO tambah authorization di header_drawer untuk masukin token
        connection.setRequestMethod(method);
        connection.setUseCaches(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.connect();

        if(method.equals("POST") || method.equals("PUT")){
            OutputStream os = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(dataToSend);
            writer.flush();
            writer.close();
            os.close();
        }

        if (connection.getResponseCode() != 200) {
            Log.i("Error", connection.getResponseCode()+"");
            return null;
        }

        BufferedReader buff = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String jsonResult = buff.readLine();
        Log.i("JSON RESULT", jsonResult);
        connection.disconnect();
        return new JSONObject(jsonResult);
    }
}
