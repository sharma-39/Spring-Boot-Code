package com.example.MySQL.PostalCode;

public class EntityModule {

    String postalCode;
    String timezoneApp;
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getTimezoneApp() {
        return timezoneApp;
    }

    public void setTimezoneApp(String timezoneApp) {
        this.timezoneApp = timezoneApp;
    }

    public EntityModule(String postalCode, String timezoneApp) {
        this.postalCode = postalCode;
        this.timezoneApp = timezoneApp;
    }




}
