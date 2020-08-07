package org.uousef.project.ai;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ConfusionMatrixController implements Initializable {
    @FXML
    private GridPane matrix;

    void setupMatrix(double[] className, int[][] confusionMatrix, int[] actual, double width, double height) throws IOException {
        double size = Math.min(width, height);
        matrix.setPrefWidth(size);
        matrix.setPrefHeight(size);
        size = size / (double) (actual.length + 1);
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("matrixElement.fxml"));
//        Parent newLoadedPane = null;
        MatrixElementController matrixElement;
        matrix.getChildren().removeIf((node -> true));

        for (int indexCol = 0; indexCol <= className.length; indexCol++) {
            for (int indexRow = 0; indexRow <= className.length; indexRow++) {
                System.out.println("ite" + indexCol + "," + indexRow);
                loader = new FXMLLoader();
                loader.setLocation(App.class.getResource("matrixElement.fxml"));
                Parent newLoadedPane = loader.load();
                matrixElement = loader.getController();

                if (indexCol == 0 && indexRow == 0)
                    continue;
                else if (indexCol == 0) {
                    matrixElement.setupString("[" + className[indexRow - 1]+"]", actual[indexRow - 1], size);
                } else if (indexRow == 0) {
                    matrixElement.setupString("[" + className[indexCol - 1]+"]", actual[indexCol - 1], size);
                } else {
                    //TODO: col vs row
                    matrixElement.setupInt(confusionMatrix[indexCol - 1][indexRow - 1], actual[indexCol - 1], size);
                }
                matrix.add(newLoadedPane, indexCol, indexRow);
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}

