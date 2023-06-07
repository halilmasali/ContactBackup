package com.halilmasali.contactbackup;

public class ContactViewModel {
    private final String name;
    private final String phoneNumber;
    private final String contactId;

    public ContactViewModel(String name, String phoneNumber, String contactId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.contactId = contactId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getContactId() { return contactId; }
}
