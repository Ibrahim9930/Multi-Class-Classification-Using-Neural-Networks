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
    private static final String[] symbolPathes = new String[]{
            "src/main/resources/org/uousef/project/ai/symbol_images/classa.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classb.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classc.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classd.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classe.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classf.PNG",
            "src/main/resources/org/uousef/project/ai/symbol_images/classg.PNG",
    };
    @FXML
    private LineChart performanceChart;
    @FXML
    private ComboBox classSelection;
    @FXML
    private Text trainText;
    @FXML
    private ScatterChart chart;
    @FXML
    private NumberAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private FileChooser fileChooser;
    @FXML
    private Label MSE, currentEpochLbl;
    @FXML
    private Pane confusionPane;

    private NeuralNetwork neuralNetwork;
    private double[][] inputData, outputData;
    private int[] actualOutputsIndecies;
    private volatile boolean startedLearning;
    private int inputsCount;
    private boolean doneTraining;

    private String fileDataJson;
    private boolean dataFromGraph;
    private ArrayList<XYChart.Series> classes, temps;
    private XYChart.Series unIdentifiedData, performanceData;

    private class ClassItem {
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
        if (neuralNetwork.inputNeuronNumber != 2 || neuralNetwork.outputNeuronNumber > 7) {
            chart.setVisible(false);
            classSelection.setVisible(false);
            dataFromGraph = false;
            trainText.setText("Upload the training data set using a JSON file");
            return;
        }

        //Initialize points classes
        classes = new ArrayList<XYChart.Series>();
        temps = new ArrayList<XYChart.Series>();
        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        for (int i = 0; i < outputsCount; i++) {
            classes.add(new XYChart.Series());
            temps.add(new XYChart.Series());
            chart.getData().add(classes.get(i));
            char classChar = 'A';
            classChar += i;
            classSelection.getItems().add(new ClassItem("Class" + classChar, symbolPathes[i], i));
        }

        //Initial value for the class selection is class 1(square)
        classSelection.setValue(classSelection.getItems().get(0));
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

    @FXML
    private void enterFileData(ActionEvent actionEvent) {

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
            //Used to reduce amount of loops required when determining the actual outputs
            actualOutputsIndecies = new int[inputsCount];

            int iterationIndex = 0;

            for (int classIndex = 0; classIndex < classes.size(); classIndex++) {
                ObservableList<XYChart.Data> currentSeries = classes.get(classIndex).getData();
                for (int j = 0; j < currentSeries.size(); j++) {
                    inputData[iterationIndex][0] = (double) currentSeries.get(j).getXValue();
                    inputData[iterationIndex][1] = (double) currentSeries.get(j).getYValue();
                    outputData[iterationIndex][classIndex] = 1;
                    actualOutputsIndecies[iterationIndex] = classIndex;
                    iterationIndex++;
                }
                tempUnidentifiedSeries.getData().addAll(classes.get(classIndex).getData());
                classes.get(classIndex).getData().clear();
            }
        }

        unIdentifiedData.getData().setAll(tempUnidentifiedSeries.getData());
        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(inputData, outputData, () -> Platform.runLater(() -> {
                MSE.setText(String.format("Current MSE: %.5f", neuralNetwork.currentMSE));
                currentEpochLbl.setText(String.format("Current Epoch: %s", neuralNetwork.currentEpoch));
                performanceData.getData().add(new XYChart.Data(neuralNetwork.currentEpoch, neuralNetwork.currentMSE));
            }), () -> {

                //This function shows final classification on the GUI
                int[][] confusionMatrix = new int[outputsCount][];
                for (int i = 0; i < confusionMatrix.length; i++)
                    confusionMatrix[i] = new int[outputsCount];
                int row;
                int col;

                int[] classesCount = new int[outputsCount];


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
                            temps.get(0).getData().add(point);
                            col = 0;
                            classesCount[0]++;
                        } else {
                            temps.get(1).getData().add(point);
                            col = 1;
                            classesCount[1]++;
                        }
                        if (actualOutput < 0.5) {
                            row = 0;
                        } else {
                            row = 1;
                        }
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
                        temps.get(maxIndex).getData().add(point);
                        col = maxIndex;

                        row = actualOutputsIndecies[i];
                        classesCount[col]++;
                    }
                    confusionMatrix[row][col]++;
                }
                Platform.runLater(() -> {
                    updateScatterPlotAndMatrix(confusionMatrix, classesCount, true);
                });
                doneTraining = true;
            });
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            startedLearning = false;
            System.gc();
        }).start();

    }

    private void learnWithFileData() {

        Object obj = JSONValue.parse(fileDataJson);
        JSONArray data = (JSONArray) obj;

        JSONObject tempObject = (JSONObject) data.get(0);
        JSONArray tempInputArray = (JSONArray) tempObject.get("input");
        JSONArray tempOutputArray = (JSONArray) tempObject.get("output");
        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        if (tempInputArray.size() != neuralNetwork.inputNeuronNumber || tempOutputArray.size() != neuralNetwork.outputNeuronNumber) {
            return;
        }
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

        long t = System.currentTimeMillis();
        new Thread(() -> {
            neuralNetwork.training(inputData, outputData, () -> Platform.runLater(() -> {
                MSE.setText(String.format("Current MSE: %.5f", neuralNetwork.currentMSE));
                currentEpochLbl.setText(String.format("Current Epoch: %s", neuralNetwork.currentEpoch));
                performanceData.getData().add(new XYChart.Data(neuralNetwork.currentEpoch, neuralNetwork.currentMSE));
            }), () -> {

                //This function shows final classification on the GUI
                int[][] confusionMatrix = new int[outputsCount][];
                for (int i = 0; i < confusionMatrix.length; i++)
                    confusionMatrix[i] = new int[outputsCount];
                int row;
                int col;

                int[] classesCount = new int[outputsCount];

                for (int i = 0; i < data.size(); i++) {
                    double[] predictedOutputs = neuralNetwork.predict(inputData[i]);

                    row = -1;
                    col = -1;

                    //Perceptron
                    if (neuralNetwork.outputNeuronNumber == 1) {
                        double predictedOutput = predictedOutputs[0];
                        double actualOutput = outputData[i][0];
                        col = predictedOutput < 0.5 ? 0 : 1;
                        if (actualOutput < 0.5) {
                            row = 0;
                            classesCount[0]++;
                        } else {
                            row = 1;
                            classesCount[1]++;
                        }
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
                        col = maxIndex;

                        row = actualOutputsIndecies[i];
                        classesCount[col]++;
                    }
                    confusionMatrix[row][col]++;
                }

                Platform.runLater(() -> updateScatterPlotAndMatrix(confusionMatrix, classesCount, false));
            });
            System.out.println("Time the operation took in milliseconds : " + (System.currentTimeMillis() - t));
            startedLearning = false;
            System.gc();
        }).start();
    }

    public void addPoint(MouseEvent mouseEvent) {
        if (!dataFromGraph)
            return;
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            ClassItem currentItem = (ClassItem) classSelection.getValue();
            int index = currentItem.index;
            classes.get(index).getData().add(new XYChart.Data((mouseEvent.getX() - 36) / xAxis.getScale(), (mouseEvent.getY() - 310) / yAxis.getScale()));
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY)
            classes.get(1).getData().add(new XYChart.Data((mouseEvent.getX() - 36) / xAxis.getScale(), (mouseEvent.getY() - 310) / yAxis.getScale()));
        inputsCount++;
    }

    private void updateScatterPlotAndMatrix(int[][] confusionMatrix, int[] classesCount, boolean showGraph) {
        int outputsCount = neuralNetwork.outputNeuronNumber == 1 ? 2 : neuralNetwork.outputNeuronNumber;
        if (showGraph) {
            unIdentifiedData.getData().clear();
            for (int i = 0; i < temps.size(); i++)
                classes.get(i).getData().addAll(temps.get(i).getData());
        }
        double width = confusionPane.getWidth(), height = confusionPane.getHeight();
        confusionPane.getChildren().removeIf((e) -> true);
        double[] name = new double[outputsCount];
        for (int i = 0; i < name.length; i++) {
            name[i] = i;
        }

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource("confusionMatrix.fxml"));
        Parent newLoadedPane = null;

        try {
            newLoadedPane = loader.load();
            ConfusionMatrixController matrix = loader.getController();
            matrix.setupMatrix(name, confusionMatrix, classesCount, width, height);
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
