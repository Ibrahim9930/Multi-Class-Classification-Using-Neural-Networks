package org.uousef.project.ai;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.uousef.project.ai.modules.NeuralNetwork;

public class SecondaryController implements Initializable {
    public static String[] symbolPathes = new String[]{
            "src/main/resources/org/uousef/project/ai/symbol_images/classa.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classb.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classc.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classd.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classe.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classf.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classg.PNG",
    };
    public Label currentEpochLbl;
    public LineChart performanceChart;
    public ComboBox classSelection;
    private double[][] inputData, outputData;
    public ScatterChart chart;
    public NumberAxis xAxis;
    public NumberAxis yAxis;
    public TableView<TableData> confusionTable;
    private NeuralNetwork neuralNetwork;
    private volatile boolean learningStarted;
    private XYChart.Series unIdentifiedData;
    private ArrayList<XYChart.Series> classes, tempClasses;
    private XYChart.Series classAData;
    private XYChart.Series classBData;
    private XYChart.Series performanceData;
    private XYChart.Series tempClassASeries, tempClassBSeries;
    private FileChooser fileChooser;
    private boolean dataFromGraph;
    private String fileDataJson;
    private int chosenClass;
    private int inputsCount;


    void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
        this.learningStarted = false;

        classes = new ArrayList<XYChart.Series>();

        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        for (int i = 0; i < outputsCount; i++) {
            classes.add(new XYChart.Series());
            chart.getData().add(classes.get(i));
            char classChar = 'A';
            classChar += i;
            classSelection.getItems().add(new ClassItem("Class" + classChar, symbolPathes[i], i));
        }
        classSelection.setValue(classSelection.getItems().get(0));
        System.out.println("Classes number is :" + classes.size());

    }

    @FXML
    private Button start;

    @FXML
    private Label MSE;

    public void startTraining(ActionEvent actionEvent) {
        if (!learningStarted) {
            learningStarted = true;
            if (dataFromGraph)
                learnWithGraphData();
            else
                learnWithFileData();
        }
    }

    public void enterFileData(ActionEvent actionEvent) {

        File file = fileChooser.showOpenDialog(null);
        fileDataJson = "";
        try {
            BufferedReader in = Files.newBufferedReader(file.toPath());

            String line = null;
            while ((line = in.readLine()) != null) {
                fileDataJson += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        dataFromGraph = false;
    }

    public void learnWithFileData() {

        Object obj = JSONValue.parse(fileDataJson);
        JSONArray data = (JSONArray) obj;

        JSONObject tempObject = (JSONObject) data.get(0);
        JSONArray tempInputArray = (JSONArray) tempObject.get("input");
        JSONArray tempOutputArray = (JSONArray) tempObject.get("output");
        inputData = new double[data.size()][tempInputArray.size()];
        outputData = new double[data.size()][tempOutputArray.size()];

        for (int i = 0; i < data.size(); i++) {
            tempObject = (JSONObject) data.get(i);
            Long tempValue;

            tempInputArray = (JSONArray) tempObject.get("input");
            for (int j = 0; j < tempInputArray.size(); j++) {
                tempValue = new Long((long) tempInputArray.get(j));
                inputData[i][j] = tempValue.doubleValue();
            }

            tempOutputArray = (JSONArray) tempObject.get("output");
            for (int j = 0; j < tempOutputArray.size(); j++) {
                tempValue = new Long((long) tempOutputArray.get(j));
                outputData[i][j] = tempValue.doubleValue();
            }
        }
        for (int i = 0; i < inputData.length; i++) {
            System.out.println();
            for (int j = 0; j < inputData[i].length; j++)
                System.out.print(inputData[i][j] + "\t");
        }
        for (int i = 0; i < outputData.length; i++) {
            System.out.println();
            for (int j = 0; j < outputData[i].length; j++)
                System.out.print(outputData[i][j] + "\t");
        }
        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(inputData, outputData, () -> Platform.runLater(() -> {
                        MSE.setText(String.format("Current MSE: %.5f", neuralNetwork.currentMES));
                        currentEpochLbl.setText(String.format("Current Epoch: %s", neuralNetwork.currentEpoch));
                        performanceData.getData().add(new XYChart.Data(neuralNetwork.currentEpoch, neuralNetwork.currentMES));
                    }), () -> {
                    }
            );
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            learningStarted = false;
            System.gc();
        }).start();
    }

    public void learnWithGraphData() {

        tempClasses = new ArrayList<XYChart.Series>();

        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        for (int i = 0; i < outputsCount; i++)
            tempClasses.add(new XYChart.Series());
        System.out.println("Temp size is: " + tempClasses.size());

        XYChart.Series tempUnidentifiedSeries = new XYChart.Series();

        //Learning data
        System.out.println("Inputs counts is: " + inputsCount);
        inputData = new double[inputsCount][2];
        outputData = new double[inputsCount][neuralNetwork.outputNeuronNumber];

        if (neuralNetwork.outputNeuronNumber == 1) {
            for (int i = 0; i < inputsCount; i++) {
                ObservableList<XYChart.Data> classASeries = classes.get(0).getData();
                ObservableList<XYChart.Data> classBSeries = classes.get(1).getData();
                //Class A data
                if (i < classASeries.size()) {
                    inputData[i][0] = (double) classASeries.get(i).getXValue();
                    inputData[i][1] = (double) classASeries.get(i).getYValue();
                    outputData[i][0] = 0;
                }
                //Class B data
                else {
                    inputData[i][0] = (double) classBSeries.get(i - classASeries.size()).getXValue();
                    inputData[i][1] = (double) classBSeries.get(i - classASeries.size()).getYValue();
                    outputData[i][0] = 1;
                }
            }
            tempUnidentifiedSeries.getData().addAll(classes.get(0).getData());
            tempUnidentifiedSeries.getData().addAll(classes.get(1).getData());
            classes.get(0).getData().clear();
            classes.get(1).getData().clear();
        } else {
            int k = 0;

            for (int i = 0; i < classes.size(); i++) {
                ObservableList<XYChart.Data> currentSeries = classes.get(i).getData();
                for (int j = 0; j < currentSeries.size(); j++) {
                    inputData[k][0] = (double) currentSeries.get(j).getXValue();
                    inputData[k][1] = (double) currentSeries.get(j).getYValue();
                    outputData[k][i] = 1;
                    k++;
                }
                tempUnidentifiedSeries.getData().addAll(classes.get(i).getData());
                classes.get(i).getData().clear();
            }
        }

        unIdentifiedData.getData().setAll(tempUnidentifiedSeries.getData());

        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(inputData, outputData, () -> Platform.runLater(() -> {
                        MSE.setText(String.format("Current MSE: %.5f", neuralNetwork.currentMES));
                        currentEpochLbl.setText(String.format("Current Epoch: %s", neuralNetwork.currentEpoch));
                        performanceData.getData().add(new XYChart.Data(neuralNetwork.currentEpoch, neuralNetwork.currentMES));
                    }), () -> Platform.runLater(() -> {
                        //This function shows final classification on the GUI

                        unIdentifiedData.getData().clear();
                        for (int i = 0; i < inputsCount; i++) {
                            double[] predictedOutputs = neuralNetwork.predict(inputData[i]);
                            XYChart.Data point = new XYChart.Data(inputData[i][0], inputData[i][1]);

                            //Perceptron
                            if (neuralNetwork.outputNeuronNumber == 1) {
                                double output = predictedOutputs[0];
                                if (output < 0.5) {
                                    classes.get(0).getData().add(point);
                                } else {
                                    classes.get(1).getData().add(point);
                                }
                            }
                            //Multiple output nodes
                            else {
                                int maxIndex = 0;
                                double maxValue = Double.MIN_VALUE;
                                for (int j = 0; j < predictedOutputs.length; j++) {
                                    double currentValue = predictedOutputs[j];
                                    if (currentValue > maxValue) {
                                        maxValue = currentValue;
                                        maxIndex = j;
                                    }
                                }
                                classes.get(maxIndex).getData().add(point);
                            }
                        }
                    })
            );
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            learningStarted = false;
            System.gc();
        }).start();

    }

    public static class TableData {
        private SimpleStringProperty className;
        private int predictedClassA;
        private int predictedClassB;

        public TableData(SimpleStringProperty className, int predictedClassA, int predictedClassB) {
            this.className = className;
            this.predictedClassA = predictedClassA;
            this.predictedClassB = predictedClassB;
        }

        public String getClassName() {
            return className.get();
        }

        public SimpleStringProperty classNameProperty() {
            return className;
        }

        public void setClassName(String className) {
            this.className.set(className);
        }

        public int getPredictedClassA() {
            return predictedClassA;
        }

        public void setPredictedClassA(int predictedClassA) {
            this.predictedClassA = predictedClassA;
        }

        public int getPredictedClassB() {
            return predictedClassB;
        }

        public void setPredictedClassB(int predictedClassB) {
            this.predictedClassB = predictedClassB;
        }
    }

    class ClassItem {
        public String className;
        public String symbolPath;
        public Image classSymbol;
        public int index;

        public ClassItem(String className, String symbolPath, int index) {
            this.className = className;
            this.symbolPath = symbolPath;
            this.index = index;
            File file = new File(symbolPath);
            try {
                this.classSymbol = new Image(file.toURI().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dataFromGraph = true;

        unIdentifiedData = new XYChart.Series();
        chart.getData().add(unIdentifiedData);

        performanceData = new XYChart.Series();
        performanceChart.getData().add(performanceData);

        fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a JSON file te parse the data from");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON file", "*.json"));
        fileChooser.setInitialDirectory(new File(""));
        classSelection.setCellFactory(lv -> new ListCell<ClassItem>() {
            @Override
            protected void updateItem(ClassItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    if (item.classSymbol != null) {
                        ImageView imageView = new ImageView(item.classSymbol);
                        imageView.setFitHeight(20);
                        imageView.setFitWidth(20);
                        setGraphic(imageView);
                    }
                    setText(item.className);
                }


            }
        });
        classSelection.setButtonCell(new ListCell<ClassItem>() {
            @Override
            protected void updateItem(ClassItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    if (item.classSymbol != null) {
                        ImageView imageView = new ImageView(item.classSymbol);
                        imageView.setFitHeight(20);
                        imageView.setFitWidth(20);
                        setGraphic(imageView);
                    }
                    setText(item.className);
                }
            }
        });
    }

    public void addPoint(MouseEvent mouseEvent) {
//        System.out.println("X axis is :" + mouseEvent.getX());
//        System.out.println("Y axis is :" + mouseEvent.getY());
//        System.out.println("Chart width is :" + chart.getPrefWidth());
//        System.out.println("Chart height is :" + chart.getPrefHeight());

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            ClassItem currentItem = (ClassItem) classSelection.getValue();
            int index = currentItem.index;
            classes.get(index).getData().add(new XYChart.Data((mouseEvent.getX() - 36.6) / xAxis.getScale(), (mouseEvent.getY() - 353) / yAxis.getScale()));
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY)
            classes.get(1).getData().add(new XYChart.Data((mouseEvent.getX() - 36.6) / xAxis.getScale(), (mouseEvent.getY() - 353) / yAxis.getScale()));
        inputsCount++;
    }


}
