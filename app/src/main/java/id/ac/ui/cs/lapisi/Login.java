package id.ac.ui.cs.lapisi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Button;

import org.json.JSONObject;

public class Login extends AppCompatActivity {

    private Context context;
    private TextView tvUsername;
    private TextView tvPassword;
    private SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(id.ac.ui.cs.lapisi.R.layout.login);
        context = getApplicationContext();
        tvUsername = (TextView) findViewById(id.ac.ui.cs.lapisi.R.id.username);
        tvPassword = (TextView) findViewById(id.ac.ui.cs.lapisi.R.id.password);
        tvUsername.setSingleLine(true);
        final Button signin = (Button) findViewById(id.ac.ui.cs.lapisi.R.id.signin);

        tvPassword.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == 66) {
                    signin.performClick();
                }
                return false;
            }
        });

        pref = getApplicationContext().getSharedPreferences("login", 0);

        if(pref.getInt("userId",0)!=0){
            Intent intent = new Intent(getApplicationContext(), HalamanUtama.class);
            finish();
            startActivity(intent);
        }

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] input = new String[2];
                input[0] = tvUsername.getText().toString();
                input[1] = tvPassword.getText().toString();
                new LoginTask(context,view).execute(input);
            }
        });
    }

    class LoginTask extends AsyncTask<String, Void, JSONObject> {

        private Exception exception;
        ProgressDialog loadingMessage;
        Context context;
        View view;

        public LoginTask (Context context, View view){
            //TODO Tambahkan interface untuk komunikasi dengan activity
            this.context = context;
            this.view = view;
        }

        @Override
        protected void onPreExecute() {
            loadingMessage = new ProgressDialog(Login.this);
            loadingMessage.setMessage("Mengecek username dan password...");
            loadingMessage.setIndeterminate(false);
            loadingMessage.setCancelable(false);
            loadingMessage.show();
        }

        protected JSONObject doInBackground(String... urls) {
            String username = urls[0];
            String password = urls[1];
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("username", username)
                    .appendQueryParameter("password", password);
            String query = builder.build().getEncodedQuery();

            try {
                JSONObject jo = NetworkHelper.doAPIConnection("https://cekpc.cs.ui.ac.id/kirim/auth/", "", query, false);
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
                Snackbar.make(view, "Gagal Login. Username/Password tidak sah.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
            }else{
                try {
                    if (hasil.getBoolean("status") == true) {
                        Snackbar.make(view, "Gagal Login. Username/Password salah.", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
                    }else{
                        String username = hasil.getString("username");
                        String fullname = hasil.getString("fullname");
                        Snackbar.make(view, "Selamat datang "+fullname, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();

                        SharedPreferences pref = getApplicationContext().getSharedPreferences("login", 0); // 0 - for private mode
                        Editor editor = pref.edit();
                        editor.putInt("userId",hasil.getInt("userId"));
                        editor.putString("hakAkses",hasil.getString("hakAkses"));
                        editor.putString("username",hasil.getString("username"));
                        editor.putString("fullname",hasil.getString("fullname"));
                        editor.apply();
                        editor.commit();

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
                    }
                }catch (Exception e){
                    Snackbar.make(view, "Terdapat kesalahan server.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }

}