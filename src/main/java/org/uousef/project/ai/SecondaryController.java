package org.uousef.project.ai;


import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.uousef.project.ai.modules.NeuralNetwork;

public class SecondaryController implements Initializable {

    private static double[][] a = {
            {0, 0},
            {1, 0},
            {0, 1},
            {1, 1},
    },
            b = {
                    {1},
                    {0},
                    {0},
                    {1},
            };
    private NeuralNetwork neuralNetwork;
    private volatile boolean learningStarted;

    void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
        this.learningStarted = false;
    }

    @FXML
    private Button start;

    @FXML
    private Text MSE;

    @FXML
    void start(ActionEvent event) {
        long t = System.currentTimeMillis();
        if (!learningStarted) {
            learningStarted = true;
            new Thread(() -> {
                neuralNetwork.training(a, b, () -> Platform.runLater(() -> {
                    MSE.setText(String.format("MSE: %s", neuralNetwork.currentMES));
                }));
                System.out.println(System.currentTimeMillis() - t);
                learningStarted = false;
                System.gc();
            }).start();
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}