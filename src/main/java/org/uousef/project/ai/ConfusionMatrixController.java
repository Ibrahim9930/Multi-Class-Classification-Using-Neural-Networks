package org.uousef.project.ai;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class ConfusionMatrixController implements Initializable {
    @FXML
    private GridPane matrix;

    void setupMatrix(double[] className, int[][] confusionMatrix, int[] predicted, double width, double height) throws IOException {
        double size = Math.min(width, height);
        int actual = 0;
        char temp = 'A';

        matrix.setPrefWidth(size);
        matrix.setPrefHeight(size);
        size = size / (double) (predicted.length + 1);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("matrixElement.fxml"));
        MatrixElementController matrixElement;
        matrix.getChildren().removeIf((node -> true));

        for (int indexRows = 0; indexRows <= className.length; indexRows++) {
            if (indexRows > 0)
                actual = IntStream.of(confusionMatrix[indexRows - 1]).sum();

            for (int indexColumn = 0; indexColumn <= className.length; indexColumn++) {
                loader = new FXMLLoader();
                loader.setLocation(App.class.getResource("matrixElement.fxml"));
                Parent newLoadedPane = loader.load();
                matrixElement = loader.getController();

                if (indexRows == 0 && indexColumn == 0)
                    continue;
                else if (indexRows == 0) {
                    temp = (char) (temp + (int) className[indexColumn - 1]);
                    matrixElement.setupString("Class " + temp, predicted[indexColumn - 1], size);
                } else if (indexColumn == 0) {
                    temp = (char) (temp + (int) className[indexRows - 1]);
                    matrixElement.setupString("Class " + temp, actual, size);
                } else {
                    matrixElement.setupInt(confusionMatrix[indexRows - 1][indexColumn - 1], actual, size);
                }
                temp = 'A';
                matrix.add(newLoadedPane, indexColumn,indexRows);
            }
        }
        System.gc();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}

