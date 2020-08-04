package org.uousef.project.ai;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import org.uousef.project.ai.modules.NeuralNetwork;

public class SecondaryController implements Initializable {

    static double[][] a = {
            {0, 0},
            {1, 0},
            {0, 1},
            {1, 1},
    },
            b = {
                    {0},
                    {0},
                    {0},
                    {1},
            };
    private NeuralNetwork neuralNetwork;

    void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
        System.out.println(neuralNetwork.inputNeuronNumber);
        System.out.println(neuralNetwork.nodeOutputs.length);
        System.out.println(neuralNetwork.nodeOutputs[neuralNetwork.nodeOutputs.length - 1].length);
    }

    @FXML
    private Button start;

    @FXML
    private Text MSE;

    @FXML
    void start(ActionEvent event) {
        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(a, b,() -> {
                Platform.runLater(() -> {
                    MSE.setText("MSE: "+ neuralNetwork.currentMES);
                    System.out.println(neuralNetwork.currentMES);
                });
            });
            System.out.println(System.currentTimeMillis() - t);
        }).start();
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}