module org.uousef.project.ai {
    requires javafx.controls;
//    requires javafx.graphics;
//    requires javafx.fxml;
    requires com.jfoenix;
    requires javafx.fxml;

    opens org.uousef.project.ai to javafx.fxml;
    exports org.uousef.project.ai;
}