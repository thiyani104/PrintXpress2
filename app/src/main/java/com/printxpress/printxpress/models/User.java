package com.printxpress.printxpress.models;

public class User {
    private final long id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String dob;
    private final String address;

    public User(long id, String fullName, String email, String phone, String dob, String address) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dob = dob;
        this.address = address;
    }

    public long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getDob() { return dob; }
    public String getAddress() { return address; }

    public String getFirstName() {
        String n = fullName == null ? "" : fullName.trim();
        int space = n.indexOf(' ');
        return space > 0 ? n.substring(0, space) : n;
    }

    public String getInitials() {
        String n = fullName == null ? "" : fullName.trim();
        if (n.isEmpty()) return "?";
        String[] parts = n.split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
