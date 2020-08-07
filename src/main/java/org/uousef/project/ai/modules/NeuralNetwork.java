package org.uousef.project.ai.modules;

import java.util.*;
import java.util.stream.DoubleStream;

public class NeuralNetwork {

    public double[][][] weights, weightCorrections;
    public double[][] thresholds, nodeOutputs;
    public double learningRate, acceptedMSE, currentMES;
    public int inputNeuronNumber, epochMax;
    public int currentEpoch;
    private double[] softMaxInput;
    //Contains weights of hidden layers and outputs
    public ArrayList<LayerInformation> layersInformation;

    public NeuralNetwork(ArrayList<LayerInformation> layersInformation, double learningRate, double acceptedMSE, int inputNeuronNumber, int epochMax) {
        this.epochMax = epochMax;
        this.learningRate = learningRate;
        this.acceptedMSE = acceptedMSE;
        this.inputNeuronNumber = inputNeuronNumber;
        this.layersInformation = layersInformation;
        softMaxInput = (layersInformation.get(layersInformation.size() - 1).activationFunction == ActivationFunction.SOFTMAX)
                ? softMaxInput = new double[layersInformation.get(layersInformation.size() - 1).neuronNumber] : null;
        initializeNeuralNetwork();
    }

    private void initializeNeuralNetwork() {

        Random randomGenerator = new Random();
        int index, nestedIndex, previousNeuronNum, temp = layersInformation.size();

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


//                weights[index][nestedIndex] = randomGenerator.doubles(previousNeuronNum, (-2.4 / (double) previousNeuronNum), (+2.4 / (double) previousNeuronNum)).toArray();
                weights[index][nestedIndex] = new double[previousNeuronNum];
                for (int i = 0; i < weights[index][nestedIndex].length; i++) {
                    weights[index][nestedIndex][i] = (randomGenerator.nextDouble() * 4.8 - 2.4) / previousNeuronNum;
                }
                thresholds[index][nestedIndex] = (randomGenerator.nextDouble() * 4.8 - 2.4) / previousNeuronNum;

                weightCorrections[index][nestedIndex] = new double[previousNeuronNum];
                Arrays.fill(weightCorrections[index][nestedIndex], 0.0);
            }

        }
    }

    private boolean feeding(double[] inputData) {
        if (inputData.length != inputNeuronNumber) return false;
        int indexLayer, indexNeuron, indexWeight;
        double tempValue, expSum;

        for (indexLayer = 0; indexLayer < weights.length; indexLayer++) {
            for (indexNeuron = 0; indexNeuron < weights[indexLayer].length; indexNeuron++) {

                tempValue = 0;
                for (indexWeight = 0; indexWeight < weights[indexLayer][indexNeuron].length; indexWeight++) {
                    if (indexLayer == 0) //first hidden layer
                    {
                        tempValue += (weights[indexLayer][indexNeuron][indexWeight] * inputData[indexWeight]);
                    } else //output and all hidden layers except first one
                    {
                        tempValue += (weights[indexLayer][indexNeuron][indexWeight] * nodeOutputs[indexLayer - 1][indexWeight]);
                    }
                }
                tempValue -= thresholds[indexLayer][indexNeuron];
                nodeOutputs[indexLayer][indexNeuron] = computeNeuronOutput(tempValue, indexLayer);
            }
            //check if its the output layer and use softmax
            if (indexLayer + 1 == weights.length && layersInformation.get(indexLayer).activationFunction == ActivationFunction.SOFTMAX) {
                softMaxInput = nodeOutputs[indexLayer].clone();
                expSum = DoubleStream.of(nodeOutputs[indexLayer]).sum();
                for (indexNeuron = 0; indexNeuron < weights[indexLayer].length; indexNeuron++) {
                    nodeOutputs[indexLayer][indexNeuron] /= expSum;
                }
            }
        }
        return true;
    }

    private void testBack(double[] inputs, double[] finalOutputs) {
        int indexLayer, indexNode, indexWeight;
        double error;
        double gradient;
        ArrayList<Double> gradient1 = new ArrayList<>(), gradient2 = new ArrayList<>();
        for (indexLayer = weights.length - 1; indexLayer >= 0; indexLayer--) {
            for (indexNode = 0; indexNode < weights[indexLayer].length; indexNode++) {
                if (indexLayer == weights.length - 1) {
                    error = (layersInformation.get(layersInformation.size()-1).activationFunction != ActivationFunction.SOFTMAX)
                            ? finalOutputs[indexNode] - nodeOutputs[weights.length - 1][indexNode]
                            : finalOutputs[indexNode] * Math.log(nodeOutputs[weights.length - 1][indexNode])/Math.log(2);

                    gradient = error * computeDerivative(nodeOutputs[weights.length - 1][indexNode], weights.length - 1,indexNode);
                    gradient1.add(gradient);
                } else {
                    gradient = 0;
                    for (indexWeight = 0; indexWeight < gradient2.size(); indexWeight++) {
                        gradient += (gradient2.get(indexWeight) * weights[indexLayer + 1][indexWeight][indexNode]);
                    }
                    gradient *= computeDerivative( nodeOutputs[indexLayer][indexNode], indexLayer,indexNode);
                    gradient1.add(gradient);
                }
//                thresholds[indexLayer][indexNode] += learningRate * thresholds[indexLayer][indexNode] * gradient;
                thresholds[indexLayer][indexNode] += learningRate * -1 * gradient;
            }

            gradient2 = (ArrayList<Double>) gradient1.clone();
            gradient1.clear();
            for (indexNode = 0; indexNode < weightCorrections[indexLayer].length; indexNode++)
                for (indexWeight = 0; indexWeight < weightCorrections[indexLayer][indexNode].length; indexWeight++)
                    if (indexLayer == 0) {
                        weightCorrections[indexLayer][indexNode][indexWeight] = learningRate * gradient2.get(indexNode) * inputs[indexWeight];
                    } else
                        weightCorrections[indexLayer][indexNode][indexWeight] = learningRate * gradient2.get(indexNode) * nodeOutputs[indexLayer - 1][indexWeight];
        }
        for (indexLayer = 0; indexLayer < weights.length; indexLayer++)
            for (indexNode = 0; indexNode < weights[indexLayer].length; indexNode++)
                for (indexWeight = 0; indexWeight < weights[indexLayer][indexNode].length; indexWeight++)
                    weights[indexLayer][indexNode][indexWeight] += weightCorrections[indexLayer][indexNode][indexWeight];
    }

    private void backPropagate(double[] finalOutputs, double[] inputs) {
        Vector<Double> oldGradients = new Vector<>();
        Vector<Double> newGradients = new Vector<>();
        // Layers loop
        for (int i = weights.length - 1; i >= 0; i--) {

            // Nodes loop
            for (int j = 0; j < weights[i].length; j++) {
                double gradientFactor = 0;
                double gradientValue = computeDerivative(nodeOutputs[i][j], i,j);
                if (i != weights.length - 1) {

                    for (int k = 0; k < weights[i + 1].length; k++) {
                        gradientFactor += weights[i + 1][k][j] * oldGradients.get(k);
                    }
                } else {
                    gradientFactor = finalOutputs[j] - nodeOutputs[i][j];
                }
                // Calculate gradient for that node and store it
                newGradients.insertElementAt(gradientValue * gradientFactor, j);

                // Weights loop(finds the weights' correction values
                for (int k = 0; k < weights[i][j].length; k++) {
                    if (i != 0)
                        weightCorrections[i][j][k] = learningRate * newGradients.get(j) * nodeOutputs[i - 1][k];
                    else
                        weightCorrections[i][j][k] = learningRate * newGradients.get(j) * inputs[k];
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

    private double computeNeuronOutput(double inputValue, int indexLayer) {
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

    private double computeDerivative( double outputValue, int indexLayer,int neuronNumber) {
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
                return outputValue - this.nodeOutputs[nodeOutputs.length - 1][neuronNumber];
            default:
                throw new IllegalStateException("Unexpected value: " + layersInformation.get(indexLayer).activationFunction);
        }
    }

    public void training(double[][] inputData, double[][] outputData, CallBack updateMES, SeriesCallBack updateSeries, CallBack doneLearning, CallBack clearSeries) {
        int iterationIndex = 0;
        int epochIndex = 0;
        double tempMSE;
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
        for (epochIndex = 0; epochIndex < epochMax; epochIndex++) {
            tempMSE = 0.0;
            clearSeries.Callback();
            for (iterationIndex = 0; iterationIndex < inputData.length; iterationIndex++) {
                feeding(inputData[iterationIndex]);
                testBack(inputData[iterationIndex], outputData[iterationIndex]);
                tempMSE += firstStepOfMSE(outputData[iterationIndex], nodeOutputs[nodeOutputs.length - 1]);
//                System.out.println("input: " + inputData[iterationIndex][0] + ":" + inputData[iterationIndex][1] + " result: " + nodeOutputs[nodeOutputs.length - 1][0]+"output: "+outputData[iterationIndex][0]);
                updateSeries.SeriesCallBack(iterationIndex);
            }
            tempMSE = tempMSE / (double) inputData.length;
            if (Math.abs(tempMSE - currentMES) > 0.01) {
                currentMES = tempMSE;
                updateMES.Callback();
            }
            currentMES = tempMSE;
            currentEpoch = epochIndex;

//            System.out.println(currentMES);
            if (acceptedMSE >= tempMSE) {
//                System.out.println("MSE");
                updateMES.Callback();
                doneLearning.Callback();
                return;
            }
        }
        updateMES.Callback();
        doneLearning.Callback();
        System.out.println("maxEpoch");
    }

    private double firstStepOfMSE(double[] desiredOutput, double[] actualOutput) {

        double toReturn = 0.0;
        for (int i = 0; i < desiredOutput.length; i++)
            toReturn += Math.pow(desiredOutput[i] - actualOutput[i], 2);

        return (toReturn);
    }

    public double[] predict(double[] input) {
        if (feeding(input))
            return nodeOutputs[nodeOutputs.length - 1];
        else return null;
    }

    @Override
    public String toString() {
        StringBuilder stringBuffer = new StringBuilder();
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
        ArrayList<LayerInformation> information = new ArrayList<>();
        information.add(new LayerInformation(3, ActivationFunction.ReLU));
        information.add(new LayerInformation(2, ActivationFunction.ReLU));
        NeuralNetwork nn = new NeuralNetwork(information, 0.03, 0.0001, 2, 1000);
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
