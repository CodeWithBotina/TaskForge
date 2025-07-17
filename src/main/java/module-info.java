module com.taskforge {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;

    // Use these exact module names:
    requires org.xerial.sqlitejdbc;  // From dependency:list output
    requires org.slf4j;              // SLF4J API
    // requires org.slf4j.nop;      // Not needed - implementation is automatic

    opens com.taskforge.ui to javafx.fxml;
    opens com.taskforge.ui.controllers to javafx.fxml;
    opens com.taskforge.model to javafx.base;

    exports com.taskforge.ui;
    exports com.taskforge.model;
    exports com.taskforge.dao;
    exports com.taskforge.service;
    exports com.taskforge.util;
}