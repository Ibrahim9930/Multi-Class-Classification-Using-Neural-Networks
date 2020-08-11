package org.uousef.project.ai.modules;

import java.util.*;
import java.util.stream.DoubleStream;

public class NeuralNetwork {

    private ArrayList<LayerInformation> layersInformation;
    private double[][][] weights, weightCorrections;
    private double[][] thresholds, nodeOutputs;
    public double learningRate, acceptedMSE, currentMSE;
    public int inputNeuronNumber, outputNeuronNumber, epochMax, currentEpoch;

    private boolean adaptiveLearning;

    public NeuralNetwork(ArrayList<LayerInformation> layersInformation, double learningRate, double acceptedMSE, int inputNeuronNumber, int epochMax, boolean adaptiveLearning) {
        this.epochMax = epochMax;
        this.learningRate = learningRate;
        this.acceptedMSE = acceptedMSE;
        this.inputNeuronNumber = inputNeuronNumber;
        this.layersInformation = layersInformation;
        this.adaptiveLearning = adaptiveLearning;
        this.outputNeuronNumber = layersInformation.get(layersInformation.size() - 1).neuronNumber;
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

                expSum = DoubleStream.of(nodeOutputs[indexLayer]).sum();
                for (indexNeuron = 0; indexNeuron < weights[indexLayer].length; indexNeuron++) {
                    nodeOutputs[indexLayer][indexNeuron] /= expSum;
                }
            }
        }
        return true;
    }

    private void backPropagate(double[] inputs, double[] finalOutputs) {
        int indexLayer, indexNode, indexWeight;
        double error, gradient;
        ArrayList<Double> gradient1 = new ArrayList<>(), gradient2 = null;

        for (indexLayer = weights.length - 1; indexLayer >= 0; indexLayer--) {
            for (indexNode = 0; indexNode < weights[indexLayer].length; indexNode++) {
                if (indexLayer == weights.length - 1) {
                    error = finalOutputs[indexNode] - nodeOutputs[weights.length - 1][indexNode];
//                            : -1 * (finalOutputs[indexNode] * Math.log(nodeOutputs[weights.length - 1][indexNode]) / Math.log(2));
                    gradient = error * computeDerivative(nodeOutputs[weights.length - 1][indexNode], weights.length - 1, indexNode);
                    gradient1.add(gradient);
                } else {
                    gradient = 0;
                    for (indexWeight = 0; indexWeight < Objects.requireNonNull(gradient2).size(); indexWeight++) {
                        gradient += (gradient2.get(indexWeight) * weights[indexLayer + 1][indexWeight][indexNode]);
                    }
                    gradient *= computeDerivative(nodeOutputs[indexLayer][indexNode], indexLayer, indexNode);
                    gradient1.add(gradient);
                }
//                thresholds[indexLayer][indexNode] += learningRate * thresholds[indexLayer][indexNode] * gradient;
                thresholds[indexLayer][indexNode] += learningRate * -1 * gradient;
            }

            gradient2 = new ArrayList<>(gradient1);
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

    public void training(double[][] inputData, double[][] outputData, CallBack updateMES, CallBack doneLearning) {
        int iterationIndex, epochIndex, lastEpoch = 0;
        double tempMSE, lastMSE = 0;

            for (epochIndex = 0; epochIndex < epochMax; epochIndex++) {
            tempMSE = 0.0;
            for (iterationIndex = 0; iterationIndex < inputData.length; iterationIndex++) {
                feeding(inputData[iterationIndex]);
                backPropagate(inputData[iterationIndex], outputData[iterationIndex]);
                tempMSE += firstStepOfMSE(outputData[iterationIndex], nodeOutputs[nodeOutputs.length - 1]);
            }
            tempMSE = tempMSE / (double) inputData.length;

            if (adaptiveLearning && tempMSE < currentMSE)
                learningRate *= 1.05;
            else if (adaptiveLearning)
                learningRate *= 0.7;

            if (epochIndex - lastEpoch > 200 || Math.abs(lastMSE - currentMSE) > 0.005) {
                currentMSE = tempMSE;
                lastMSE = tempMSE;
                lastEpoch = epochIndex;
                updateMES.Callback();
            }
            currentMSE = tempMSE;
            currentEpoch = epochIndex;
            if (acceptedMSE >= tempMSE) {
                updateMES.Callback();
                doneLearning.Callback();
                return;
            }
        }
        updateMES.Callback();
        doneLearning.Callback();
        System.out.println("maxEpoch");
    }

    public double[] predict(double[] input) {
        if (feeding(input))
            return nodeOutputs[nodeOutputs.length - 1];
        else return null;
    }

    private double firstStepOfMSE(double[] desiredOutput, double[] actualOutput) {

        double toReturn = 0.0;
        for (int i = 0; i < desiredOutput.length; i++)
            toReturn += Math.pow(desiredOutput[i] - actualOutput[i], 2);

        return (toReturn);
    }

    private double computeNeuronOutput(double inputValue, int indexLayer) {
        switch (layersInformation.get(indexLayer).activationFunction) {
            case LINEAR:
                return inputValue;
            case ReLU:
                return (inputValue >= 0 ? inputValue : 0);
            case TANH:
                return (2.0 / (1 + Math.exp(-2 * inputValue))) - 1.0;
            case SIGMOID:
                return (1.0 / (1 + Math.exp(-1 * inputValue)));
            case SOFTMAX: //soft max need another step to compute the result
                return Math.exp(inputValue);
            default:
                throw new IllegalStateException("Unexpected value: " + layersInformation.get(indexLayer).activationFunction);
        }
    }

    private double computeDerivative(double outputValue, int indexLayer, int neuronNumber) {
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

    public double getCurrentMSE() {
        return currentMSE;
    }

    public int getInputNeuronNumber() {
        return inputNeuronNumber;
    }

    public int getOutputNeuronNumber() {
        return outputNeuronNumber;
    }

    public int getCurrentEpoch() {
        return currentEpoch;
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
}
