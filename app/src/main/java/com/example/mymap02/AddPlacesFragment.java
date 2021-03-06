package com.example.mymap02;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddPlacesFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public AddPlacesFragment() {
    }

    public static AddPlacesFragment newInstance(String param1, String param2) {
        AddPlacesFragment fragment = new AddPlacesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    } // ????????????

    DatabaseReference databaseReference;
    FirebaseDatabase firebaseDatabase;
    EditText edName, edAddress;
    Button btnSave;
    ImageView imgPlace;
    String fileName;
    Uri imageUri;
    Button btnSearch;
    Button btnGoogle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_places, container, false);

        firebaseDatabase = FirebaseDatabase.getInstance("https://my-project-345710-default-rtdb.firebaseio.com/");
        edName = v.findViewById(R.id.edName);
        edAddress = v.findViewById(R.id.edAddress);
        btnSave = v.findViewById(R.id.btnSave);
        imgPlace = v.findViewById(R.id.imgPlace);
        btnSearch = v.findViewById(R.id.btnSearch);
        btnGoogle = v.findViewById(R.id.btnGoogle);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, "");
                startActivity(intent);
            }
        });

        btnGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("http://maps.google.com/maps?q=");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        imgPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!checkCamearPermission()) {
                    ActivityCompat.requestPermissions(
                            getActivity(), new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE}
                            , 2
                    );
                } else {
                    takeImage();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri != null) {
                    Places places = new Places();
                    ProgressDialog progressDialog = new ProgressDialog((MainActivity) getActivity());
                    progressDialog.setTitle("Uploading ...");
                    progressDialog.show();

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss", Locale.CANADA);
                    Date now = new Date();
                    fileName = formatter.format(now);

                    StorageReference Imagename = FirebaseStorage.getInstance().getReference("images/" + fileName);
                    Imagename.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText((MainActivity) getActivity(), "Uploaded", Toast.LENGTH_SHORT).show();
                            Imagename.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @RequiresApi(api = Build.VERSION_CODES.M)
                                @Override
                                public void onSuccess(Uri uri) {
                                    places.setName(edName.getText().toString());
                                    places.setAddress(edAddress.getText().toString());
                                    places.setImage(String.valueOf(uri));

                                    // key?????? ??????????????? ???????????????
//                                String str = place0.toString();
//                                int index = str.lastIndexOf('.');
//                                String str2 = str.substring(index+1, str.length());
//                                System.out.println(str2);
//                                databaseReference = firebaseDatabase.getReference("placesinfo").child(str2);

                                    Places place0 = new Places(edName.getText().toString(), edAddress.getText().toString(), String.valueOf(uri));

                                    databaseReference = firebaseDatabase.getReference("placesinfo").child(fileName);

                                    databaseReference.setValue(place0);
                                    edName.setText("");  // ?????? ?????? ??? textView ??????????????????
                                    edAddress.setText("");
                                    imgPlace.setImageResource(R.drawable.gallery); // ???????????? ?????? ?????? ??? ????????? ?????????

                                    if (progressDialog.isShowing())
                                        progressDialog.dismiss();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (progressDialog.isShowing())
                                progressDialog.dismiss();

                            Toast.makeText((MainActivity) getActivity(), "Failed to Upload", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double progress = 100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount();
                            progressDialog.setMessage(" Uploaded " + (int) progress + "%");
                        }
                    });
                } else {
                    Toast.makeText((MainActivity) getActivity(), "???????????? ???????????????", Toast.LENGTH_SHORT).show();
                }
                imageUri = null;
            }
        });
        return v;
    }

    public boolean checkCamearPermission() {
        int result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
        int result2 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result3 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);

        return result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED &&
                result3 == PackageManager.PERMISSION_GRANTED;
    }

    void takeImage() {
        // ?????????????????? ??????
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // ?????? ???????????? ??????
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgPlace.setImageURI(imageUri);
        }
    }
}