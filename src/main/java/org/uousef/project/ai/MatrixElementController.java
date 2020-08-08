package org.uousef.project.ai;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class MatrixElementController {
    @FXML
    private VBox elementBackground;
    @FXML
    private Text textValue;

    void setupInt(int value, int max, double size) {

        double fontSize = size / 70.0;
        fontSize = (fontSize >= 1) ? 18 : 18 * fontSize;

        double intensity = value / (double) max;
        intensity = (value * (80 / (double) max)) + 5;
        if (Double.isNaN(intensity)) intensity = 0.0;

        textValue.setText(String.valueOf(value));
        elementBackground.setPrefHeight(size);
        elementBackground.setPrefWidth(size);
        textValue.setFill(Color.web("#14213D"));
        elementBackground.setStyle("-fx-background-color: hsb(37," + intensity + "%,99%);"
                + "-fx-font-size:" + fontSize + ";");
    }

    void setupString(String string, int number, double size) {
        double fontSize = size / 70.0;
        fontSize = (fontSize >= 1) ? 18 : 18 * fontSize;
        string += ("\n(" + number + ")");

        textValue.setText(string);
        elementBackground.setPrefHeight(size);
        elementBackground.setPrefWidth(size);
        elementBackground.setStyle("-fx-background-color: #233A6C;"
                + "-fx-font-size:" + fontSize + ";"
                + "-fx-border-color: black");
    }

}
