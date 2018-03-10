package jayantb95.firebasegooglesignin.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.sql.Struct;
import java.util.HashMap;

import jayantb95.firebasegooglesignin.R;
import jayantb95.firebasegooglesignin.dataModel.UserModel;

import static jayantb95.firebasegooglesignin.helpers.DataConstants.CURRENT_UID;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private Button btnSignOut;
    private GridLayout gridLayoutUserInfo;
    private ImageView imgUserPic;
    private TextView txtDisplayName;
    private TextView txtEmail;
    private TextView txtTitleLogin;

    private String photo;
    private String name;
    private String email;
    private String uid;
    private Uri photoUri;
    private GoogleSignInClient mGoogleSignInClient;

    private int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference userDatabaseRef;
    private SignInButton signInButtonGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initialize();
    }

    private void initialize() {
        signInButtonGoogle = findViewById(R.id.sign_in_button);
        signInButtonGoogle.setSize(SignInButton.SIZE_WIDE);
        btnSignOut = findViewById(R.id.btn_sign_out);
        txtDisplayName = findViewById(R.id.txt_name);
        txtEmail = findViewById(R.id.txt_email);
        txtTitleLogin = findViewById(R.id.txt_title_login);
        imgUserPic = findViewById(R.id.img_user_pic);
        gridLayoutUserInfo = findViewById(R.id.grid_lay_user_info);

        configureGoogleSignIn();
        configureFirebase();
        showFirebaseData();
        listener();
    }

    private void listener() {
        signInButtonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(LoginActivity.this, gso);

    }

    private void configureFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // Get signedIn user
                FirebaseUser user = firebaseAuth.getCurrentUser();

                //if user is signed in, we call a helper method to save the user details to Firebase
                if (user != null) {
                    // User is signed in
                    CURRENT_UID = user.getUid();
                    Log.d(TAG, "current uid:" + CURRENT_UID);
//                    showFirebaseData();
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "Signed out success");
                        viewsLogout();
                        clear();
                        Toast.makeText(LoginActivity.this, "Successfully signed out.",
                                Toast.LENGTH_SHORT).show();


                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle id: " + acct.getId());
        Log.d(TAG, "firebaseAuthWithGoogle idtoken: " + acct.getIdToken());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            createUserInFirebase();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Log.w(TAG, "Authentication Failed.");
                        }

                    }
                });
    }

    //This method creates a new user on our own Firebase database
    //after a successful Authentication on Firebase
    private void createUserInFirebase() {
        userDatabaseRef = mFirebaseDatabase.getReference("UserInfo/" + CURRENT_UID);

        userDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null) {

                    Log.d(TAG, "executed");
                    // Insert into Firebase database
                    UserModel user = new UserModel();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPhotoUrl(photo);

                    userDatabaseRef.push().setValue(user);

                    Toast.makeText(LoginActivity.this, "Account created!", Toast.LENGTH_SHORT).show();

                    // After saving data to Firebase, goto next activity
//                    Intent intent = new Intent(MainActivity.this, NavDrawerActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
                }
                showFirebaseData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "Account not created!", Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void showFirebaseData() {
        userDatabaseRef = mFirebaseDatabase.getReference("UserInfo/" + CURRENT_UID);

        userDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    txtDisplayName.setText(ds.child("name").getValue(String.class));
                    txtEmail.setText(ds.child("email").getValue(String.class));

                    Picasso.get().load(ds.child("photoUrl").getValue(String.class))
                            .error(android.R.drawable.ic_menu_camera)
                            .into(imgUserPic);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void viewsLogin() {
        btnSignOut.setVisibility(View.VISIBLE);
        gridLayoutUserInfo.setVisibility(View.VISIBLE);
        signInButtonGoogle.setVisibility(View.GONE);
        txtTitleLogin.setVisibility(View.GONE);
    }

    private void viewsLogout() {
        btnSignOut.setVisibility(View.GONE);
        gridLayoutUserInfo.setVisibility(View.GONE);
        signInButtonGoogle.setVisibility(View.VISIBLE);
        txtTitleLogin.setVisibility(View.VISIBLE);
    }

    private void clear() {
        imgUserPic.setImageResource(0);
        txtDisplayName.setText("");
        txtEmail.setText("");

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                name = account.getDisplayName();
                email = account.getEmail();
                photoUri = account.getPhotoUrl();
                if (photoUri != null) {
                    photo = photoUri.toString();
                }

                Log.d(TAG, "Account id: " + account.getId());
                Log.d(TAG, "Account idtoken: " + account.getIdToken());
                viewsLogin();
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(LoginActivity.this);
//        firebaseAuthWithGoogle(account);
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthStateListener != null) {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
        clear();
    }
}
