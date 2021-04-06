package com.example.lifebank.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.lifebank.R;
import com.example.lifebank.model.UserData;
import com.goodiebag.pinview.Pinview;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputemail, inputpassword, retypePassword, fullName, etAddress, contact;
    private FirebaseAuth mAuth;
    private Button btnSignup, btnOtp, btnverify;
    private ProgressDialog pd;
    private Spinner gender, bloodgroup, division;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    TextView otpNote;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String sendCode;
    private String email;
    private String password;
    private String ConfirmPassword;
    private String Name;
    private int Gender;
    private String Contact;
    private int BloodGroup;
    private String Address;
    private int Division;
    private String blood;
    private String div;
    private Pinview otp;
    private String enteredCode;
    private PhoneAuthCredential credential;
    private AppCompatImageView ivVerified;


    private boolean isUpdate = false;

    private DatabaseReference db_ref, donor_ref;
    private FirebaseDatabase db_User;
    private CheckBox isDonor;
    private static  final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setListener();
    }

    private void setListener() {
        pd.dismiss();
        btnOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*USE OF GOOGLE CLOUD PLATFORM -> API SERVICE->ANDROID DEVICE VERIFICATION FOR REMOVE RE-CAPTCHA*/

                setmCallbacks();
                createPopUP();
                sendOtp();
            }
        });


        if (mAuth.getCurrentUser() != null) {

            inputemail.setVisibility(View.GONE);
            inputpassword.setVisibility(View.GONE);
            retypePassword.setVisibility(View.GONE);
            btnSignup.setText("Update Profile");
            pd.dismiss();
            getSupportActionBar().setTitle("Profile");
            findViewById(R.id.image_logo).setVisibility(View.GONE);
            isUpdate = true;

            Query Profile = db_ref.child(mAuth.getCurrentUser().getUid());
            Profile.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                    UserData userData = dataSnapshot.getValue(UserData.class);

                    if (userData != null) {
                        pd.show();
                        fullName.setText(userData.getName());
                        gender.setSelection(userData.getGender());
                        etAddress.setText(userData.getAddress());
                        contact.setText(userData.getContact());
                        btnOtp.setVisibility(View.GONE);
                        ivVerified.setVisibility(View.VISIBLE);
                        bloodgroup.setSelection(userData.getBloodGroup());
                        division.setSelection(userData.getDivision());
                        Query donor = donor_ref.child(division.getSelectedItem().toString())
                                .child(bloodgroup.getSelectedItem().toString())
                                .child(mAuth.getCurrentUser().getUid());

                        donor.addListenerForSingleValueEvent(new ValueEventListener() {

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.exists()) {
                                    isDonor.setChecked(true);
                                    isDonor.setText("Unmark this to leave from donors");
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Your are not a donor! Be a donor and save life by donating blood.",
                                            Toast.LENGTH_LONG).show();
                                }
                                pd.dismiss();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.d("User", databaseError.getMessage());
                            }

                        });
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d("User", databaseError.getMessage());
                }
            });


        } else pd.dismiss();
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                email = inputemail.getText().toString();
                password = inputpassword.getText().toString();
                ConfirmPassword = retypePassword.getText().toString();
                Name = fullName.getText().toString();
                Gender = gender.getSelectedItemPosition();
                Contact = contact.getText().toString();
                BloodGroup = bloodgroup.getSelectedItemPosition();
                Address = etAddress.getText().toString();
                Division = division.getSelectedItemPosition();
                blood = bloodgroup.getSelectedItem().toString();
                div = division.getSelectedItem().toString();

                if (validation()) {
                    pd.show();
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {

                                    if (task.isSuccessful()) {
                                        String id = mAuth.getCurrentUser().getUid();
                                        db_ref.child(id).child("Name").setValue(Name);
                                        db_ref.child(id).child("Gender").setValue(Gender);
                                        db_ref.child(id).child("Contact").setValue(Contact);
                                        db_ref.child(id).child("BloodGroup").setValue(BloodGroup);
                                        db_ref.child(id).child("Address").setValue(Address);
                                        db_ref.child(id).child("Division").setValue(Division);

                                        if (isDonor.isChecked()) {
                                            donor_ref.child(div).child(blood).child(id).child("UID").setValue(id).toString();
                                            donor_ref.child(div).child(blood).child(id).child("LastDonate").setValue("Don't donate yet!");
                                            donor_ref.child(div).child(blood).child(id).child("TotalDonate").setValue(0);
                                            donor_ref.child(div).child(blood).child(id).child("Name").setValue(Name);
                                            donor_ref.child(div).child(blood).child(id).child("Contact").setValue(Contact);
                                            donor_ref.child(div).child(blood).child(id).child("Address").setValue(Address);

                                        }

                                        Toast.makeText(getApplicationContext(), "Welcome, your account has been created!", Toast.LENGTH_LONG)
                                                .show();
                                        Intent intent = new Intent(RegisterActivity.this, Dashboard.class);
                                        startActivity(intent);

                                        finish();
                                    }
                                    pd.dismiss();
                                }

                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void init() {
        pd = new ProgressDialog(this);
        pd.setMessage("Loading...");
        pd.setCancelable(true);
        pd.setCanceledOnTouchOutside(false);
        pd.show();
        setContentView(R.layout.activity_profile);

        db_User = FirebaseDatabase.getInstance();
        db_ref = db_User.getReference("users");
        donor_ref = db_User.getReference("donors");
        mAuth = FirebaseAuth.getInstance();

        inputemail = findViewById(R.id.input_userEmail);
        inputpassword = findViewById(R.id.input_password);
        retypePassword = findViewById(R.id.input_password_confirm);
        fullName = findViewById(R.id.input_fullName);
        gender = findViewById(R.id.gender);
        etAddress = findViewById(R.id.inputAddress);
        division = findViewById(R.id.inputDivision);
        bloodgroup = findViewById(R.id.inputBloodGroup);
        contact = findViewById(R.id.inputMobile);
        isDonor = findViewById(R.id.checkbox);
        btnOtp = findViewById(R.id.btn_otp);
        btnSignup = findViewById(R.id.button_register);
        ivVerified = findViewById(R.id.ivVerified);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private boolean validation() {
        if (Name.isEmpty()) {
            fullName.setError("Please enter name");
            fullName.requestFocus();
            return false;
        } else if (Contact.length() != 10) {
            contact.setError("Contact number must be 10 digit");
            contact.requestFocus();
            return false;
        }else if (btnOtp.isClickable()){
            contact.setError("Please verify your Phone Number");
            contact.requestFocus();
            return false;
        }else if (Address.isEmpty()) {
            etAddress.setError("Please enter address");
            etAddress.requestFocus();
            return false;
        } else if (email.isEmpty()) {
            inputemail.setError("Please enter email");
            inputemail.requestFocus();
            return false;
        } else if (password.length() < 6) {
            inputpassword.setError("Password length must be 6 or greater");
            inputpassword.requestFocus();
            return false;
        } else if (retypePassword.length() < 6) {
            retypePassword.setError("Password length must be 6 or greater");
            retypePassword.requestFocus();
            return false;
        } else if (password.compareTo(ConfirmPassword) != 0) {
            retypePassword.setError("Password not match");
            retypePassword.requestFocus();
            return false;
        }
        return true;
    }

    private void createPopUP() {
            builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.otp_authentication, null);
            otpNote = (TextView) view.findViewById(R.id.otpNote);
            otpNote.setText("We have send OTP on your number!");
            otp = view.findViewById(R.id.pinViewOtp);
            otp.requestFocus();
            btnverify = (Button) view.findViewById(R.id.btn_verify);
            builder.setView(view);
            dialog = builder.create();
            dialog.show();
            btnverify.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enteredCode = otp.getValue();

                    if (enteredCode.length() < 6) {
                        Toast.makeText(RegisterActivity.this, "Please enter 6 digit OTP", Toast.LENGTH_SHORT).show();
                    } else if (sendCode != null && enteredCode != null) {
                        credential = PhoneAuthProvider.getCredential(sendCode, enteredCode);
                        signInWithPhoneAuthCredential(credential);
                    }
                }
            });
    }

    private void ShowError(String error) {

        Toast.makeText(RegisterActivity.this, "Please, Enter a valid " + error,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendOtp() {
            String phone = contact.getText().toString();

            PhoneAuthOptions options =
                    PhoneAuthOptions.newBuilder(mAuth)
                            .setPhoneNumber("+91" + phone)       // Phone number to verify
                            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                            .setActivity(this)                 // Activity (for callback binding)
                            .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                            .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
    }
    private void setmCallbacks() {
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {



            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: "+e.getMessage() );

            }

            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                sendCode = s;
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            dialog.dismiss();
                            btnOtp.setClickable(false);
                            ivVerified.setVisibility(View.VISIBLE);
                            btnOtp.setVisibility(View.GONE);
                            etAddress.requestFocus();

                        } else {
                            Toast.makeText(RegisterActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
