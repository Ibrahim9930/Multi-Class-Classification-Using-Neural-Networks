package org.uousef.project.ai;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.uousef.project.ai.modules.ActivationFunction;
import org.uousef.project.ai.modules.LayerInformation;
import org.uousef.project.ai.modules.NeuralNetwork;

public class PrimaryController implements Initializable {
    NeuralNetwork neuralNetwork;
    @FXML
    private JFXButton jfButton;

    String str(double[][] te)
    {
        String toReturn ="";
        for (int i =0 ; i< te.length;i++)
        {
            for (int j =0; j<te[i].length;j++)
            {
                toReturn+=", "+te[i][j];
            }
            toReturn+="\n";
        }
        return toReturn;
    }
    @FXML
    void print(ActionEvent event) {
        System.out.println("hhh");
        System.out.println(str(neuralNetwork.nodeOutputs));
        System.out.println("hlllll");
        neuralNetwork.feeding(new double[]{1.0, 2.0});
        System.out.println("done feeding");
        System.out.println(neuralNetwork.toString());
        System.out.println("output");
        System.out.println(str(neuralNetwork.nodeOutputs));
    }
    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ArrayList<LayerInformation> layerInformations = new ArrayList<LayerInformation>();
        layerInformations.add(new LayerInformation(1, ActivationFunction.LINEAR));
        layerInformations.add(new LayerInformation(3, ActivationFunction.ReLU));
        layerInformations.add(new LayerInformation(4, ActivationFunction.TANH));
        neuralNetwork = new NeuralNetwork(layerInformations,0.1,0.1,2);
        System.out.println("done inti");
        System.out.println(neuralNetwork.toString());
    }
}
