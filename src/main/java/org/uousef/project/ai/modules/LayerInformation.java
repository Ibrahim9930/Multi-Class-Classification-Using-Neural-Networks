package org.uousef.project.ai.modules;

public class LayerInformation
{
    public int neuronNumber;
    ActivationFunction activationFunction;

    public LayerInformation(int neuronNumber, ActivationFunction activationFunction) {
        this.neuronNumber = neuronNumber;
        this.activationFunction = activationFunction;
    }

    public String toString()
    {
        return ""+neuronNumber+"ac: "+activationFunction;
    }
}
