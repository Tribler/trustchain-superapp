# Trustchain Trading Bot

## Naive Bayes Classifier
This naive implementation of a bayesian classifier is able to determine the mean and standard deviation of recent trades, and determines whether or not to fill a bid/ask order depending on how far away from the mean price the limit order is.

v0.1 implements a trained bot for the DD_BTC pairing, assuming a mean of 100DD per 1BTC. Features are described below.

### Features

1. Columns:
    1. Price (DD_BTC pairing, integer values only)
    1. Buy/Sell (Binary, 0 for buy, 1 for sell)
    
1. Training data: 100,000 data points with 2 target labels
    1. 0: Below mean
    1. 1: Above mean    

### Training the model
Training data is in .csv format. Import the training data by creating a `NaiveBayes` object with the training data as argument. After instantiating an instance of the model, it is automatically trained for prediction.

### Obtaining Predictions
Use `predict` in NaiveBayesClassifier to obtain a target label prediction. `predict` will choose the label with the highest probability. 
Use `predictWithProbability` in NaiveBayesClassifier to obtain a target label prediction and its associated probability.

