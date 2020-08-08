package org.uousef.project.ai;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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
    public Text trainText;
    public ScatterChart chart;
    public NumberAxis xAxis;
    public NumberAxis yAxis;
    private FileChooser fileChooser;

    private String fileDataJson;

    private NeuralNetwork neuralNetwork;
    private int inputsCount;
    private volatile boolean startedLearning;
    private boolean doneTraining;

    private double[][] inputData, outputData;
    private ArrayList<XYChart.Series> classes;
    private XYChart.Series unIdentifiedData;
    private boolean dataFromGraph;

    private XYChart.Series performanceData;

    @FXML
    private Label MSE;
    @FXML
    private Pane confusionPane;
    @FXML
    private JFXButton back;

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
//        fileChooser.setInitialDirectory(new File(""));

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

    void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
        this.startedLearning = false;
        this.doneTraining = false;

        // Can't enter data from graph
        if(neuralNetwork.inputNeuronNumber != 2){
            chart.setVisible(false);
            classSelection.setVisible(false);
            dataFromGraph = false;
            trainText.setText("Upload the training data set using a JSON file");
            return;
        }

        //Initialize points classes
        classes = new ArrayList<XYChart.Series>();
        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        for (int i = 0; i < outputsCount; i++) {
            classes.add(new XYChart.Series());
            chart.getData().add(classes.get(i));
            char classChar = 'A';
            classChar += i;
            classSelection.getItems().add(new ClassItem("Class" + classChar, symbolPathes[i], i));
        }

        //Initial value for the class selection is class 1(square)
        classSelection.setValue(classSelection.getItems().get(0));
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
        chart.setOpacity(0.5);
        classSelection.setDisable(true);
    }

    public void startTraining(ActionEvent actionEvent) {
        if (!startedLearning) {
            startedLearning = true;
            if (dataFromGraph)
                learnWithGraphData();
            else
                learnWithFileData();
        }
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
//        for (int i = 0; i < inputData.length; i++) {
//            System.out.println();
//            for (int j = 0; j < inputData[i].length; j++)
//                System.out.print(inputData[i][j] + "\t");
//        }
//        for (int i = 0; i < outputData.length; i++) {
//            System.out.println();
//            for (int j = 0; j < outputData[i].length; j++)
//                System.out.print(outputData[i][j] + "\t");
//        }
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
            startedLearning = false;
            System.gc();
        }).start();
    }

    private void learnWithGraphData() {

        XYChart.Series tempUnidentifiedSeries = new XYChart.Series();

        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;

        //Learning data
        inputData = new double[inputsCount][2];
        outputData = new double[inputsCount][neuralNetwork.outputNeuronNumber];

        //Perceptron
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
                        int[][] confusionMatrix = new int[outputsCount][];
                        for (int i = 0; i < confusionMatrix.length; i++)
                            confusionMatrix[i] = new int[outputsCount];
                        int row;
                        int col;
                        int[] classesCount = new int[outputsCount];
                        unIdentifiedData.getData().clear();

                        for (int i = 0; i < inputsCount; i++) {
                            double[] predictedOutputs = neuralNetwork.predict(inputData[i]);
                            XYChart.Data point = new XYChart.Data(inputData[i][0], inputData[i][1]);

                            row = -1;
                            col = -1;
                            //Perceptron
                            if (neuralNetwork.outputNeuronNumber == 1) {
                                double predictedOutput = predictedOutputs[0];
                                double actualOutput = outputData[i][0];

                                if (predictedOutput < 0.5) {
                                    classes.get(0).getData().add(point);
                                    col = 0;
                                } else {
                                    classes.get(1).getData().add(point);
                                    col = 1;
                                }
                                if (actualOutput < 0.5) {
                                    row = 0;
                                    classesCount[0]++;
                                } else {
                                    row = 1;
                                    classesCount[1]++;
                                }
                                confusionMatrix[row][col]++;
                            }
                            //Multiple output nodes
                            else {
                                int maxIndex = -1;
                                double maxValue = Double.MIN_VALUE;
                                for (int j = 0; j < predictedOutputs.length; j++) {
                                    double currentValue = predictedOutputs[j];
                                    if (currentValue > maxValue) {
                                        maxValue = currentValue;
                                        maxIndex = j;
                                    }
                                }
                                classes.get(maxIndex).getData().add(point);
                                col = maxIndex;
                                maxIndex = -1;
                                maxValue = Double.MIN_VALUE;
                                for (int j = 0; j < outputData[i].length; j++) {
                                    double currentValue = outputData[i][j];
                                    if (currentValue > maxValue) {
                                        maxValue = currentValue;
                                        maxIndex = j;
                                    }
                                }
                                row = maxIndex;
                                classesCount[maxIndex]++;
                                confusionMatrix[row][col]++;
                            }

                        }
                        double width = confusionPane.getWidth(), height = confusionPane.getHeight();
                        confusionPane.getChildren().removeIf((e) -> true);
                        double[] name = new double[outputsCount];
                        for (int i=0;i< name.length;i++)
                        {
                            name[i] = i;
                        }

                        FXMLLoader loader = new FXMLLoader();
                        loader.setLocation(App.class.getResource("confusionMatrix.fxml"));
                        Parent newLoadedPane = null;

                        try {
                            newLoadedPane = loader.load();
                            ConfusionMatrixController matrix = loader.getController();
//                            System.out.println(matrix);
                            matrix.setupMatrix(name, confusionMatrix, classesCount, width, height);
                            confusionPane.getChildren().add(newLoadedPane);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        doneTraining = true;
                    })
            );
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            startedLearning = false;
            System.gc();
        }).start();

    }


    public void addPoint(MouseEvent mouseEvent) {
        if(!dataFromGraph)
            return;
//        System.out.println("X axis is :" + mouseEvent.getX());
//        System.out.println("Y axis is :" + mouseEvent.getY());
//        System.out.println("Chart width is :" + chart.getPrefWidth());
//        System.out.println("Chart height is :" + chart.getPrefHeight());

        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            ClassItem currentItem = (ClassItem) classSelection.getValue();
            int index = currentItem.index;
            classes.get(index).getData().add(new XYChart.Data((mouseEvent.getX() - 36) / xAxis.getScale(), (mouseEvent.getY() - 281) / yAxis.getScale()));
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY)
            classes.get(1).getData().add(new XYChart.Data((mouseEvent.getX() - 36) / xAxis.getScale(), (mouseEvent.getY() - 281) / yAxis.getScale()));
        inputsCount++;
    }

    @FXML
    void ssss(ActionEvent event) {
        double width = confusionPane.getWidth(), height = confusionPane.getHeight();
        confusionPane.getChildren().removeIf((e) -> true);
        double[] name = {1, 2, 3};
        int[] ac = {60, 105, 100};
        int[][] con =
                {
                        {50, 10, 5},
                        {5, 100, 2},
                        {50, 90, 18},
                };
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("confusionMatrix.fxml"));
        Parent newLoadedPane = null;

        try {
            newLoadedPane = loader.load();
            ConfusionMatrixController matrix = loader.getController();
            System.out.println(matrix);
            matrix.setupMatrix(name, con, ac, width, height);
            confusionPane.getChildren().add(newLoadedPane);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void backHomePage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(App.class.getResource("primary.fxml"));
            Parent secondaryParent = loader.load();
            Scene SecondaryScene = new Scene(secondaryParent);

            PrimaryController controller = loader.getController();
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(SecondaryScene);
            System.gc();
            window.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
