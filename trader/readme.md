# Trustchain Trading App

The trustchain trading app consist of two parts.
1. An AI trading bot using a Naive Bayes Classifier which buys or sells Bitcoins using a decentralized market.
2. Sending receiving money functionality to other peers.

## AI trading bot
The AI trading but is shown upon opening the app. It receives bids and asks from other peers which want to buy or sell Bitcoins for Dymbe Dollars.
Upon receiving a bid or ask, it decides to either execute the offer or not.

### Bids/Asks
The bids and asks are received messages from

### Naive Bayes Classifier
This naive implementation of a bayesian classifier is able to determine the mean and standard deviation of recent trades, and determines whether or not to fill a bid/ask order depending on how far away from the mean price the limit order is.

v0.1 implements a trained bot for the DD_BTC pairing, assuming a mean of 100DD per 1BTC. Features are described below.

#### Features

1. Columns:
    1. Price (DD_BTC pairing, integer values only)
    1. Bid_or_Ask (Binary, 0 for bid, 1 for ask)
    1. Labels (0, 1, 2)

1. Training data: 100,000 data points with 3 target labels
    1. 0: Do nothing
    1. 1: Execute buy order (ask price surpasses bid threshold under the mean)
    1. 2: Execute sell order (bid price surpasses ask threshold above the mean)

#### Training the model
Training data is in .csv format. Import the training data by creating a `NaiveBayes` object with the training data as argument. After instantiating an instance of the model, it is automatically trained for prediction.

#### Obtaining Predictions
Use `predict` in NaiveBayesClassifier to obtain a target label prediction. `predict` will choose the label with the highest probability.
Use `predictWithProbability` in NaiveBayesClassifier to obtain a target label prediction and its associated probability.

##
