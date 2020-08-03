package main.java.sample;

import java.util.Random;
import java.util.Vector;

public class NeuralNetwork {
    float[][][] weights;
    float[][][] weightCorrections;
    float[][] nodeOutputs;
    int layerCount;
    int layerNodeCount[];
    int inputCount;
    int outputCount;
    float MSN;
    float learningRate;

    public NeuralNetwork(int layerCount, int[] layerNodeCount, int inputCount, int outputCount, float learningRate) {
        this.inputCount = inputCount;
        this.outputCount = outputCount;
        this.layerCount = layerCount;
        this.layerNodeCount = layerNodeCount;
        this.learningRate = learningRate;
        initializeWeightsAndOutputs();
    }

    private void initializeWeightsAndOutputs() {
        // Random number generator
        Random rand = new Random();

        // Number of layers
        weights = new float[layerCount][][];
        weightCorrections = new float[layerCount][][];
        nodeOutputs = new float[layerCount][];

        // Initialize layers
        for (int i = 0; i < layerCount; i++) {
            // Number of nodes
            weights[i] = new float[layerNodeCount[i]][];
            weightCorrections[i] = new float[layerNodeCount[i]][];
            nodeOutputs[i] = new float[layerNodeCount[i]];

            // Initialize nodes
            for (int j = 0; j < layerNodeCount[i]; j++) {
                // Layers other than the first layer
                if (i != 0) {
                    //Number of inputs
                    weights[i][j] = new float[layerNodeCount[i - 1]];
                    weightCorrections[i][j] = new float[layerNodeCount[i - 1]];

                    // Initialize inputs
                    for (int l = 0; l < layerNodeCount[i - 1]; l++) {
                        weights[i][j][l] = (float) ((rand.nextFloat() * 4.8 - 2.4) / layerNodeCount[i - 1]);
                        weightCorrections[i][j][l] = 0;
                    }
                }
                // First layer is connected with the input nodes
                else {
                    weights[i][j] = new float[inputCount];
                    for (int l = 0; l < inputCount; l++) {
                        weights[i][j][l] = (float) ((rand.nextFloat() * 4.8 - 2.4) / inputCount);
                        weightCorrections[i][j][l] = 0;
                    }
                }
            }
        }
    }

    public void backPropagate(float finalOutputs[], float inputs[]) {
        Vector gradients = new Vector();
        // Layers loop
        for (int i = weights.length - 1; i >= 0; i--) {

            // Nodes loop
            for (int j = 0; j < weights[i].length; j++) {
                float gradientFactor = 0;

                if (i != weights.length - 1) {
                    for (int k = 0; k < weights[i + 1][j].length; k++) {
                        gradientFactor += weights[i + 1][k][j] * (float) gradients.get(k);
                    }
                } else
                    gradientFactor = finalOutputs[j] - nodeOutputs[i][j];
                // Calculate gradient for that node and store it
                gradients.insertElementAt(nodeOutputs[i][j] *(1-nodeOutputs[i][j])* gradientFactor, j);

                // Weights loop(finds the weights' correction values
                for (int k = 0; k < weights[i][j].length; k++) {
                    if (i != 0)
                        weightCorrections[i][j][k] = learningRate * (float) gradients.get(j) * nodeOutputs[i - 1][k];
                    else
                        weightCorrections[i][j][k] = learningRate * (float) gradients.get(j) * inputs[k];
                }
            }
        }
    }

    public static void main(String[] args) {
        int layerNodes[] = new int[]{2, 3, 1};
        NeuralNetwork nn = new NeuralNetwork(3, layerNodes, 2, 1, (float) 0.01);
        for (int i = 0; i < nn.weights.length; i++)
            for (int j = 0; j < nn.weights[i].length; j++)
                for (int l = 0; l < nn.weights[i][j].length; l++) {
                    System.out.printf("Layer number: %d\tNode number: %d\tInput number: %d with the value %f\n", i, j, l, nn.weights[i][j][l]);
                }
    }
}
