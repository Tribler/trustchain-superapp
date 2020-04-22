# TrustChain Voter
This submodule of the TrustChain Super App enables a front-end for using the functionality that is provided in the `common/util/Votinghelper` API. For documentation on the API itself, please see [this page](../common/README.md).

This module currently contains the following functionality:
- Retrieve and show list of current proposals including tally
- Cast a vote on a proposal
- Create a new proposal based in Yes / No voting mode or threshold voting mode

## Context
<!-- TODO: In what setting this module is used -->

## Proposal list
When entering the submodule, the user sees the proposal list at first. Using the toggle, the user can switch between a list of all proposals (left) and a list of new proposals (right), which contains proposals on which the user has not casted a vote yet.

<!-- TODO: change file path when merging to Tribler repo -->
<img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/all-proposals.png" width="180"> <img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/new-proposals.png" width="180">

## Creating a new proposal
A user can create a new proposal by clicking on the `create proposal` button in the bottom right corner. A dialog will pop up in which the user can enter a name for the proposal and switch between Yes/No voting (left) and threshold voting (right).

<!-- TODO: change file path when merging to Tribler repo -->
<img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/create-yes-no-proposal.png" width="180"> <img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/create-threshold-proposal.png" width="180">

### Yes/No voting mode
In the Yes/No voting mode the result of the proposal is determined by counting the number of Yes and No votes.

### Threshold voting mode
In the threshold voting mode, the result of the proposal is determined by deciding whether the number of Yes votes reaches a certain threshold. This threshold is decided upon when the community or dao is created and by default is `80%`.

## Casting vote
When clicking on a proposal the user will see a new dialog pop up which he/she can use to cast a vote if he/she has not done so before (left). In the case that the user has already cast a vote on the selected proposal, an overview of the proposal and the users vote will be shown (right).

<!-- TODO: change file path when merging to Tribler repo -->
<img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/cast-vote.png" width="180"> <img src="https://raw.githubusercontent.com/emieldesmidt/trustchain-superapp/doc/doc/voting/vote-has-been-cast.png" width="180">

