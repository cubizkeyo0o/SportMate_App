package com.sportmate.app.presentation.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.R;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public final class TextListAdapter extends RecyclerView.Adapter<TextListAdapter.TextViewHolder> {
    private final List<String> items;

    public TextListAdapter(List<String> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    @NonNull
    @Override
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text, parent, false);
        return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
        holder.text.setText(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class TextViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.item_text);
        }
    }
}