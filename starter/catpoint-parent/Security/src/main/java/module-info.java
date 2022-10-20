module Security {
    exports com.udacity.catpoint.security.application;
    exports com.udacity.catpoint.security.service;
    exports com.udacity.catpoint.security.data;
    requires Image;
    requires java.desktop;
    requires com.google.gson;
    requires com.google.common;
    requires java.prefs;

    opens com.udacity.catpoint.security.data to com.google.gson;
}