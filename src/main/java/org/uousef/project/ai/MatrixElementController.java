package org.uousef.project.ai;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class MatrixElementController {
    @FXML
    private VBox elementBackground;
    @FXML
    private Text textValue;

    private int value;
    private double intensity;

    void setupInt(int value, int max, double size) {
        this.value = value;
        this.intensity = value / (double) max;
        double fontSize = size / 70.0;
        fontSize = (fontSize >= 1) ? 18 : 18 * fontSize;


        this.intensity = (value * (60 / (double) max)) + 10;
        intensity = 100 - intensity;

        textValue.setText(String.valueOf(this.value));
        elementBackground.setPrefHeight(size);
        elementBackground.setPrefWidth(size);
        elementBackground.setStyle("-fx-background-color: hsb(221, 67%," + intensity + "%);"
                + "-fx-font-size:" + fontSize + ";");
    }

    void setupString(String string, int number, double size) {
        double fontSize = size / 70.0;
        fontSize = (fontSize >= 1) ? 18 : 18 * fontSize;

        string += ("\n(" + number + ")");
        textValue.setText(string);
        elementBackground.setPrefHeight(size);
        elementBackground.setPrefWidth(size);
        elementBackground.setStyle("-fx-background-color: hsb(221, 67%, 85%);"
                + "-fx-font-size:" + fontSize + ";");
    }

}
