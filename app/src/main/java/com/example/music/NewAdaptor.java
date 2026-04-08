package com.example.music;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class NewAdaptor extends ArrayAdapter<String> {
    String objects[];

    public NewAdaptor(@NonNull Context context, int resource,@NonNull String[] objects) {
        super(context, resource, objects);
        this.objects = objects;

    }
    @Nullable
    @Override
    public String getItem(int position) {
        return objects[position];
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
convertView= LayoutInflater.from(getContext()).inflate(R.layout.items,parent,false);
TextView t=convertView.findViewById(R.id.textView);
t.setText(getItem(position));
        return convertView;
    }
}
