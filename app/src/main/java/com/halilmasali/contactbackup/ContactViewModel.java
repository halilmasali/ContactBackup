package com.halilmasali.contactbackup;

public class ContactViewModel {
    private final String name;
    private final String phoneNumber;

    public ContactViewModel(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
