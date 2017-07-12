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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import org.json.JSONObject;

public class Login extends AppCompatActivity {

    private Context context;
    private EditText etUsername;
    private EditText etPassword;
    private SharedPreferences pref;
    Validation val = new Validation();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(id.ac.ui.cs.lapisi.R.layout.login);
        context = getApplicationContext();
        etUsername = (EditText) findViewById(id.ac.ui.cs.lapisi.R.id.username);
        // TextWatcher would let us check validation error on the fly

        etUsername.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                val.hasText(etUsername);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

//        etUsername.setSingleLine(true);

        etPassword = (EditText) findViewById(id.ac.ui.cs.lapisi.R.id.password);
        etPassword.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                val.hasText(etPassword);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        final Button signin = (Button) findViewById(id.ac.ui.cs.lapisi.R.id.signin);

        etPassword.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == 66) {
                    if (checkValidation()) {
                        signin.performClick();
                    }

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
                if (checkValidation()) {
                    String[] input = new String[2];
                    input[0] = etUsername.getText().toString();
                    input[1] = etPassword.getText().toString();
                    new LoginTask(context,view).execute(input);
                }

            }
        });
    }

    private boolean checkValidation()
    {
        boolean ret = true;

        if (!val.hasText(etUsername)) ret = false;
        if (!val.hasText(etPassword)) ret = false;

        return ret;
    }

    class Validation {

        //    // Error Messages
        private static final String REQUIRED_MSG = "required";
//
//
//    // return true if the input field is valid, based on the parameter passed
//    public static boolean isValid(EditText editText, String regex, String errMsg, boolean required) {
//
//        String text = editText.getText().toString().trim();
//        // clearing the error, if it was previously set by some other values
//        editText.setError(null);
//
//        // text required and editText is blank, so return false
//        if (required && !hasText(editText) ) return false;
//
//        return true;
//    }

        // check the input field has any text or not
        // return true if it contains text otherwise false
        public boolean hasText(EditText editText) {

            String text = editText.getText().toString().trim();
            editText.setError(null);

            // length 0 means there is no text
            if (text.length() == 0) {
                editText.setError(REQUIRED_MSG);
                return false;
            }

            return true;
        }
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
                        int userId = hasil.getInt("userId");
                        String username = hasil.getString("username");
                        String fullname = hasil.getString("fullname");
                        String hakAkses = hasil.getString("hakAkses");
//                        Snackbar.make(view, "Selamat datang "+fullname, Snackbar.LENGTH_LONG)
//                                .setAction("Action", null).setDuration(Snackbar.LENGTH_LONG).show();

                        SharedPreferences pref = getApplicationContext().getSharedPreferences("login", 0); // 0 - for private mode
                        Editor editor = pref.edit();
                        editor.putInt("userId",userId);
                        editor.putString("hakAkses",hakAkses);
                        editor.putString("username",username);
                        editor.putString("fullname",fullname);
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

