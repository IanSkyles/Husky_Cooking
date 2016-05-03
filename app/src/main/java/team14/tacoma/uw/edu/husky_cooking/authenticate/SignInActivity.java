/*
 * Mike Ford and Ian Skyles
 * TCSS450 – Spring 2016
 * Recipe Project
 */
package team14.tacoma.uw.edu.husky_cooking.authenticate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import team14.tacoma.uw.edu.husky_cooking.R;
import team14.tacoma.uw.edu.husky_cooking.RecipeActivity;
import team14.tacoma.uw.edu.husky_cooking.RegisterUserFragment;


/**
 * This class controls the user signing into our application.
 * It interfaces with our database hosted on CSSGATE.
 *
 * @author Mike Ford
 * @author Ian Skyles
 * @version 5/2/2016
 */
public class SignInActivity extends AppCompatActivity
        implements LogInFragment.LoginInteractionListener
        , RegisterUserFragment.UserAddListener{

    /** CSSGATE login url */
    private static final String LOGIN_URL =
            "http://cssgate.insttech.washington.edu/~_450atm14/husky_cooking/login.php?";
    /** CSSGATE add user url */
    private static final String USER_ADD_URL =
            "http://cssgate.insttech.washington.edu/~_450atm14/husky_cooking/addUser.php?";
    /** A tag for debugging */
    private static final String TAG = "SignInActivity";

    /** Interface for accessing and modifying preference data */
    private SharedPreferences  mSharedPreferences;
    /** These EditTexts allow the user to enter Username,
     * Password, RegisterPassword. and Email.*/
    private EditText mUserName, mPwd, mRegisterEmail, mRegisterPassword;

    /** Button to login*/
    private Button mLoginButton;
    /** Button to register*/
    private Button mRegisterButton;



    /**
     * Saves instance on creation of method of fragment/app.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_sign_in);

        mRegisterEmail = (EditText) findViewById(R.id.new_user_email);
        mRegisterPassword = (EditText) findViewById(R.id.new_user_password);
        mUserName = (EditText) findViewById(R.id.user_id);
        mPwd = (EditText) findViewById(R.id.pwd);
        mLoginButton = (Button) findViewById(R.id.signin_button);



        mSharedPreferences = getSharedPreferences(getString(R.string.LOGIN_PREFS)
                , Context.MODE_PRIVATE);
        if(!mSharedPreferences.getBoolean(getString(R.string.LOGGEDIN), false)) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new LogInFragment())
                    .commit();
            mRegisterButton = (Button) findViewById(R.id.sign_up_button);


        }else{
            Intent i = new Intent(this, RecipeActivity.class);
            startActivity(i);
            finish();

        }





    }



    public void signup(String url){



    }


    /**
     * This method ensures network connectivity and
     * checks if login and password are valid.
     * User will be logged in if valid.
     * @param url - the place to log in at on the cssgate server
     */
    public void login(String url, String user){


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            //Check if the login and password are valid

//            new LoginTask().execute(url);
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                        openFileOutput(getString(R.string.LOGIN_FILE)
                                , Context.MODE_PRIVATE));
                outputStreamWriter.write(url);
                outputStreamWriter.close();
//                Toast.makeText(this,"Stored in File Successfully!", Toast.LENGTH_LONG)
//                        .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(this, "No network connection available. Cannot authenticate user",
                    Toast.LENGTH_SHORT) .show();
            return;
        }



        LoginTask task = new LoginTask();
        String result = null;
        try {
            result = task.execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        if(result.contains("success")){
            mSharedPreferences
                    .edit()
                    .putBoolean(getString(R.string.LOGGEDIN), true)
                    .commit();
            mSharedPreferences
                    .edit()
                    .putString(getString(R.string.LOGGED_USER), user)
                    .commit();
            Intent i  = new Intent(this, RecipeActivity.class);
            startActivity(i);
            finish();
        }else{
            Toast.makeText(getApplicationContext(), "Log in failed!", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Used to add a user DB.
     * @param url
     */
    public void addUser(String url){
        AddUserTask task = new AddUserTask();
        task.execute(new String[]{url.toString()});
        getSupportFragmentManager().popBackStackImmediate();
    }


    /**
     * Adds the user to our database asynchronously.
     */
    private class AddUserTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls){
            String response ="";
            HttpURLConnection urlConnection=null;
            for(String url: urls){
                try{
                    URL urlObject = new URL(url);
                    urlConnection = (HttpURLConnection) urlObject.openConnection();

                    InputStream content = urlConnection.getInputStream();

                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s = "";
                    while((s = buffer.readLine())!= null){
                        response += s;
                    }
                } catch (Exception e) {
                    response = "Unable to add user, Reason: "
                            + e.getMessage();
                } finally {
                    if(urlConnection != null){
                        urlConnection.disconnect();
                    }
                }
            }
            return response;
        }

        /**
         * It checks to see if there was a problem with the URL(Network) which is when an
         * exception is caught. It tries to call the parse Method and checks to see if it was successful.
         * If not, it displays the exception.
         *
         * @param result
         */
        @Override
        protected void onPostExecute(String result){
            try {
                JSONObject jsonObject = new JSONObject(result);
                String status = (String) jsonObject.get("result");
                if(status.equals("success")){
                    Toast.makeText(getApplicationContext(), "User successfully added!\n" +
                            "Feel free to LOG IN!",
                            Toast.LENGTH_LONG)
                            .show();

                }else {
                    Toast.makeText(getApplicationContext(), "Failed to add: "
                                    + jsonObject.get("error") + "\nPlease enter a different e-mail adress"
                            ,Toast.LENGTH_LONG)
                            .show();
                }
            }catch (JSONException e){
                Toast.makeText(getApplicationContext(), "Something wrong with the data" +
                        e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

    }


    /**
     * Logs our user into the database asynchronously.
     */
    private class LoginTask extends AsyncTask<String, Void, String> {
        boolean failure= false;

        @Override
        protected void onPreExecute(){
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... urls){
            String response = "";
            HttpURLConnection urlConnection = null;
            for(String url:urls){
                try {
                    URL urlObject = new URL(url);
                    urlConnection = (HttpURLConnection) urlObject.openConnection();
                    InputStream content = urlConnection.getInputStream();
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                    String s ="";
                    while((s=buffer.readLine())!=null){
                        response +=s;
                    }
                }catch (Exception e){
                    response = "Unable to login, Reason: " + e.getMessage();
                }finally{
                    if(urlConnection !=null){
                        urlConnection.disconnect();
                    }
                }
            }

            return response;
        }


        @Override
        protected void onPostExecute(String result) {
            if(result.startsWith("Unable to")){
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG)
                        .show();
                return;
            }

            if(result!=null){
                Log.e("SignInActivity", result.toString());
            }

            if(User.parseUserJSON(result)){

//                startActivity(new Intent(this, RecipeActivity.class));
            }
        }
    }

}
