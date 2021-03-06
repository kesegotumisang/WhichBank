package bw.co.whichbank.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import bw.co.whichbank.R;
import bw.co.whichbank.util.CustomDialog;
import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.Manifest.permission.READ_CONTACTS;

//import co.whatcreations.nobox.utils.CustomDialog;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A gigs authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private TextView forgotPasswordTextView;
    private TextView createAccountTextView;
    private EditText userNameEditText;
    private Button mEmailSignInButton;
    private boolean signUp = false;
    private TextInputLayout userName;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        preferences = getSharedPreferences("WhichBank",MODE_PRIVATE);

        userName = (TextInputLayout)findViewById(R.id.userName);
        userNameEditText = (EditText) findViewById(R.id.userNameEditText);
        userName.animate().translationXBy(-1000).setDuration(0).start();
        mPasswordView = (EditText) findViewById(R.id.password);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        //Check to see which state and set the keyboard ime label accordingly
        mPasswordView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    //set keyBoard options when focus on password field
                    if (signUp) {
                        mPasswordView.setImeActionLabel("Sign Up", EditorInfo.IME_ACTION_DONE);
                    } else {
                        mPasswordView.setImeActionLabel("Login", EditorInfo.IME_ACTION_DONE);
                    }
                }
            }
        });

        //Check to see which state and sign up or login into the app
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if(signUp){
                    if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                        launchFirstIntent();
                        return true;
                    }
                }else {
                    if (id == R.id.login || id == EditorInfo.IME_ACTION_DONE) {
                        attemptLogin();
                        return true;
                    }
                }

                return false;

            }
        });

        /**
         *Launch Dialog to prompt the user to enter their email and reset the password
         */
//        View dialogView = ;
        forgotPasswordTextView = (TextView)findViewById(R.id.forgotPasswordTextView);
        forgotPasswordTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {

                resetPasswordDialog();

            }
        });

        createAccountTextView = (TextView)findViewById(R.id.createAccountTextView);

        createAccountTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //make the text unclickable while playing the animation
                createAccountTextView.setClickable(false);
                //if not in sign up state start it up
                if(!signUp) {
                    //Attempt to create userAccount
                    setUpSignUp();
                    createAccountTextView.setText(R.string.have_an_account);
                    signUp = true;
                }
                //start up sign in state
                else{
                    createAccountTextView.setClickable(false);
                    setUpSignIn();
                    createAccountTextView.setText(R.string.sign_up);
                    signUp=false;
                }
            }
        });

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            mEmailSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    //if not in sign up state attempt to login
                    if(!signUp) {
                        attemptLogin();
                    }
                    //if not in sign in state attempt sign up
                    else{
//                    attemptSignUp();
                        //Start up Intent
                        launchFirstIntent();
                    }
                }
            });
        }


        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void resetPasswordDialog(){

        final LayoutInflater inflater = this.getLayoutInflater();
        final CustomDialog alertDialog = new CustomDialog(LoginActivity.this, CustomDialog.WARNING_TYPE);
        alertDialog.show();
        alertDialog.input.setVisibility(View.VISIBLE);
        alertDialog.setTitleText("Confirm Password Reset?")
                .setConfirmText("Accept")
                .setCancelText("Cancel")
                .setCanceledOnTouchOutside(true);

        alertDialog.setConfirmClickListener(new CustomDialog.OnSweetClickListener() {
            @Override
            public void onClick(CustomDialog sDialog) {
                String input = alertDialog.input.getText().toString();
                if(input.contains("@")) {
                    alertDialog.input.setVisibility(View.GONE);
                    sDialog
                            .setTitleText("Sent!")
                            .setContentText("Go to your email " + alertDialog.input.getText().toString() + " to reset your password.")
                            .setConfirmText("OK")
                            .setConfirmClickListener(null)
                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                }else {
                    if (input.trim().isEmpty()) {
                        alertDialog.input.setError(getString(R.string.error_field_required));
                    } else {
                        alertDialog.input.setError(getString(R.string.error_invalid_email));
                    }
                }
            }
        });

    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }


    private void attemptSignUp(){
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        userNameEditText.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String userName = userNameEditText.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check if the username is valid.
        if(TextUtils.isEmpty(userName)){
            userNameEditText.setError(getString(R.string.error_field_required));
            focusView = userNameEditText;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            // Show a progress spinner, and kick off a background task to
            // perform the user sign up attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);

        }
    }


    /**
     * Launch intent to set see gigs
     */
    private void launchFirstIntent(){
        // TODO: Change the logic here: Kess
        Intent i = new Intent(LoginActivity.this, Welcome.class);
        i.putExtra("userName", userNameEditText.getText());
        i.putExtra("userEmail", mEmailView.getText());
        i.putExtra("userPassword", mPasswordView.getText());
        startActivity(i);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setUpSignIn() {
        /**
         * Animate text slide in
         */
        userName.animate().x(-1000).setDuration(1500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                createAccountTextView.setClickable(true);
                userName.setVisibility(View.GONE);
                mEmailSignInButton.setText("Login");
                animation.cancel();
            }
        }).start();



        /**
         * Animate text slide out
         */
        mEmailSignInButton.setText(R.string.action_sign_in);
        forgotPasswordTextView.animate().xBy(1500).setDuration(1500).start();
        forgotPasswordTextView.setVisibility(View.VISIBLE);
//        userName.setVisibility(View.INVISIBLE);

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        View focusView;
        focusView = mEmailView;
        focusView.requestFocus();

    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setUpSignUp() {
        /**
         * Animate text slide in
         */
        userName.animate().x(0).setDuration(1500).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mEmailSignInButton.setText("Sign Up");
                createAccountTextView.setClickable(true);
                animation.cancel();
            }
        }).start();

        forgotPasswordTextView.animate().xBy(-1500).setDuration(1500).start();

        userName.setVisibility(View.VISIBLE);

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        View focusView;
        focusView = userNameEditText;
        focusView.requestFocus();

    }

    private boolean isEmailValid(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */

    public class UserSignUpTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUserName;
        private final String mEmail;
        private final String mPassword;

        UserSignUpTask(String userName, String email, String password) {
            mEmail = email;
            mPassword = password;
            mUserName = userName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.



            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);


//            if (success) {
//                finish();
//            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                mPasswordView.requestFocus();
//            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            // TODO: Change the logic here: Kess
            if (success) {
                startActivity(new Intent(LoginActivity.this,Welcome.class));
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
