package org.uousef.project.ai;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.uousef.project.ai.modules.NeuralNetwork;

public class SecondaryController implements Initializable {

    public Label currentEpochLbl;
    public LineChart performanceChart;
    private double[][] inputData, outputData;
    public ScatterChart chart;
    public NumberAxis xAxis;
    public NumberAxis yAxis;
    private NeuralNetwork neuralNetwork;
    private volatile boolean learningStarted;
    private XYChart.Series unIdentifiedData;
    private XYChart.Series classAData;
    private XYChart.Series classBData;
    private XYChart.Series performanceData;
    private XYChart.Series tempClassASeries, tempClassBSeries;
    private FileChooser fileChooser;
    private boolean dataFromGraph;
    private String fileDataJson;

    void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
        this.learningStarted = false;
    }

    @FXML
    private Button start, butttton;

    @FXML
    private Label MSE;
    @FXML
    private Pane confusionPane;

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

            String line;
            while ((line = in.readLine()) != null) {
                fileDataJson += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        dataFromGraph = false;
    }

    private void learnWithFileData() {

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
                tempValue = (Long) tempInputArray.get(j);
                inputData[i][j] = tempValue.doubleValue();
            }

            tempOutputArray = (JSONArray) tempObject.get("output");
            for (int j = 0; j < tempOutputArray.size(); j++) {
                tempValue = (Long) tempOutputArray.get(j);
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
                    }), (i) -> {
                    }, () -> {
                    }, () -> {
                    }
            );
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            learningStarted = false;
            System.gc();
        }).start();
    }

    private void learnWithGraphData() {

        //Stores new values for class A and class B before showing them on gui
        tempClassASeries = new XYChart.Series();
        tempClassBSeries = new XYChart.Series();

        ObservableList<XYChart.Data> classAInput = classAData.getData();
        ObservableList<XYChart.Data> classBInput = classBData.getData();

        XYChart.Series tempUnidentifiedSeries = new XYChart.Series();

        //Learning data
        inputData = new double[classAInput.size() + classBInput.size()][2];
        outputData = new double[classAInput.size() + classBInput.size()][1];

        //One loop for class A and class B data
        for (int i = 0; i < classAInput.size() + classBInput.size(); i++) {
            //Class A data
            if (i < classAInput.size()) {
                inputData[i][0] = (double) classAInput.get(i).getXValue();
                inputData[i][1] = (double) classAInput.get(i).getYValue();
                outputData[i][0] = 0;
            }
            //Class B data
            else {
                inputData[i][0] = (double) classBInput.get(i - classAInput.size()).getXValue();
                inputData[i][1] = (double) classBInput.get(i - classAInput.size()).getYValue();
                outputData[i][0] = 1;
            }

        }
        //Convert class A and class B shapes into unidentified shapes
        tempUnidentifiedSeries.getData().addAll(classAInput);
        tempUnidentifiedSeries.getData().addAll(classBInput);
        classBData.getData().clear();
        classAData.getData().clear();

        unIdentifiedData.getData().setAll(tempUnidentifiedSeries.getData());

        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(inputData, outputData, () -> Platform.runLater(() -> {
                        MSE.setText(String.format("Current MSE: %.5f", neuralNetwork.currentMES));
                        currentEpochLbl.setText(String.format("Current Epoch: %s", neuralNetwork.currentEpoch));
                        performanceData.getData().add(new XYChart.Data(neuralNetwork.currentEpoch, neuralNetwork.currentMES));
                    }), (i) -> {
                        //This function classifies the input on run time but doesn't display them on the GUI

                        double output = neuralNetwork.nodeOutputs[neuralNetwork.nodeOutputs.length - 1][0];
                        XYChart.Data point = new XYChart.Data(inputData[i][0], inputData[i][1]);
                        if (output < 0.5) {
                            //If statement not necessary, just to avoid duplicate exceptions
                            if (!tempClassASeries.getData().contains(point)) {
                                tempClassASeries.getData().add(point);
                            }

                        } else if (!tempClassBSeries.getData().contains(point)) {
                            if (!tempClassBSeries.getData().contains(point)) {
                                tempClassBSeries.getData().add(point);
                            }
                        }

                    }, () -> Platform.runLater(() -> {
                        //This function shows final classification on the GUI

                        unIdentifiedData.getData().clear();
                        classAData.getData().addAll(tempClassASeries.getData());
                        classBData.getData().addAll(tempClassBSeries.getData());
                    }), () -> {
                        //Clears series before each epoch
                        tempClassASeries.getData().clear();
                        tempClassBSeries.getData().clear();
                    }
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dataFromGraph = true;
        unIdentifiedData = new XYChart.Series();
//        unIdentifiedData.setName("Unidentified");
        classAData = new XYChart.Series();
//        classAData.setName("Class A");
        classBData = new XYChart.Series();
//        classBData.setName("classB");
        chart.getData().add(unIdentifiedData);
        chart.getData().add(classAData);
        chart.getData().add(classBData);

        performanceData = new XYChart.Series();
        performanceChart.getData().add(performanceData);
        ObservableList<TableData> tableData =
                FXCollections.observableArrayList(
                        new TableData(new SimpleStringProperty("Class A"), 0, 0),
                        new TableData(new SimpleStringProperty("Class B"), 0, 0)
                );

        TableColumn<TableData, String> headerCol = new TableColumn<>("Actual/Predicted");
        headerCol.setCellValueFactory(
                new PropertyValueFactory<TableData, String>("className")
        );
        headerCol.setStyle("-fx-pref-width: 120px;");
        TableColumn classACol = new TableColumn("Class A");
//        classACol.setCellValueFactory(
//                new PropertyValueFactory<TableData, Integer>("predictedClassA")
//        );
        TableColumn classBCol = new TableColumn("Class B");
//        classBCol.setCellValueFactory(
//                new PropertyValueFactory<TableData, Integer>("predictedClassB")
//        );

        fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a JSON file te parse the data from");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("JSON file", "*.json"));
    }

    public void addPoint(MouseEvent mouseEvent) {
        if (mouseEvent.getButton() == MouseButton.PRIMARY)
            classAData.getData().add(new XYChart.Data((mouseEvent.getX() - 36.6) / xAxis.getScale(), (mouseEvent.getY() - 353) / yAxis.getScale()));
        else if (mouseEvent.getButton() == MouseButton.SECONDARY)
            classBData.getData().add(new XYChart.Data((mouseEvent.getX() - 36.6) / xAxis.getScale(), (mouseEvent.getY() - 353) / yAxis.getScale()));
    }

    @FXML
    void ssss(ActionEvent event) {
        double width = confusionPane.getWidth(), height = confusionPane.getHeight();
        confusionPane.getChildren().removeIf((e) -> true);
        double[] name = {1, 2,3};
        int[] ac = {60, 105,100};
        int[][] con =
                {
                        {50, 10,5},
                        {5, 100,2},
                        {50, 90,18},
                };
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("confusionMatrix.fxml"));
        Parent newLoadedPane = null;

        try {
            newLoadedPane = loader.load();
            ConfusionMatrixController matrix = loader.getController();
            System.out.println(matrix);
            matrix.setupMatrix(name, con, ac,width,height);
            confusionPane.getChildren().add(newLoadedPane);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
