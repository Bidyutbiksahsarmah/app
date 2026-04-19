package com.example.music;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NewAdaptor extends RecyclerView.Adapter<NewAdaptor.ViewHolder> implements Filterable {

    private List<String> originalList;
    private List<String> filteredList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position, String songName);
    }

    public NewAdaptor(String[] objects, OnItemClickListener listener) {
        this.originalList = new ArrayList<>();
        for (String s : objects) {
            originalList.add(s);
        }
        this.filteredList = new ArrayList<>(originalList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String songName = filteredList.get(position);
        holder.textView.setText(songName);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position, songName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charString = constraint.toString();
                if (charString.isEmpty()) {
                    filteredList = new ArrayList<>(originalList);
                } else {
                    List<String> filtered = new ArrayList<>();
                    for (String row : originalList) {
                        if (row.toLowerCase().contains(charString.toLowerCase())) {
                            filtered.add(row);
                        }
                    }
                    filteredList = filtered;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (ArrayList<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
