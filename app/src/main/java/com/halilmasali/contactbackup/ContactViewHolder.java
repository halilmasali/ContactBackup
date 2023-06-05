package com.halilmasali.contactbackup;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ContactViewHolder extends RecyclerView.ViewHolder {
    private final TextView txtName;
    private final TextView txtPhoneNumber;

    public ContactViewHolder(@NonNull View itemView) {
        super(itemView);
        txtName = itemView.findViewById(R.id.txtName);
        txtPhoneNumber = itemView.findViewById(R.id.txtPhoneNumber);
    }

    public void bind(ContactViewModel contact) {
        txtName.setText(contact.getName());
        txtPhoneNumber.setText(contact.getPhoneNumber());
    }
}
