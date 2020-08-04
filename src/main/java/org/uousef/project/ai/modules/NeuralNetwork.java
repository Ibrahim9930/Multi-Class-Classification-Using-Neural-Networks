package org.uousef.project.ai.modules;
import java.util.*;
import java.util.stream.DoubleStream;

public class NeuralNetwork {

    public double[][][] weights, weightCorrections;
    public double[][] thresholds, nodeOutputs;
    public double learningRate, acceptedMSE, currentMES;
    public int inputNeuronNumber;
    //Contains weights of hidden layers and outputs
    ArrayList<LayerInformation> layersInformation;

    public NeuralNetwork(ArrayList<LayerInformation> layersInformation, double learningRate, double acceptedMSE, int inputNeuronNumber) {
        this.learningRate = learningRate;
        this.acceptedMSE = acceptedMSE;
        this.inputNeuronNumber = inputNeuronNumber;
        this.layersInformation = layersInformation;

        initializeNeuralNetwork();
    }

    public void initializeNeuralNetwork() {

        Random randomGenerator = new Random();
        int index, nestedIndex, previousNeuronNum = 0, temp = layersInformation.size();

        weights = new double[temp][][];
        weightCorrections = new double[temp][][];
        nodeOutputs = new double[temp][];
        thresholds = new double[temp][];

        for (index = 0; index < layersInformation.size(); index++) {
            temp = layersInformation.get(index).neuronNumber;

            weights[index] = new double[temp][];
            weightCorrections[index] = new double[temp][];

            nodeOutputs[index] = new double[temp];
            thresholds[index] = new double[temp];

            for (nestedIndex = 0; nestedIndex < weights[index].length; nestedIndex++) {
                previousNeuronNum = (index == 0) ? inputNeuronNumber : weights[index - 1].length;

                weights[index][nestedIndex] = randomGenerator.doubles(previousNeuronNum, (-2.4 / (double) previousNeuronNum), (+2.4 / (double) previousNeuronNum)).toArray();
                thresholds[index][nestedIndex] = (randomGenerator.nextDouble() * 4.8 - 2.4) / previousNeuronNum;

                weightCorrections[index][nestedIndex] = new double[previousNeuronNum];
                Arrays.fill(weightCorrections[index][nestedIndex], 0.0);
            }

        }
    }

    public boolean feeding(double[] inputData) {
        if (inputData.length != inputNeuronNumber) return false;
        int indexLayer, indexNeuron, indexWeight;
        double tempValue, expSum;

        for (indexLayer = 0; indexLayer < weights.length; indexLayer++) {
            for (indexNeuron = 0; indexNeuron < weights[indexLayer].length; indexNeuron++) {

                tempValue = 0;
                for (indexWeight = 0; indexWeight < weights[indexLayer][indexNeuron].length; indexWeight++) {
                    if (indexLayer == 0) //first hidden layer
                    {
                        tempValue = weights[indexLayer][indexNeuron][indexWeight] * inputData[indexWeight];
                    } else //output and all hidden layers except first one
                    {
                        tempValue = weights[indexLayer][indexNeuron][indexWeight] * nodeOutputs[indexLayer - 1][indexWeight];
                    }
                }
                tempValue -= thresholds[indexLayer][indexNeuron];
                nodeOutputs[indexLayer][indexNeuron] = computeNeuronOutput(tempValue, indexLayer);
            }
            //check if its the output layer and use softmax
            if (false&&indexLayer + 1 == weights.length && layersInformation.get(indexLayer).activationFunction == ActivationFunction.SOFTMAX) {
                expSum = DoubleStream.of(nodeOutputs[indexLayer]).sum();
                for (indexNeuron = 0; indexNeuron < weights[indexLayer].length; indexNeuron++) {
                    nodeOutputs[indexLayer][indexNeuron] /= expSum;
                }
            }
        }
        return true;
    }

    public void backPropagate(double finalOutputs[], double inputs[]) {
        Vector oldGradients = new Vector();
        Vector newGradients = new Vector();
        // Layers loop
        for (int i = weights.length - 1; i >= 0; i--) {

            // Nodes loop
            for (int j = 0; j < weights[i].length; j++) {
                double gradientFactor = 0;
                double gradientValue = computeDerivative(0, nodeOutputs[i][j], i);
                if (i != weights.length - 1) {

                    for (int k = 0; k < weights[i + 1].length; k++) {
                        gradientFactor += weights[i + 1][k][j] * (double) oldGradients.get(k);
                    }
                } else {
                    gradientFactor = finalOutputs[j] - nodeOutputs[i][j];
                }
                // Calculate gradient for that node and store it
                newGradients.insertElementAt(gradientValue * gradientFactor, j);

                // Weights loop(finds the weights' correction values
                for (int k = 0; k < weights[i][j].length; k++) {
                    if (i != 0)
                        weightCorrections[i][j][k] = learningRate * (double) newGradients.get(j) * nodeOutputs[i - 1][k];
                    else
                        weightCorrections[i][j][k] = learningRate * (double) newGradients.get(j) * inputs[k];
                }
            }
            oldGradients.clear();
            oldGradients.addAll(newGradients);
//            Collections.copy(oldGradients, newGradients);
        }

        // Update weights
        for (int i = 0; i < weights.length; i++)
            for (int j = 0; j < weights[i].length; j++)
                for (int k = 0; k < weights[i][j].length; k++)
                    weights[i][j][k] += weightCorrections[i][j][k];
    }

    public double computeNeuronOutput(double inputValue, int indexLayer) {
        switch (layersInformation.get(indexLayer).activationFunction) {
            case LINEAR:
            case SOFTMAX: //soft max need another step to compute the result
                return inputValue;
            case ReLU:
                return (inputValue >= 0 ? inputValue : 0);
            case TANH:
                return (2.0 / (1 + Math.exp(-2 * inputValue))) - 1.0;
            case SIGMOID:
                return (1.0 / (1 + Math.exp(-1 * inputValue)));
            default:
                throw new IllegalStateException("Unexpected value: " + layersInformation.get(indexLayer).activationFunction);
        }
    }

    public double computeDerivative(double inputValue, double outputValue, int indexLayer) {
        switch (layersInformation.get(indexLayer).activationFunction) {
            case TANH:
                return 1.0 - Math.pow(outputValue, 2.0);
            case ReLU:
                return outputValue >= 0 ? 1.0 : 0.0;
            case SIGMOID:
                return outputValue * (1 - outputValue);
            case LINEAR:
                return 1.0;
            case SOFTMAX:
                //TODO: not implemented yet
                return Double.NaN;
            default:
                throw new IllegalStateException("Unexpected value: " + layersInformation.get(indexLayer).activationFunction);
        }
    }

    public void training(double[][] inputData, double[][] outputData, CallBack updateMES) {
        int epochMax = 100000, iterationsMax = 2000000, epochIndex, iterationIndex;
        double tempMSE;
        for (epochIndex = 0; epochIndex < epochMax; epochIndex++) {
            if (iterationsMax-- == 0) {
                System.out.println("maxIteration");
                return;
            }
            tempMSE = 0.0;
            for (iterationIndex = 0; iterationIndex < inputData.length; iterationIndex++) {
                feeding(inputData[iterationIndex]);
                backPropagate(outputData[iterationIndex], inputData[iterationIndex]);
                tempMSE += firstStepOfMSE(outputData[iterationIndex], nodeOutputs[nodeOutputs.length - 1]);
                System.out.println("input: " + inputData[iterationIndex][0] + ":" + inputData[iterationIndex][1] + " result: " + nodeOutputs[nodeOutputs.length - 1][0]);
            }
            tempMSE = tempMSE / (double) inputData.length;
            currentMES = tempMSE;
            updateMES.Callback();
            System.out.println(currentMES);
            if (Math.abs(tempMSE - acceptedMSE) < 0.00001) {
                System.out.println("MSE");
                return;
            }
        }
        System.out.println("maxEpoch");
    }

    double firstStepOfMSE(double[] desiredOutput, double[] actualOutput) {
        double toReturn = 0.0;
        for (int i = 0; i < desiredOutput.length; i++) {
            toReturn += Math.pow(desiredOutput[i] - actualOutput[i], 2);
        }
        return (toReturn);
    }

    double[] predict(double[] input) {
        if (feeding(input)) {
            return nodeOutputs[nodeOutputs.length - 1];
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        int i, j, k;
        for (i = 0; i < weights.length; i++) {
            for (j = 0; j < weights[i].length; j++) {
                for (k = 0; k < weights[i][j].length; k++) {
                    stringBuffer.append(weights[i][j][k]);
                    stringBuffer.append(',');
                }
                stringBuffer.append('\t');
            }
            stringBuffer.append('\n');
        }
        return stringBuffer.toString();
    }

    public static void main(String[] args) {
        ArrayList<LayerInformation> information = new ArrayList<LayerInformation>();
        information.add(new LayerInformation(3, ActivationFunction.ReLU));
        information.add(new LayerInformation(2, ActivationFunction.ReLU));
        NeuralNetwork nn = new NeuralNetwork(information, 0.03, 0.0001, 2);
//        for (int i=0;i<nn.weights.length;i++)
//            for (int j=0;j<nn.weights[i].length;j++)
//                for(int k=0;k<nn.weights[i][j].length;k++)
//                {
//                    System.out.printf("Layer: %d,Node: %d,Connections: %d\thas value:%f\n",i,j,k,nn.weights[i][j][k]);
//                }
        double[] inputData = new double[]{0.5, 1, 2};
        double[] outputData = new double[]{1, 0, 1};

        nn.backPropagate(outputData, inputData);
    }

}
