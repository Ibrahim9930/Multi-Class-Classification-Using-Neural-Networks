package org.uousef.project.ai;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import org.uousef.project.ai.modules.ActivationFunction;
import org.uousef.project.ai.modules.LayerInformation;
import org.uousef.project.ai.modules.NeuralNetwork;

public class PrimaryController implements Initializable {

    @FXML private Group group;
    @FXML private Pane neuralNetworkPane;
    @FXML private JFXCheckBox adaptiveLearning;
    @FXML private JFXComboBox<ActivationFunction> comboBoxOutputActivation, comboBoxActivationFunction;
    @FXML private TextField textFieldInputNumber, textFieldOutputNumber,
            textFieldLearningRate, textFieldMaxEpoch,
            textFieldMSE, textFieldNeuronNo;

    private ArrayList<LayerInformation> hiddenLayers;
    private int inputNeuronNumber = 2, outputNeuronNumber = 1, maxEpoch = 10000, neuronNumber = 1;
    private double learningRate = 0.1, acceptedMSE = 0.0001;
    private boolean adaptiveLearningFlag = false;

    private void drawing() {
        ArrayList<Point2D>
                previousNeurons = new ArrayList<>(),
                currentNeurons = new ArrayList<>();
        double
                width = neuralNetworkPane.widthProperty().get(),
                height = neuralNetworkPane.heightProperty().get(),
                availableWidth = width - 80,
                availableHeight = height - 20,
                horizontalStepValue, verticalStepValue;

        int index, nestedIndex, indexAtPreviousLayer;

        Point2D neuronPoint;

        group.getChildren().clear();

        verticalStepValue = availableHeight / (double) (inputNeuronNumber + 1);
        horizontalStepValue = availableWidth / (double) (hiddenLayers.size() + 1);

        //input layer
        for (index = 0; index < inputNeuronNumber; index++) {
            neuronPoint = new Point2D(40, 10 + verticalStepValue * (index + 1));
            group.getChildren().add(new Circle(neuronPoint.getX(), neuronPoint.getY(), 20, Color.web("#FCA311")));
            previousNeurons.add(neuronPoint);
        }

        //hidden layers
        for (index = 0; index < hiddenLayers.size(); index++) {
            verticalStepValue = availableHeight / (double) (hiddenLayers.get(index).neuronNumber + 1);

            for (nestedIndex = 0; nestedIndex < hiddenLayers.get(index).neuronNumber; nestedIndex++) {
                neuronPoint = new Point2D(40 + horizontalStepValue * (index + 1), 10 + verticalStepValue * (nestedIndex + 1));
                group.getChildren().add(new Circle(neuronPoint.getX(), neuronPoint.getY(), 20, Color.web("#457b9d")));

                currentNeurons.add(neuronPoint);
                for (indexAtPreviousLayer = 0; indexAtPreviousLayer < previousNeurons.size(); indexAtPreviousLayer++) {
                    group.getChildren().add(new Line(neuronPoint.getX() - 20, neuronPoint.getY(),
                            previousNeurons.get(indexAtPreviousLayer).getX() + 20, previousNeurons.get(indexAtPreviousLayer).getY()));
                }
            }
            previousNeurons = new ArrayList<>(currentNeurons);
            currentNeurons.clear();
        }

        //output layer
        verticalStepValue = availableHeight / (double) (outputNeuronNumber + 1);
        for (index = 0; index < outputNeuronNumber; index++) {
            neuronPoint = new Point2D(40 + availableWidth, 10 + verticalStepValue * (index + 1));
            group.getChildren().add(new Circle(neuronPoint.getX(), neuronPoint.getY(), 20, Color.web("#1d3557")));

            for (indexAtPreviousLayer = 0; indexAtPreviousLayer < previousNeurons.size(); indexAtPreviousLayer++) {
                group.getChildren().add(new Line(neuronPoint.getX() - 20, neuronPoint.getY(),
                        previousNeurons.get(indexAtPreviousLayer).getX() + 20, previousNeurons.get(indexAtPreviousLayer).getY()));
            }
        }
        System.gc();
    }

    @FXML
    void addLayer() {
        hiddenLayers.add(new LayerInformation(this.neuronNumber, this.comboBoxActivationFunction.getSelectionModel().getSelectedItem()));
        Platform.runLater(this::drawing);
    }

    @FXML
    void removeLayer() {
        try {
            hiddenLayers.remove(hiddenLayers.size() - 1);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Platform.runLater(this::drawing);
    }

    @FXML
    void chooseDatasetPage(ActionEvent event) {
        ArrayList<LayerInformation> temp = new ArrayList<>(hiddenLayers);
        temp.add(new LayerInformation(outputNeuronNumber, comboBoxOutputActivation.getSelectionModel().getSelectedItem()));

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("secondary.fxml"));
            Parent secondaryParent = loader.load();
            Scene SecondaryScene = new Scene(secondaryParent);

            SecondaryController secondaryController = loader.getController();
            secondaryController.setNeuralNetwork(new NeuralNetwork(temp, learningRate, acceptedMSE, inputNeuronNumber, maxEpoch,adaptiveLearningFlag));
            System.gc();

            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(SecondaryScene);
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void adaptiveLearningChanged() {
        adaptiveLearningFlag = adaptiveLearning.isSelected();
    }

    private int checkIfInteger(String number) {
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        hiddenLayers = new ArrayList<>();


        textFieldInputNumber.textProperty().addListener((obs, oldValue, newValue) -> {
            int temp = checkIfInteger(newValue);
            if ((temp > 0)) {
                inputNeuronNumber = temp;
            } else {
                inputNeuronNumber = 2;
                textFieldInputNumber.setText("");
            }
            Platform.runLater(this::drawing);
        });

        textFieldOutputNumber.textProperty().addListener((obs, oldValue, newValue) -> {
            int temp = checkIfInteger(newValue);
            if ((temp > 0)) {
                outputNeuronNumber = temp;
            } else {
                outputNeuronNumber = 1;
                textFieldOutputNumber.setText("");
            }
            Platform.runLater(this::drawing);
        });

        comboBoxOutputActivation.getItems().addAll(ActivationFunction.TANH,
                ActivationFunction.SIGMOID);//, ActivationFunction.SOFTMAX);
        comboBoxOutputActivation.getSelectionModel().select(0);

        textFieldLearningRate.textProperty().addListener((obs, oldValue, newValue) -> {
            double temp;
            try {
                temp = Double.parseDouble(newValue);
                learningRate = temp;
            } catch (Exception e) {
                learningRate = 0.1;
                textFieldOutputNumber.setText("");
            }
        });

        textFieldMaxEpoch.textProperty().addListener((obs, oldValue, newValue) -> {
            int temp = checkIfInteger(newValue);
            if ((temp >= 0)) {
                maxEpoch = temp;
            } else {
                maxEpoch = 10000;
                textFieldMaxEpoch.setText("");
            }
        });

        textFieldMSE.textProperty().addListener((obs, oldValue, newValue) -> {
            double temp;
            try {
                temp = Double.parseDouble(newValue);
                acceptedMSE = temp;
            } catch (Exception e) {
                acceptedMSE = 0.0001;
                textFieldOutputNumber.setText("");
            }
        });

        textFieldNeuronNo.textProperty().addListener((obs, oldValue, newValue) -> {
            int temp = checkIfInteger(newValue);
            if ((temp > 0)) {
                neuronNumber = temp;
            } else {
                neuronNumber = 1;
                textFieldNeuronNo.setText("");
            }
        });

        comboBoxActivationFunction.getItems().addAll(ActivationFunction.LINEAR, ActivationFunction.ReLU,
                ActivationFunction.TANH, ActivationFunction.SIGMOID);
        comboBoxActivationFunction.getSelectionModel().select(0);

        Platform.runLater(this::drawing);
    }
}
