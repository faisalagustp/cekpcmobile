package id.ac.ui.cs.inventarisfasilkom;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;

import com.google.zxing.common.StringUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HalamanDetail extends AppCompatActivity {

    private List<String> tambahan;
    private List<Spinner> spinnerTambahan;
    private SharedPreferences pref;
    private Context context;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detil_barcode);
        tambahan = new ArrayList<String>();
        spinnerTambahan = new ArrayList<Spinner>();

        toolbar = (Toolbar) findViewById(R.id.toolbar);


        pref = getApplicationContext().getSharedPreferences("login", 0);
        this.context = getApplicationContext();

        Intent myIntent = getIntent();
        TextView tv = (TextView) findViewById(R.id.barcode);
        tv.setText(myIntent.getStringExtra("barcodeNumber"));
        tv = (TextView) findViewById(R.id.nama_inventaris);
        tv.setText(myIntent.getStringExtra("namaInventaris") + "("+myIntent.getStringExtra("jenisInventaris")+")");
        tv = (TextView) findViewById(R.id.lokasi);
        tv.setText(myIntent.getStringExtra("lokasiInventaris"));

        Button laporkan = (Button) findViewById(R.id.laporkan);
        laporkan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] param = new String[3];
                TextView tv = (TextView) findViewById(R.id.barcode);
                if(pref.getInt("userId",0) != 0){
                    param[0] = Integer.toString(pref.getInt("userId",0));
                    param[1] = tv.getText().toString();
                    param[2] = "[";

                    String[] target = {"Keyboard","Mouse","Monitor","Windows"};
                    for(String jenis : target ) {
                        int resID = getResources().getIdentifier("spinner"+jenis, "id", getPackageName());
                        Spinner sp = (Spinner) findViewById(resID);
                        String isi = sp.getSelectedItem().toString();
                        param[2] += "{ \"jenis\" : \""+jenis+"\" ,";
                        param[2] += " \"skor\" : "+isi.charAt(0)+" ,";
                        param[2] += " \"deskripsi\" : \""+isi.substring(3)+"\" },";
                    }
                    //buat tambahan
                    int i = 0;
                    for (String tambah : tambahan){
                        Spinner sp = spinnerTambahan.get(i);
                        String isi = sp.getSelectedItem().toString();
                        param[2] += "{ \"jenis\" : \""+tambah+"\" ,";
                        param[2] += " \"skor\" : "+isi.charAt(0)+" ,";
                        param[2] += " \"deskripsi\" : \""+isi.substring(3)+"\" },";
                        i++;
                    }

                    param[2] = param[2].substring(0,param[2].length()-1);
                    param[2] += "]";



                    new HalamanDetailTask(context,view).execute(param);
                }
            }
        });


        Button tambah = (Button) findViewById(R.id.tambah);
        tambah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(HalamanDetail.this);
                dialog.setTitle("Buat Entri Tambahan");
                dialog.setMessage("Masukkan entri tambahan yang anda inginkan");
                final EditText input = new EditText (HalamanDetail.this);
                dialog.setView(input);
                dialog.setIcon(android.R.drawable.ic_input_add);

                dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            String textnya = input.getText().toString();
                            tambahan.add(textnya);
                            LinearLayout container = (LinearLayout) findViewById(R.id.Tambahable);

                            TextView txt1 = (TextView) findViewById(R.id.os);
                            TextView txt2 = new TextView(getApplicationContext());
                            txt2.setText(textnya);
                            txt2.setLayoutParams(txt1.getLayoutParams());
                            txt2.setTextColor(txt1.getTextColors());

                            Spinner sp1 = (Spinner) findViewById(R.id.spinnerWindows);
                            Spinner sp2 = new Spinner(getApplicationContext());
                            sp2.setLayoutParams(sp1.getLayoutParams());
                            sp2.setAdapter(sp1.getAdapter());
                            spinnerTambahan.add(sp2);

                            container.addView(txt2);
                            container.addView(sp2);
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                });
                dialog.show();
            }
        });
    }

    class HalamanDetailTask extends AsyncTask<String, Void, JSONObject> {

        private Exception exception;
        ProgressDialog loadingMessage;
        Context context;
        View view;

        public HalamanDetailTask (Context context, View view){
            this.context = context;
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            loadingMessage = new ProgressDialog(HalamanDetail.this);
            loadingMessage.setMessage("Mengirim Laporan...");
            loadingMessage.setIndeterminate(false);
            loadingMessage.setCancelable(false);
            loadingMessage.show();
        }

        protected JSONObject doInBackground(String... urls) {
            String userId = urls[0];
            String barcode = urls[1];
            String isiLaporan = urls[2];

            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("userId", userId)
                    .appendQueryParameter("barcode", barcode)
                    .appendQueryParameter("isiLaporan", isiLaporan);
            String query = builder.build().getEncodedQuery();

            try {
                JSONObject jo = NetworkHelper.doAPIConnection("https://cekpc.cs.ui.ac.id/kirim/laporan/", "", query, false);
                return jo;
            }catch (Exception e){
                Log.i("Hasil","Gagal Maning Son");
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(JSONObject hasil) {
            loadingMessage.dismiss();
            if(hasil == null){
                Snackbar.make(view, "Gagal Tersimpan. Coba lagi.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
            }else{
                try {
                    if(hasil.getBoolean("status")==false){
                        Snackbar.make(view, "Laporan anda telah tersimpan dalam sistem.", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
                        Thread timerThread = new Thread() {
                            public void run() {
                                try {
                                    sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } finally {
                                    Intent intent = new Intent(getApplicationContext(), HalamanUtama.class);
                                    finish();
                                    startActivity(intent);
                                }
                            }
                        };
                        timerThread.start();
                    }else{
                        Snackbar.make(view, "Laporan anda gagal tersimpan. " + hasil.getString("message"), Snackbar.LENGTH_LONG)
                                .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
                    }
                }catch (Exception e){
                    Snackbar.make(view, "Laporan anda gagal tersimpan. Balikan server tidak sah", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }


}
