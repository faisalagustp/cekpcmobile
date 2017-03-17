package id.ac.ui.cs.inventarisfasilkom;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

public class HalamanUtama extends AppCompatActivity {

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_halaman_utama);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pref = getApplicationContext().getSharedPreferences("login", 0);

        if(pref.getInt("userId",0)==0){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            finish();
            startActivity(intent);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(HalamanUtama.this);
                integrator.setPrompt("Scan barcode inventaris yang dimaksud");
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.setBeepEnabled(false);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            }
        });

        Button tombol = (Button) findViewById(R.id.tombolCari);
        tombol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView tv = (TextView) findViewById(R.id.errorMessage);
                tv.setText("");
                EditText barcode = (EditText) findViewById(R.id.barcodeET);
                String[] input = new String[1];
                input[0] = barcode.getText().toString();
                new HalamanUtamaTask(getApplicationContext()).execute(input);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                TextView tv = (TextView) findViewById(R.id.errorMessage);
                tv.setText("Barcode tidak terbaca. Silakan scan kembali");
            } else {
                //cek di API, barcode ada atau nggak
                Context context = getApplicationContext();
                String[] input = new String[1];
                input[0] = result.getContents();
                EditText et = (EditText) findViewById(R.id.barcodeET);
                et.setText(result.getContents());
                TextView tv = (TextView) findViewById(R.id.errorMessage);
                tv.setText("");
                new HalamanUtamaTask(context).execute(input);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_halaman_utama, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            Editor editor = pref.edit();
            editor.clear();
            editor.commit();

            Intent intent = new Intent(getApplicationContext(),Login.class);
            finish();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    class HalamanUtamaTask extends AsyncTask<String, Void, JSONObject> {

        private Exception exception;
        ProgressDialog loadingMessage;
        Context context;

        public HalamanUtamaTask (Context context){
            //TODO Tambahkan interface untuk komunikasi dengan activity
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            loadingMessage = new ProgressDialog(HalamanUtama.this);
            loadingMessage.setMessage("Mengecek barcode...");
            loadingMessage.setIndeterminate(false);
            loadingMessage.setCancelable(false);
            loadingMessage.show();
        }

        protected JSONObject doInBackground(String... urls) {
            String barcode = urls[0];

            try {
                JSONObject jo = NetworkHelper.doAPIConnection("https://cekpc.cs.ui.ac.id/kirim/inventaris/"+barcode+"/", "", "", false);
                return jo;
            }catch (Exception e){
                Log.i("Hasil","Gagal Maning Son");
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(JSONObject hasil) {
            loadingMessage.dismiss();
            TextView tv = (TextView) findViewById(R.id.errorMessage);
            if(hasil == null){
                tv.setText("Barcode tidak dikenali oleh sistem.");
            }else{
                try {
                    if(hasil.getBoolean("status")==false){
                        Intent intent = new Intent(getApplicationContext(),HalamanDetail.class);
                        intent.putExtra("barcodeNumber",hasil.getString("barcode"));
                        intent.putExtra("namaInventaris",hasil.getString("nama"));
                        intent.putExtra("jenisInventaris",hasil.getString("jenis"));
                        intent.putExtra("lokasiInventaris",hasil.getString("lokasi"));
                        intent.putExtra("defaultLaporan",hasil.getJSONArray("defaultLaporan").toString());
                        intent.putExtra("pilihanLokasi",hasil.getJSONArray("pilihanLokasi").toString());
                        startActivity(intent);
                    }else{
                        tv.setText("Barcode tidak dikenali oleh sistem.");
                    }
                }catch (Exception e){
                    tv.setText("Barcode tidak dikenali oleh sistem.");
                    e.printStackTrace();
                }
            }
        }
    }
}
