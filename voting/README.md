# TrustChain Voter
This submodule of the TrustChain Super App enables a front-end for using the functionality that is provided in the `common/util/Votinghelper` API. For documentation on the API itself, please see 

This module currently contains the following functionality:
- Retrieve and show list of current proposals including tally
- Cast a vote on a proposal
- Create a new proposal based in Yes / No voting mode or threshold voting mode

## Context
*In what setting this module is used*

## Creating a new proposal


### Yes / No voting mode
In the Yes / No voting mode the result of the proposal is determined by counting the number of Yes and No votes.

### Threshold voting mode
In the threshold voting mode, the result of the proposal is determined by deciding whether the number of Yes votes reaches a certain threshold. This threshold is decided upon when the community or dao is created and by default is `80%`.

## Proposal list
When entering the submodule, the user sees the proposal list at first. Using the toggle, the user can switch between a list of all proposals and a list of new proposals, which contains proposals on which the user has not casted a vote yet.

## Casting vote
When clicking on a proposal the user will see a new dialog pop up which he/she can use to cast a vote if he/she has not done so before. In the case that the user has already cast a vote on the selected proposal, an overview of the proposal and the users vote will be shown.
