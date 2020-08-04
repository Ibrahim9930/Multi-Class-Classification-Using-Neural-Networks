package org.uousef.project.ai.modules;

public class LayerInformation
{
    int neuronNumber;
    ActivationFunction activationFunction;

    public LayerInformation(int neuronNumber, ActivationFunction activationFunction) {
        this.neuronNumber = neuronNumber;
        this.activationFunction = activationFunction;
    }
}
