package com.example.music;

import android.Manifest;
import android.content.Intent;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.service.media.MediaBrowserService;
import android.view.View;import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    ListView listView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);

        ArrayList<String> songList = new ArrayList<>();
            listView = findViewById(R.id.listView);
            requestPermission();
        }
        private void requestPermission() {

            String permission;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission = Manifest.permission.READ_MEDIA_AUDIO;
            } else {
                permission = Manifest.permission.READ_EXTERNAL_STORAGE;
            }
            Dexter.withContext(this)
                .withPermission(permission)
                .withListener(new PermissionListener()
                {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
//                        Toast toast = Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT);
//                        toast.show();
                        ArrayList<File> mySongs = findSong(Environment.getExternalStorageDirectory());
                        Collections.sort(mySongs, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        String [] items = new String[mySongs.size()];
                        for(int i=0;i<mySongs.size();i++){
                            items[i]=mySongs.get(i).getName().replace(".mp3","");
                        }
                        ArrayAdapter<String> ad = new ArrayAdapter<String>(
                                MainActivity.this,
                                android.R.layout.simple_list_item_1,
                                items
                        )
                        {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);

                                android.widget.TextView textView = view.findViewById(android.R.id.text1);
                                textView.setTextColor(getResources().getColor(android.R.color.white,null));

                                return view;
                            }
                        };

NewAdaptor adapter = new NewAdaptor(MainActivity.this,R.layout.items,items);

                        listView.setAdapter(adapter);

                        listView.setOnItemClickListener((parent, view, position, id) ->{

                            Intent intent = new Intent(MainActivity.this,PlaySong.class);
                            String currentSong = listView.getItemAtPosition(position).toString();
                            intent.putExtra("songList",mySongs);
                            intent.putExtra("currentSong",currentSong);
                            intent.putExtra("position",position);
                            startActivity(intent);

                        });
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                     //   finish();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();

                    }
                })
                .check();
    }
    public ArrayList<File> findSong(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findSong(singleFile));
                } else {
                    if (singleFile.getName().endsWith(".mp3")&& !singleFile.getName().startsWith(".") ){
                        arrayList.add(singleFile);
                    }
                }
        }
    }
        return arrayList;
    }
}
