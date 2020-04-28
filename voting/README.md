# TrustChain Voter
This submodule of the TrustChain Super App enables a demo front-end for using the functionality that is provided in the `common/util/Votinghelper` API. For documentation on the API itself, please see [this page](../common/README.md). 

This module currently contains the following functionality:
- Retrieve and show list of current proposals including tally
- Cast a vote on a proposal
- Create a new proposal based in Yes / No voting mode or threshold voting mode
- `EXPERIMENTAL` Freedom of Computing file proposal 

## Context
<!-- TODO: In what setting this module is used -->

## Proposal list
When entering the submodule, the user sees the proposal list at first. Using the toggle, the user can switch between a list of all proposals (left) and a list of new proposals (right), which contains proposals on which the user has not casted a vote yet.

<img src="https://user-images.githubusercontent.com/17474698/80521306-1b516500-898b-11ea-9f02-f982db4c3cde.gif" width="280"> 

## Creating a new proposal
A user can create a new proposal by clicking on the `create proposal` button in the bottom right corner. A dialog will pop up in which the user can enter a name for the proposal and switch between Yes/No voting (left) and threshold voting (right).

<img src="https://user-images.githubusercontent.com/17474698/80522657-37560600-898d-11ea-9dbb-eeff8eec9ea2.gif" width="280">

### Yes/No voting mode
In the Yes/No voting mode the result of the proposal is determined by counting the number of Yes and No votes.
<img src="https://user-images.githubusercontent.com/17474698/80524305-b2b8b700-898f-11ea-99f3-6225eb3fe705.png" width="280">

### Threshold voting mode
In the threshold voting mode, the result of the proposal is determined by deciding whether the number of Yes votes reaches a certain threshold. This threshold is decided upon when the community or dao is created and by default is `80%`.
<img src="https://user-images.githubusercontent.com/17474698/80524307-b3514d80-898f-11ea-9965-9c9a3268b0b9.png" width="280">

## Casting vote
When clicking on a proposal the user will see a new dialog pop up which he/she can use to cast a vote if he/she has not done so before (left). In the case that the user has already cast a vote on the selected proposal, an overview of the proposal and the users vote will be shown (right).

<!-- TODO: change file path when merging to Tribler repo -->
<img src="https://user-images.githubusercontent.com/17474698/80523868-08d92a80-898f-11ea-8b39-4967a5915a27.gif" width="280"> 

## `EXPERIMENTAL` Freedom of Computing file proposal
Mostly as a proof of concept, and a demonstration of an actual use case, one can propose a file to be uploaded using the functionality of the Freedom of Computing group. This can be done through proposing the filepath of the relevant file, and only once it has been accepted by the community, this will be available for uploading. As this is experimental, and a proof of concept, this is not included in the main development branch. For those interested, it can be found [here](https://github.com/emieldesmidt/trustchain-superapp/tree/FOC-Integration)

<img src="https://user-images.githubusercontent.com/17474698/80526174-a2560b80-8992-11ea-96cd-7089d7b47e29.gif" width="280">
